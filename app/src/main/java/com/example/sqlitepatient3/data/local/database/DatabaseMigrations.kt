package com.example.sqlitepatient3.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Centralized repository of database migrations.
 * All migrations for the application's database schema changes are defined here.
 * This organization makes it easier to track and manage database evolution.
 */
object DatabaseMigrations {
    /**
     * Migration from version 1 to version 2
     * - Adds externalId column to patients table
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add external ID column for integration with external systems
            database.execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT")

            // Record migration in system properties
            database.execSQL(
                "INSERT OR REPLACE INTO system_properties (key, value, updatedAt) VALUES (?, ?, ?)",
                arrayOf("last_migration", "1_to_2", System.currentTimeMillis().toString())
            )
        }
    }

    /**
     * Migration from version 2 to version 3
     * - Adds index on patients.externalId for faster lookups
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create index to improve query performance when searching by externalId
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")

            // Record migration in system properties
            database.execSQL(
                "INSERT OR REPLACE INTO system_properties (key, value, updatedAt) VALUES (?, ?, ?)",
                arrayOf("last_migration", "2_to_3", System.currentTimeMillis().toString())
            )
        }
    }

    /**
     * Collection of all migrations for easy reference.
     * This array should be passed to Room's database builder using the spread operator:
     * .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3
    )
}