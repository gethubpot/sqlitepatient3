package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sqlitepatient3.data.local.dao.EventDao
import com.example.sqlitepatient3.data.local.dao.FacilityDao
import com.example.sqlitepatient3.data.local.dao.PatientDao
import com.example.sqlitepatient3.data.local.dao.SystemPropertiesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.data.local.entity.FacilityEntity
import com.example.sqlitepatient3.data.local.entity.EventEntity
import com.example.sqlitepatient3.data.local.entity.SystemPropertyEntity

/**
 * Room database for the application.
 */
@Database(
    entities = [
        PatientEntity::class,
        FacilityEntity::class,
        EventEntity::class,
        SystemPropertyEntity::class
    ],
    version = 3,  // Updated to version 3 to match latest migration
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun facilityDao(): FacilityDao
    abstract fun eventDao(): EventDao
    abstract fun systemPropertiesDao(): SystemPropertiesDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "sqlitepatient3.db"

        // Include all migrations from the DatabaseMigrations object
        private val MIGRATIONS = arrayOf(
            *DatabaseMigrations.ALL_MIGRATIONS
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Run database initialization on first creation
                            CoroutineScope(Dispatchers.IO).launch {
                                // Record database creation in system properties
                                db.execSQL(
                                    """
                                    INSERT INTO system_properties (key, value, updatedAt) 
                                    VALUES ('db_created_at', ?, ?)
                                    """.trimIndent(),
                                    arrayOf(
                                        System.currentTimeMillis().toString(),
                                        System.currentTimeMillis().toString()
                                    )
                                )

                                // Verify database structure after creation
                                if (DatabaseUtils.verifyDatabaseIntegrity(db)) {
                                    Log.i(TAG, "Database created successfully and passed integrity check")
                                } else {
                                    Log.w(TAG, "Database created but failed integrity check")
                                }

                                // You can add initial data here if needed
                                // For example, adding some default facilities
                                INSTANCE?.let { database ->
                                    // Example of prepopulating data
                                    // database.facilityDao().insertFacility(FacilityEntity(name = "General Hospital", isActive = true))
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enable foreign key constraints
                            db.execSQL("PRAGMA foreign_keys = ON")
                            // Run regular maintenance
                            CoroutineScope(Dispatchers.IO).launch {
                                // Perform integrity check
                                val integrityResult = db.query("PRAGMA integrity_check")
                                integrityResult.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val result = cursor.getString(0)
                                        if (result != "ok") {
                                            Log.e(TAG, "Database integrity check failed: $result")
                                            // Consider triggering auto-restore from backup here
                                        }
                                    }
                                }
                            }
                        }
                    })
                    // Add all migrations for proper version management
                    .addMigrations(*MIGRATIONS)
                    // For development, you can enable fallback but it's risky for production
                    // .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Method to close database instance when application is shutting down
        fun closeInstance() {
            INSTANCE?.let {
                try {
                    if (it.isOpen) {
                        it.close()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing database", e)
                }
                INSTANCE = null
            }
        }

        // Method to run maintenance routines on the database
        fun performMaintenance(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = getInstance(context).openHelper.writableDatabase

                    // Check integrity first
                    val isIntact = DatabaseUtils.verifyDatabaseIntegrity(db)

                    if (isIntact) {
                        // Only optimize if integrity check passes
                        DatabaseUtils.optimizeDatabase(db)

                        // Update maintenance timestamp
                        db.execSQL(
                            """
                            INSERT OR REPLACE INTO system_properties (key, value, updatedAt) 
                            VALUES ('last_maintenance', ?, ?)
                            """.trimIndent(),
                            arrayOf(
                                System.currentTimeMillis().toString(),
                                System.currentTimeMillis().toString()
                            )
                        )

                        Log.i(TAG, "Database maintenance completed successfully")
                    } else {
                        Log.e(TAG, "Database integrity check failed")
                        // Could trigger backup restoration or other recovery mechanisms

                        // Report database corruption
                        db.execSQL(
                            """
                            INSERT OR REPLACE INTO system_properties (key, value, updatedAt) 
                            VALUES ('db_corruption_detected', ?, ?)
                            """.trimIndent(),
                            arrayOf(
                                System.currentTimeMillis().toString(),
                                System.currentTimeMillis().toString()
                            )
                        )

                        // Attempt auto-recovery by rebuilding indexes
                        try {
                            Log.i(TAG, "Attempting database recovery...")
                            db.execSQL("REINDEX")

                            // Check if recovery was successful
                            val recoverySuccessful = DatabaseUtils.verifyDatabaseIntegrity(db)
                            if (recoverySuccessful) {
                                Log.i(TAG, "Database recovery successful")
                                db.execSQL(
                                    """
                                    INSERT OR REPLACE INTO system_properties (key, value, updatedAt) 
                                    VALUES ('db_recovery_success', ?, ?)
                                    """.trimIndent(),
                                    arrayOf(
                                        System.currentTimeMillis().toString(),
                                        System.currentTimeMillis().toString()
                                    )
                                )
                            } else {
                                Log.e(TAG, "Database recovery failed")
                                // Consider triggering automatic restoration from backup
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during database recovery attempt", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during database maintenance", e)
                }
            }
        }

        /**
         * Captures the current database schema as a SchemaDiffUtil.DatabaseSchema object
         */
        fun captureCurrentSchema(context: Context): SchemaDiffUtil.DatabaseSchema {
            val db = getInstance(context).openHelper.readableDatabase
            return SchemaDiffUtil.Companion.extractSchemaFromDatabase(db)
        }
    }
}