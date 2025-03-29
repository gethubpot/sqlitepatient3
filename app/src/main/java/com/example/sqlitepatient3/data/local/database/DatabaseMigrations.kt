package com.example.sqlitepatient3.data.local.database

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                MigrationSteps.addExternalIdToPatients(database)
                MigrationSteps.recordMigration(database, 1, 2)
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
                MigrationSteps.createExternalIdIndex(database)
                MigrationSteps.recordMigration(database, 2, 3)
                Log.i(TAG, "Migration from version 2 to 3 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during migration from version 2 to 3", e)
                throw e
            }
        }
    }

    /**
     * Migration from version 3 to version 4
     * - Adds diagnostic_codes table for ICD-10 codes
     * - Adds patient_diagnoses table to link patients with diagnoses
     * - Adds hospiceDiagnosisId column to patients table
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                MigrationSteps.addDiagnosisTables(database)
                MigrationSteps.recordMigration(database, 3, 4)
                Log.i(TAG, "Migration from version 3 to 4 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during migration from version 3 to 4", e)
                throw e
            }
        }
    }

    /**
     * Migration directly from version 1 to version 3
     * - Combines both previous migrations for a fresh install directly to version 3
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                MigrationSteps.addExternalIdToPatients(database)
                MigrationSteps.createExternalIdIndex(database)
                MigrationSteps.recordMigration(database, 1, 3)
                Log.i(TAG, "Direct migration from version 1 to 3 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during direct migration from version 1 to 3", e)
                throw e
            }
        }
    }

    /**
     * Migration directly from version 1 to version 4
     * - Combines all previous migrations for a fresh install directly to latest version
     */
    val MIGRATION_1_4 = object : Migration(1, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                MigrationSteps.addExternalIdToPatients(database)
                MigrationSteps.createExternalIdIndex(database)
                MigrationSteps.addDiagnosisTables(database)
                MigrationSteps.recordMigration(database, 1, 4)
                Log.i(TAG, "Direct migration from version 1 to 4 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during direct migration from version 1 to 4", e)
                throw e
            }
        }
    }

    /**
     * Migration directly from version 2 to version 4
     * - Combines v2->v3 and v3->v4 migrations
     */
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                MigrationSteps.createExternalIdIndex(database)
                MigrationSteps.addDiagnosisTables(database)
                MigrationSteps.recordMigration(database, 2, 4)
                Log.i(TAG, "Direct migration from version 2 to 4 completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during direct migration from version 2 to 4", e)
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
        MIGRATION_3_4,
        MIGRATION_1_3,
        MIGRATION_1_4,
        MIGRATION_2_4
    )
}