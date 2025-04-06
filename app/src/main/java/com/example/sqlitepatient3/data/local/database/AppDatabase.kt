package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sqlitepatient3.data.local.dao.DiagnosticCodeDao
import com.example.sqlitepatient3.data.local.dao.EventDao
import com.example.sqlitepatient3.data.local.dao.FacilityDao
import com.example.sqlitepatient3.data.local.dao.PatientDao
import com.example.sqlitepatient3.data.local.dao.PatientDiagnosisDao
import com.example.sqlitepatient3.data.local.dao.SystemPropertiesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.sqlitepatient3.data.local.entity.DiagnosticCodeEntity
import com.example.sqlitepatient3.data.local.entity.EventEntity
import com.example.sqlitepatient3.data.local.entity.FacilityEntity
import com.example.sqlitepatient3.data.local.entity.PatientDiagnosisEntity
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.data.local.entity.SystemPropertyEntity

/**
 * Room database for the application.
 */
@Database(
    entities = [
        PatientEntity::class,
        FacilityEntity::class,
        EventEntity::class,
        SystemPropertyEntity::class,
        DiagnosticCodeEntity::class,
        PatientDiagnosisEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun patientDao(): PatientDao
    abstract fun facilityDao(): FacilityDao
    abstract fun eventDao(): EventDao
    abstract fun systemPropertiesDao(): SystemPropertiesDao
    abstract fun diagnosticCodeDao(): DiagnosticCodeDao
    abstract fun patientDiagnosisDao(): PatientDiagnosisDao

    companion object {
        private const val TAG = "AppDatabase"
        private const val DATABASE_NAME = "sqlitepatient3.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Get migrations using MigrationManager
                val migrationManager = MigrationManager(context.applicationContext)
                val migrations = migrationManager.getMigrationsToRun()

                // Log the migrations that will be applied
                if (migrations.isNotEmpty()) {
                    Log.i(TAG, "Applying migrations: ${migrations.joinToString(", ") {
                        "v${it.startVersion} to v${it.endVersion}"
                    }}")
                } else {
                    Log.i(TAG, "No migrations to apply")
                }

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
                                if (!migrationManager.verifyDatabaseIntegrity(db)) {
                                    Log.e(TAG, "Database integrity check failed on open")
                                    // Consider triggering auto-restore from backup here
                                }
                            }
                        }
                    })
                    // Add the migrations determined by the manager
                    .addMigrations(*migrations)
                    // Fallback is commented out for safety, but could be uncommented if needed
                    // .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Closes the current database instance if it exists and sets the INSTANCE to null.
         * This method should be called when you need to ensure no database connections remain open,
         * such as before performing a backup or restore operation.
         */
        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.let { db ->
                    db.close()
                    Log.i(TAG, "Database instance closed")
                }
                INSTANCE = null
            }
        }

        /**
         * Performs database maintenance tasks including VACUUM and ANALYZE.
         * This helps keep the database optimized and reduces file size.
         */
        fun performMaintenance(context: Context) {
            val db = getInstance(context).openHelper.writableDatabase
            try {
                Log.i(TAG, "Starting database maintenance")
                // Vacuum to reclaim space and defragment
                db.execSQL("VACUUM")
                // Analyze to update statistics for query optimization
                db.execSQL("ANALYZE")
                // Record maintenance time
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
            } catch (e: Exception) {
                Log.e(TAG, "Error during database maintenance", e)
            }
        }
    }
}