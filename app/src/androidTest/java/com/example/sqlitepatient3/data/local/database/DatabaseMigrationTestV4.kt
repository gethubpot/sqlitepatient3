package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.logging.Logger

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTestV4 {
    private val TEST_DB = "migration-test-v4.db"
    private val MIGRATION_3_4 = DatabaseMigrations.MIGRATION_3_4
    private val MIGRATION_1_4 = DatabaseMigrations.MIGRATION_1_4
    private val MIGRATION_2_4 = DatabaseMigrations.MIGRATION_2_4
    private val ALL_MIGRATIONS = DatabaseMigrations.ALL_MIGRATIONS

    private val logger = Logger.getLogger(DatabaseMigrationTestV4::class.java.name)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setup() {
        context.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanup() {
        try {
            context.deleteDatabase(TEST_DB)
        } catch (e: Exception) {
            logger.warning("Failed to clean up test database: ${e.message}")
        }
    }

    private fun createV1Database(db: SupportSQLiteDatabase) {
        // Create patients table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS patients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                firstName TEXT NOT NULL,
                lastName TEXT NOT NULL,
                upi TEXT NOT NULL,
                dateOfBirth INTEGER,
                isMale INTEGER NOT NULL,
                medicareNumber TEXT NOT NULL,
                facilityId INTEGER,
                isHospice INTEGER NOT NULL DEFAULT 0,
                onCcm INTEGER NOT NULL DEFAULT 0,
                onPsych INTEGER NOT NULL DEFAULT 0,
                onPsyMed INTEGER NOT NULL DEFAULT 0,
                psyMedReviewDate INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Create facilities table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS facilities (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                entityType TEXT,
                lastName TEXT,
                firstName TEXT,
                middleName TEXT,
                suffix TEXT,
                address1 TEXT,
                address2 TEXT,
                city TEXT,
                state TEXT,
                zipCode TEXT,
                phoneNumber TEXT,
                faxNumber TEXT,
                email TEXT,
                npi TEXT,
                isActive INTEGER NOT NULL DEFAULT 1,
                facilityCode TEXT,
                notes TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Create events table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                patientId INTEGER NOT NULL,
                eventDateTime INTEGER NOT NULL,
                eventBillDate INTEGER NOT NULL,
                eventMinutes INTEGER NOT NULL DEFAULT 0,
                noteText TEXT,
                cptCode TEXT,
                modifier TEXT,
                eventFile TEXT,
                eventType TEXT NOT NULL,
                visitType TEXT NOT NULL,
                visitLocation TEXT NOT NULL,
                status TEXT NOT NULL,
                hospDischargeDate INTEGER,
                ttddDate INTEGER,
                monthlyBillingId INTEGER,
                followUpRecurrence TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (patientId) REFERENCES patients(id) ON DELETE CASCADE
            )
        """)

        // Create system_properties table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Create necessary indices
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_patients_upi ON patients(upi)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_patients_facilityId ON patients(facilityId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_patientId ON events(patientId)")

        // Insert test data
        insertTestPatient(db)
    }

    private fun createV2Database(db: SupportSQLiteDatabase) {
        createV1Database(db)

        // Add externalId column to patients
        db.execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")

        // Update test data
        db.execSQL("UPDATE patients SET externalId = 'EXT12345' WHERE lastName = 'Doe'")
    }

    private fun createV3Database(db: SupportSQLiteDatabase) {
        createV2Database(db)

        // Add index on externalId
        db.execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")
    }

    private fun insertTestPatient(db: SupportSQLiteDatabase) {
        val timestamp = System.currentTimeMillis()

        db.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber,
                facilityId, isHospice, onCcm, onPsych, onPsyMed,
                createdAt, updatedAt
            ) VALUES (
                'John', 'Doe', 'doejoh000000', 1, '12345',
                1, 0, 0, 0, 0,
                $timestamp, $timestamp
            )
        """)
    }

    @Test
    fun testMigration3To4() {
        // Create v3 database
        helper.createDatabase(TEST_DB, 3).apply {
            createV3Database(this)
            close()
        }

        // Migrate to v4
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        try {
            // Verify patient data survived migration
            val patientCursor = dbV4.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            patientCursor.use {
                assertTrue("Test patient should exist after migration", it.moveToFirst())
                assertEquals("John", it.getString(it.getColumnIndexOrThrow("firstName")))
                assertEquals("Doe", it.getString(it.getColumnIndexOrThrow("lastName")))
                // Verify new column exists and is null for existing patients
                assertTrue(it.isNull(it.getColumnIndexOrThrow("hospiceDiagnosisId")))
            }

            // Verify diagnostic_codes table exists
            val tablesCursor = dbV4.query("SELECT name FROM sqlite_master WHERE type='table' AND name='diagnostic_codes'")
            tablesCursor.use {
                assertTrue("diagnostic_codes table should exist", it.moveToFirst())
            }

            // Verify patient_diagnoses table exists
            val diagnosesTableCursor = dbV4.query("SELECT name FROM sqlite_master WHERE type='table' AND name='patient_diagnoses'")
            diagnosesTableCursor.use {
                assertTrue("patient_diagnoses table should exist", it.moveToFirst())
            }

            // Test inserting data into new tables
            // Insert a diagnostic code
            val codeTimestamp = System.currentTimeMillis()
            dbV4.execSQL("""
                INSERT INTO diagnostic_codes (
                    icdCode, description, shorthand, billable, commonCode, createdAt, updatedAt
                ) VALUES (
                    'I10', 'Essential Hypertension', 'HTN', 1, 1, $codeTimestamp, $codeTimestamp
                )
            """)

            // Verify the insert worked
            val codeCursor = dbV4.query("SELECT * FROM diagnostic_codes WHERE icdCode = 'I10'")
            codeCursor.use {
                assertTrue("Diagnostic code should be inserted", it.moveToFirst())
                assertEquals("Essential Hypertension", it.getString(it.getColumnIndexOrThrow("description")))
            }

            // Insert a patient diagnosis
            val diagnosisTimestamp = System.currentTimeMillis()
            dbV4.execSQL("""
                INSERT INTO patient_diagnoses (
                    patientId, icdCode, priority, isHospiceCode, active, createdAt, updatedAt
                ) VALUES (
                    1, 'I10', 1, 0, 1, $diagnosisTimestamp, $diagnosisTimestamp
                )
            """)

            // Verify the insert worked
            val diagnosisCursor = dbV4.query("SELECT * FROM patient_diagnoses WHERE patientId = 1")
            diagnosisCursor.use {
                assertTrue("Patient diagnosis should be inserted", it.moveToFirst())
                assertEquals("I10", it.getString(it.getColumnIndexOrThrow("icdCode")))
                assertEquals(1, it.getInt(it.getColumnIndexOrThrow("priority")))
            }

            // Update patient's hospiceDiagnosisId
            dbV4.execSQL("UPDATE patients SET hospiceDiagnosisId = 1 WHERE id = 1")

            // Verify the update worked
            val updatedPatientCursor = dbV4.query("SELECT hospiceDiagnosisId FROM patients WHERE id = 1")
            updatedPatientCursor.use {
                assertTrue("Patient should be updated", it.moveToFirst())
                assertEquals(1, it.getInt(it.getColumnIndexOrThrow("hospiceDiagnosisId")))
            }

            // Verify indices on new tables
            val diagnosticCodesIndices = getTableIndices(dbV4, "diagnostic_codes")
            assertTrue("Index on icdCode should exist",
                diagnosticCodesIndices.any { it.contains("index_diagnostic_codes_icdCode") })

            val patientDiagnosesIndices = getTableIndices(dbV4, "patient_diagnoses")
            assertTrue("Index on patientId should exist",
                patientDiagnosesIndices.any { it.contains("index_patient_diagnoses_patientId") })

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(4)
        } finally {
            dbV4.close()
        }
    }

    @Test
    fun testMigration1To4() {
        // Create v1 database
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Migrate directly from v1 to v4
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_1_4)

        try {
            // Verify patient data survived migration
            val patientCursor = dbV4.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            patientCursor.use {
                assertTrue("Test patient should exist after migration", it.moveToFirst())
                assertEquals("John", it.getString(it.getColumnIndexOrThrow("firstName")))
            }

            // Verify all expected columns exist in patients table
            val patientColumns = getTableColumns(dbV4, "patients")
            assertTrue("externalId column should exist", "externalId" in patientColumns)
            assertTrue("hospiceDiagnosisId column should exist", "hospiceDiagnosisId" in patientColumns)

            // Verify diagnostic_codes and patient_diagnoses tables exist
            val tables = getTables(dbV4)
            assertTrue("diagnostic_codes table should exist", "diagnostic_codes" in tables)
            assertTrue("patient_diagnoses table should exist", "patient_diagnoses" in tables)

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(4)
        } finally {
            dbV4.close()
        }
    }

    @Test
    fun testMigration2To4() {
        // Create v2 database
        helper.createDatabase(TEST_DB, 2).apply {
            createV2Database(this)
            close()
        }

        // Migrate directly from v2 to v4
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_2_4)

        try {
            // Verify patient data survived migration
            val patientCursor = dbV4.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            patientCursor.use {
                assertTrue("Test patient should exist after migration", it.moveToFirst())
                assertEquals("John", it.getString(it.getColumnIndexOrThrow("firstName")))
                assertEquals("EXT12345", it.getString(it.getColumnIndexOrThrow("externalId")))
            }

            // Verify diagnostic_codes and patient_diagnoses tables exist
            val tables = getTables(dbV4)
            assertTrue("diagnostic_codes table should exist", "diagnostic_codes" in tables)
            assertTrue("patient_diagnoses table should exist", "patient_diagnoses" in tables)

            // Verify indices
            val patientIndices = getTableIndices(dbV4, "patients")
            assertTrue("Index on externalId should exist",
                patientIndices.any { it.contains("index_patients_externalId") })
            assertTrue("Index on hospiceDiagnosisId should exist",
                patientIndices.any { it.contains("index_patients_hospiceDiagnosisId") })

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(4)
        } finally {
            dbV4.close()
        }
    }

    @Test
    fun testAllSequentialMigrationsToV4() {
        // Create v1 database
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Migrate from v1 to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)
        dbV2.close()

        // Migrate from v2 to v3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, DatabaseMigrations.MIGRATION_2_3)
        dbV3.close()

        // Migrate from v3 to v4
        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true, DatabaseMigrations.MIGRATION_3_4)

        try {
            // Verify all tables exist
            val tables = getTables(dbV4)
            assertTrue("patients table should exist", "patients" in tables)
            assertTrue("facilities table should exist", "facilities" in tables)
            assertTrue("events table should exist", "events" in tables)
            assertTrue("diagnostic_codes table should exist", "diagnostic_codes" in tables)
            assertTrue("patient_diagnoses table should exist", "patient_diagnoses" in tables)

            // Run integrity check
            assertTrue("Database should pass integrity check",
                DatabaseUtils.verifyDatabaseIntegrity(dbV4))

            // Verify Room can open the database after all migrations
            verifyRoomCanOpenDatabase(4)
        } finally {
            dbV4.close()
        }
    }

    private fun verifyRoomCanOpenDatabase(expectedVersion: Int) {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS)
            .build()
            .apply {
                openHelper.writableDatabase
                assertEquals(expectedVersion, openHelper.readableDatabase.version)
                close()
            }
    }

    private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
        return db.query("PRAGMA table_info($tableName)").use { cursor ->
            val columns = mutableListOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
            columns
        }
    }

    private fun getTableIndices(db: SupportSQLiteDatabase, tableName: String): List<String> {
        return db.query("PRAGMA index_list($tableName)").use { cursor ->
            val indices = mutableListOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                indices.add(cursor.getString(nameIndex))
            }
            indices
        }
    }

    private fun getTables(db: SupportSQLiteDatabase): List<String> {
        return db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
            tables
        }
    }
}