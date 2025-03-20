package com.example.sqlitepatient3.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log

/**
 * Centralized repository of database migrations.
 * All migrations for the application's database schema changes are defined here.
 * This organization makes it easier to track and manage database evolution.
 */
object DatabaseMigrations {
    private const val TAG = "DatabaseMigrations"

    /**
     * Migration from version 1 to version 2
     * - Adds externalId column to patients table
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Add external ID column for integration with external systems
                database.execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")

                // Record migration in system properties
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO system_properties 
                    (key, value, updatedAt) 
                    VALUES ('last_migration', '1_to_2', ?)
                    """,
                    arrayOf(System.currentTimeMillis().toString())
                )

                Log.i(TAG, "Migration from version 1 to 2 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during migration from version 1 to 2", e)
                throw e
            }
        }
    }

    /**
     * Migration from version 2 to version 3
     * - Adds index on patients.externalId for faster lookups
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Create index to improve query performance when searching by externalId
                database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")

                // Record migration in system properties
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO system_properties 
                    (key, value, updatedAt) 
                    VALUES ('last_migration', '2_to_3', ?)
                    """,
                    arrayOf(System.currentTimeMillis().toString())
                )

                Log.i(TAG, "Migration from version 2 to 3 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during migration from version 2 to 3", e)
                throw e
            }
        }
    }

    /**
     * Migration directly from version 1 to version 3
     * - Combines both previous migrations for a fresh install directly to latest version
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Add external ID column first
                database.execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")

                // Then create index on that column
                database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")

                // Record migration in system properties
                database.execSQL(
                    """
                    INSERT OR REPLACE INTO system_properties 
                    (key, value, updatedAt) 
                    VALUES ('last_migration', '1_to_3', ?)
                    """,
                    arrayOf(System.currentTimeMillis().toString())
                )

                Log.i(TAG, "Direct migration from version 1 to 3 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during direct migration from version 1 to 3", e)
                throw e
            }
        }
    }

    /**
     * Collection of all migrations for easy reference.
     * This array should be passed to Room's database builder using the spread operator:
     * .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_1_3
    )

    /**
     * Helper method to verify database tables after migration
     */
    fun verifyMigrationResults(database: SupportSQLiteDatabase) {
        try {
            // Verify patients table has externalId column
            val patientColumns = getTableColumns(database, "patients")
            if (!patientColumns.contains("externalId")) {
                Log.e(TAG, "Migration verification failed: patients table missing externalId column")
                throw IllegalStateException("Migration failed: patients table missing externalId column")
            }

            // Verify index exists
            val patientIndices = getTableIndices(database, "patients")
            if (!patientIndices.any { it.contains("index_patients_externalId") }) {
                Log.e(TAG, "Migration verification failed: patients table missing externalId index")
                throw IllegalStateException("Migration failed: patients table missing externalId index")
            }

            Log.i(TAG, "Migration verification passed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration verification failed with exception", e)
            throw e
        }
    }

    /**
     * Helper method to get all columns for a table
     */
    private fun getTableColumns(database: SupportSQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        database.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameColumnIndex >= 0) {
                    columns.add(cursor.getString(nameColumnIndex))
                }
            }
        }
        return columns
    }

    /**
     * Helper method to get all indices for a table
     */
    private fun getTableIndices(database: SupportSQLiteDatabase, tableName: String): List<String> {
        val indices = mutableListOf<String>()
        database.query("PRAGMA index_list($tableName)").use { cursor ->
            val nameColumnIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameColumnIndex >= 0) {
                    indices.add(cursor.getString(nameColumnIndex))
                }
            }
        }
        return indices
    }
}