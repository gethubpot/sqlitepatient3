package com.example.sqlitepatient3.data.local.database

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Contains individual schema change operations that can be composed
 * to create migration paths between different database versions.
 * This helps eliminate duplicated code across migration paths.
 */
object MigrationSteps {
    private const val TAG = "MigrationSteps"

    /**
     * Adds the externalId column to the patients table (v1 to v2)
     */
    fun addExternalIdToPatients(database: SupportSQLiteDatabase) {
        try {
            database.execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")
            Log.d(TAG, "Added externalId column to patients table")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding externalId column to patients table", e)
            throw e
        }
    }

    /**
     * Creates an index on the externalId column (v2 to v3)
     */
    fun createExternalIdIndex(database: SupportSQLiteDatabase) {
        try {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")
            Log.d(TAG, "Created index on patients.externalId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating index on patients.externalId", e)
            throw e
        }
    }

    /**
     * Adds diagnosis-related tables and columns (v3 to v4)
     */
    fun addDiagnosisTables(database: SupportSQLiteDatabase) {
        try {
            // Add hospiceDiagnosisId column to patients
            database.execSQL("ALTER TABLE patients ADD COLUMN hospiceDiagnosisId INTEGER DEFAULT NULL")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patients_hospiceDiagnosisId ON patients(hospiceDiagnosisId)")

            // Create diagnostic_codes table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS diagnostic_codes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    icdCode TEXT NOT NULL,
                    description TEXT NOT NULL,
                    shorthand TEXT,
                    billable INTEGER NOT NULL DEFAULT 1,
                    commonCode INTEGER NOT NULL DEFAULT 0, -- Stored as 1 for true, 0 for false
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)

            // Create indices for diagnostic_codes
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diagnostic_codes_icdCode ON diagnostic_codes(icdCode)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_shorthand ON diagnostic_codes(shorthand)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_billable ON diagnostic_codes(billable)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_commonCode ON diagnostic_codes(commonCode)")

            // Create patient_diagnoses table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS patient_diagnoses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    patientId INTEGER NOT NULL,
                    icdCode TEXT NOT NULL,
                    priority INTEGER NOT NULL,
                    isHospiceCode INTEGER NOT NULL DEFAULT 0,
                    diagnosisDate INTEGER,
                    resolvedDate INTEGER,
                    notes TEXT,
                    active INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
                )
            """)

            // Create indices for patient_diagnoses
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patient_diagnoses_patientId ON patient_diagnoses(patientId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patient_diagnoses_icdCode ON patient_diagnoses(icdCode)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patient_diagnoses_priority ON patient_diagnoses(priority)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patient_diagnoses_isHospiceCode ON patient_diagnoses(isHospiceCode)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_patient_diagnoses_active ON patient_diagnoses(active)")

            Log.d(TAG, "Added diagnosis tables and related columns")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding diagnosis tables", e)
            throw e
        }
    }

    /**
     * Changes the commonCode column in diagnostic_codes to nullable INTEGER (v4 to v5).
     * Uses the table recreation strategy.
     */
    fun changeCommonCodeToInt(database: SupportSQLiteDatabase) {
        try {
            // 1. Rename the existing table
            database.execSQL("ALTER TABLE diagnostic_codes RENAME TO diagnostic_codes_old_v4")
            Log.d(TAG, "Renamed diagnostic_codes to diagnostic_codes_old_v4")

            // 2. Create the new table with the correct schema (commonCode as INTEGER)
            database.execSQL("""
                CREATE TABLE diagnostic_codes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    icdCode TEXT NOT NULL,
                    description TEXT NOT NULL,
                    shorthand TEXT,
                    billable INTEGER NOT NULL, -- Keep as INTEGER (Boolean)
                    commonCode INTEGER,        -- Changed to nullable INTEGER
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            Log.d(TAG, "Created new diagnostic_codes table with commonCode as INTEGER")

            // 3. Copy data from the old table to the new table, converting commonCode
            //    (Assuming old commonCode was 1 for true, 0 for false)
            //    We'll map 1 to 1, and 0 or other values to NULL (or keep 0 if preferred)
            database.execSQL("""
                INSERT INTO diagnostic_codes (id, icdCode, description, shorthand, billable, commonCode, createdAt, updatedAt)
                SELECT id, icdCode, description, shorthand, billable,
                       CASE WHEN commonCode = 1 THEN 1 ELSE NULL END, -- Convert old 1 to new 1, others to NULL
                       createdAt, updatedAt
                FROM diagnostic_codes_old_v4
            """)
            Log.d(TAG, "Copied data from old table to new table, converting commonCode")

            // 4. Drop the old table
            database.execSQL("DROP TABLE diagnostic_codes_old_v4")
            Log.d(TAG, "Dropped old diagnostic_codes_old_v4 table")

            // 5. Recreate indices for the new table
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_diagnostic_codes_icdCode ON diagnostic_codes(icdCode)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_shorthand ON diagnostic_codes(shorthand)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_billable ON diagnostic_codes(billable)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_diagnostic_codes_commonCode ON diagnostic_codes(commonCode)") // Index on new INTEGER column
            Log.d(TAG, "Recreated indices for new diagnostic_codes table")

        } catch (e: Exception) {
            Log.e(TAG, "Error changing commonCode column type", e)
            // Consider attempting to rollback if possible, e.g., rename _old table back
            try {
                database.execSQL("DROP TABLE IF EXISTS diagnostic_codes")
                database.execSQL("ALTER TABLE diagnostic_codes_old_v4 RENAME TO diagnostic_codes")
                Log.w(TAG, "Attempted to rollback commonCode change")
            } catch (rollbackEx: Exception) {
                Log.e(TAG, "Rollback failed", rollbackEx)
            }
            throw e // Re-throw original exception
        }
    }

    /**
     * Records a migration entry in the system_properties table
     */
    fun recordMigration(database: SupportSQLiteDatabase, fromVersion: Int, toVersion: Int) {
        try {
            val timestamp = System.currentTimeMillis().toString()
            database.execSQL(
                """
                INSERT OR REPLACE INTO system_properties
                (key, value, updatedAt)
                VALUES ('last_migration', '${fromVersion}_to_${toVersion}', ?)
                """,
                arrayOf(timestamp)
            )
            Log.d(TAG, "Recorded migration from $fromVersion to $toVersion in system_properties")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording migration in system_properties", e)
            // Don't throw here, as this is non-critical
        }
    }
}