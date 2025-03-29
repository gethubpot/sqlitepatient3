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
                    commonCode INTEGER NOT NULL DEFAULT 0,
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