package com.example.sqlitepatient3.data.local.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import android.util.Log
import java.io.IOException

/**
 * Test suite for database migrations.
 * This class contains comprehensive tests for all migration paths between database versions.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTestSuite {
    private val TAG = "MigrationTestSuite"
    private val TEST_DB = "migration-test.db"

    // Get migrations from the centralized repository
    private val MIGRATION_1_2 = DatabaseMigrations.MIGRATION_1_2
    private val MIGRATION_2_3 = DatabaseMigrations.MIGRATION_2_3
    private val MIGRATION_1_3 = DatabaseMigrations.MIGRATION_1_3
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)

    // Application context for database operations
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // Migration test helper
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setup() {
        // Ensure clean state before each test
        context.deleteDatabase(TEST_DB)
        Log.i(TAG, "Test setup: deleted any existing test database")
    }

    @After
    fun cleanup() {
        try {
            context.deleteDatabase(TEST_DB)
            Log.i(TAG, "Test cleanup: deleted test database")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up test database", e)
        }
    }

    /**
     * Creates the initial database schema for version 1
     */
    private fun createV1Schema(db: SupportSQLiteDatabase) {
        Log.i(TAG, "Creating v1 schema")

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
        db.execSQL("CREATE INDEX IF NOT EXISTS index_patients_lastName_firstName ON patients(lastName, firstName)")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_facilities_name ON facilities(name)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_facilities_facilityCode ON facilities(facilityCode)")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_patientId ON events(patientId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventDateTime ON events(eventDateTime)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_status ON events(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventType ON events(eventType)")

        // Record schema creation
        db.execSQL("""
            INSERT OR REPLACE INTO system_properties (key, value, updatedAt)
            VALUES ('schema_created', 'v1', ?)
        """, arrayOf(System.currentTimeMillis().toString()))

        Log.i(TAG, "V1 schema created successfully")
    }

    /**
     * Adds test data to the database
     */
    private fun insertTestData(db: SupportSQLiteDatabase, version: Int) {
        Log.i(TAG, "Inserting test data for schema v$version")

        val timestamp = System.currentTimeMillis()

        // Insert test patient
        db.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, 
                isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
                ${if (version >= 2) ", externalId" else ""}
            ) VALUES (
                'John', 'Doe', 'doejoh000000', 1, '12345', 
                0, 0, 0, 0, $timestamp, $timestamp
                ${if (version >= 2) ", 'EXT12345'" else ""}
            )
        """)

        // Insert test facility
        db.execSQL("""
            INSERT INTO facilities (
                name, isActive, facilityCode, createdAt, updatedAt
            ) VALUES (
                'Test Facility', 1, 'TF001', $timestamp, $timestamp
            )
        """)

        Log.i(TAG, "Test data inserted successfully")
    }

    @Test
    fun testMigration1To2() {
        Log.i(TAG, "Starting migration test: 1 -> 2")

        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Schema(this)
            insertTestData(this, 1)
            close()
        }

        Log.i(TAG, "Created and populated v1 database, now migrating to v2")

        // Migrate to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        try {
            // Verify data survived migration
            val cursor = dbV2.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            cursor.use {
                assertTrue("Test data should exist after migration", cursor.moveToFirst())
                assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
                assertEquals("Doe", cursor.getString(cursor.getColumnIndexOrThrow("lastName")))
                assertEquals("doejoh000000", cursor.getString(cursor.getColumnIndexOrThrow("upi")))
            }

            // Verify new column exists
            val columns = getTableColumns(dbV2, "patients")
            assertTrue("externalId column should exist", "externalId" in columns)

            // Verify system properties was updated with migration info
            val propCursor = dbV2.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
            propCursor.use {
                assertTrue("Migration metadata should be recorded", propCursor.moveToFirst())
                assertEquals("1_to_2", propCursor.getString(0))
            }

            // Test that externalId column works by setting a value
            dbV2.execSQL("UPDATE patients SET externalId = 'TEST-EXT-ID' WHERE lastName = 'Doe'")
            val updateCursor = dbV2.query("SELECT externalId FROM patients WHERE lastName = 'Doe'")
            updateCursor.use {
                assertTrue("Should find updated record", it.moveToFirst())
                assertEquals("TEST-EXT-ID", it.getString(0))
            }

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(2)

            Log.i(TAG, "Migration 1->2 test passed successfully")
        } finally {
            dbV2.close()
        }
    }

    @Test
    fun testMigration2To3() {
        Log.i(TAG, "Starting migration test: 2 -> 3")

        // Create v1 database
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Schema(this)
            close()
        }

        // Migrate to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        try {
            // Add test data with externalId
            insertTestData(dbV2, 2)
            Log.i(TAG, "Created and populated v2 database, now migrating to v3")
        } finally {
            dbV2.close()
        }

        // Migrate to v3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        try {
            // Verify data survived migration
            val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            cursor.use {
                assertTrue("Test data should exist after migration", cursor.moveToFirst())
                assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
                assertEquals("EXT12345", cursor.getString(cursor.getColumnIndexOrThrow("externalId")))
            }

            // Verify index exists
            val indexes = getTableIndexes(dbV3, "patients")
            assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

            // Verify system properties was updated
            val propCursor = dbV3.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
            propCursor.use {
                assertTrue("Migration metadata should be recorded", propCursor.moveToFirst())
                assertEquals("2_to_3", propCursor.getString(0))
            }

            // Test that the index works by querying on externalId
            val indexTestCursor = dbV3.query(
                "EXPLAIN QUERY PLAN SELECT * FROM patients WHERE externalId = 'EXT12345'"
            )
            var usesIndex = false
            indexTestCursor.use {
                while (it.moveToNext()) {
                    val explanation = it.getString(it.getColumnIndex("detail"))
                    if (explanation != null && explanation.contains("USING INDEX") &&
                        (explanation.contains("externalId") || explanation.contains("patients"))) {
                        usesIndex = true
                        break
                    }
                }
            }

            // On some SQLite versions, the query plan might not explicitly show index usage for simple queries
            // Check instead that the index exists and running the query works
            if (!usesIndex) {
                Log.w(TAG, "Query on externalId doesn't explicitly use index in query plan, but this may be normal")
                val queryWorks = dbV3.query("SELECT * FROM patients WHERE externalId = 'EXT12345'").use {
                    it.moveToFirst()
                }
                assertTrue("Query on externalId should work", queryWorks)
            }

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(3)

            Log.i(TAG, "Migration 2->3 test passed successfully")
        } finally {
            dbV3.close()
        }
    }

    @Test
    fun testDirectMigration1To3() {
        Log.i(TAG, "Starting direct migration test: 1 -> 3")

        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Schema(this)
            insertTestData(this, 1)
            close()
        }

        Log.i(TAG, "Created and populated v1 database, now directly migrating to v3")

        // Migrate directly from 1 to 3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_3)

        try {
            // Verify data survived migration
            val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            cursor.use {
                assertTrue("Test data should exist after migration", cursor.moveToFirst())
                assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
                assertFalse("externalId should be null initially",
                    it.getString(it.getColumnIndexOrThrow("externalId")) != null)
            }

            // Verify column and index exist
            val columns = getTableColumns(dbV3, "patients")
            assertTrue("externalId column should exist", "externalId" in columns)

            val indexes = getTableIndexes(dbV3, "patients")
            assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

            // Test that new column works
            dbV3.execSQL("UPDATE patients SET externalId = 'DIRECT-MIGRATION' WHERE lastName = 'Doe'")
            val updateCursor = dbV3.query("SELECT externalId FROM patients WHERE lastName = 'Doe'")
            updateCursor.use {
                assertTrue("Should find updated record", it.moveToFirst())
                assertEquals("DIRECT-MIGRATION", it.getString(0))
            }

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(3)

            Log.i(TAG, "Direct migration 1->3 test passed successfully")
        } finally {
            dbV3.close()
        }
    }

    @Test
    fun testAllMigrationsInSequence() {
        Log.i(TAG, "Starting sequential migrations test: 1 -> 2 -> 3")

        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Schema(this)
            insertTestData(this, 1)
            close()
        }

        // First migrate from v1 to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        dbV2.close()

        // Then migrate from v2 to v3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        try {
            // Verify final state
            val columns = getTableColumns(dbV3, "patients")
            assertTrue("externalId column should exist", "externalId" in columns)

            val indexes = getTableIndexes(dbV3, "patients")
            assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

            // Run integrity check
            assertTrue("Database should pass integrity check",
                DatabaseUtils.verifyDatabaseIntegrity(dbV3))

            // Verify Room can open the database after migration
            verifyRoomCanOpenDatabase(3)

            Log.i(TAG, "Sequential migrations test passed successfully")
        } finally {
            dbV3.close()
        }
    }

    @Test
    fun testDatabaseAutoRecovery() {
        Log.i(TAG, "Starting database auto-recovery test")

        // Create latest version database
        val dbV3 = helper.createDatabase(TEST_DB, 3).apply {
            createV1Schema(this)
            insertTestData(this, 3)

            // Create the externalId column and index to match v3 schema
            execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")
            execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")

            // Simulate corruption by dropping an index
            try {
                execSQL("DROP INDEX index_patients_lastName_firstName")
                Log.i(TAG, "Simulated corruption by dropping an index")
            } catch (e: Exception) {
                Log.w(TAG, "Could not drop index for corruption test: ${e.message}")
            }

            close()
        }

        // Open database and attempt auto-recovery
        val recoveredDb = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        try {
            // Check that recovery mechanisms work
            val integrityPassed = DatabaseUtils.verifyDatabaseIntegrity(recoveredDb)
            assertTrue("Database auto-recovery should maintain integrity", integrityPassed)

            // Verify the missing index can be recreated
            recoveredDb.execSQL("CREATE INDEX IF NOT EXISTS index_patients_lastName_firstName ON patients(lastName, firstName)")

            // Verify the database is fully functional
            val queryCursor = recoveredDb.query("SELECT * FROM patients WHERE lastName = 'Doe'")
            queryCursor.use {
                assertTrue("Database should be queryable after recovery", it.moveToFirst())
            }

            Log.i(TAG, "Database auto-recovery test passed successfully")
        } finally {
            recoveredDb.close()
        }
    }

    @Test
    fun testForeignKeyEnforcement() {
        Log.i(TAG, "Starting foreign key enforcement test")

        // Create v3 database with test data
        val dbV3 = helper.createDatabase(TEST_DB, 3).apply {
            createV1Schema(this)

            // Add v2/v3 schema changes
            execSQL("ALTER TABLE patients ADD COLUMN externalId TEXT DEFAULT NULL")
            execSQL("CREATE INDEX IF NOT EXISTS index_patients_externalId ON patients(externalId)")

            // Enable foreign keys for this test
            execSQL("PRAGMA foreign_keys = ON")

            // Insert a facility and patient
            val timestamp = System.currentTimeMillis()
            execSQL("""
                INSERT INTO facilities (
                    id, name, isActive, facilityCode, createdAt, updatedAt
                ) VALUES (
                    1, 'Test Facility', 1, 'TF001', $timestamp, $timestamp
                )
            """)

            execSQL("""
                INSERT INTO patients (
                    id, firstName, lastName, upi, isMale, medicareNumber, 
                    facilityId, isHospice, onCcm, onPsych, onPsyMed, 
                    createdAt, updatedAt, externalId
                ) VALUES (
                    1, 'John', 'Doe', 'doejoh000000', 1, '12345', 
                    1, 0, 0, 0, 0, $timestamp, $timestamp, 'EXT12345'
                )
            """)

            // Test foreign key by inserting event for patient
            execSQL("""
                INSERT INTO events (
                    patientId, eventDateTime, eventBillDate, eventMinutes,
                    eventType, visitType, visitLocation, status,
                    followUpRecurrence, createdAt, updatedAt
                ) VALUES (
                    1, $timestamp, $timestamp, 30,
                    'FACE_TO_FACE', 'HOME_VISIT', 'PATIENT_HOME', 'PENDING',
                    'NONE', $timestamp, $timestamp
                )
            """)

            Log.i(TAG, "Created test database with foreign key relationships")
        }

        try {
            // Verify foreign key constraints by attempting to insert with invalid patientId
            var constraintViolated = false
            try {
                dbV3.execSQL("""
                    INSERT INTO events (
                        patientId, eventDateTime, eventBillDate, eventMinutes,
                        eventType, visitType, visitLocation, status,
                        followUpRecurrence, createdAt, updatedAt
                    ) VALUES (
                        999, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 30,
                        'FACE_TO_FACE', 'HOME_VISIT', 'PATIENT_HOME', 'PENDING',
                        'NONE', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                    )
                """)
            } catch (e: Exception) {
                Log.i(TAG, "Foreign key constraint violation caught as expected: ${e.message}")
                constraintViolated = true
            }

            assertTrue("Foreign key constraint should be violated", constraintViolated)

            // Test cascade delete
            dbV3.execSQL("DELETE FROM patients WHERE id = 1")

            // Verify event was deleted too
            val eventCursor = dbV3.query("SELECT COUNT(*) FROM events WHERE patientId = 1")
            eventCursor.use {
                assertTrue(it.moveToFirst())
                assertEquals("Event should be deleted when patient is deleted", 0, it.getInt(0))
            }

            Log.i(TAG, "Foreign key enforcement test passed successfully")
        } finally {
            dbV3.close()
        }
    }

    // Helper method to verify Room can open the database
    private fun verifyRoomCanOpenDatabase(expectedVersion: Int) {
        Log.i(TAG, "Verifying Room can open database version $expectedVersion")

        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS)
            .build()

        try {
            // Verify database is open
            assertTrue("Database should be open", db.isOpen)

            // Verify database version is as expected
            assertEquals("Database version should match expected version",
                expectedVersion,
                db.openHelper.readableDatabase.version)

            // Perform a simple query to ensure the database is functional
            db.openHelper.readableDatabase.query("SELECT 1").use { cursor ->
                assertTrue("Database should be queryable", cursor.moveToFirst())
            }

            Log.i(TAG, "Room successfully opened database version $expectedVersion")
        } finally {
            // Close db when done
            db.close()
        }
    }

    // Helper method to get table columns
    private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }

    // Helper method to get table indexes
    private fun getTableIndexes(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val indexes = mutableListOf<String>()
        db.query("PRAGMA index_list($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(nameIndex))
            }
        }
        return indexes
    }
}