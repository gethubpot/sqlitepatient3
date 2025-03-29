package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.logging.Logger

/**
 * Tests for the new migration system.
 * This validates that the modular approach to migrations works correctly.
 */
@RunWith(AndroidJUnit4::class)
class MigrationSystemTest {
    private val TEST_DB = "migration-system-test.db"
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val logger = Logger.getLogger(MigrationSystemTest::class.java.name)

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private val migrationManager = MigrationManager(context)

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

    @Test
    fun testMigrationManagerPathFinding() {
        // Create a v1 database
        helper.createDatabase(TEST_DB, 1).apply {
            createBasicSchemaV1(this)
            close()
        }

        // Get automatic migrations from v1 to v4
        val migrations = migrationManager.getMigrationsToRun()

        // Validate that a migration path was found
        assertTrue("Migration path should not be empty", migrations.isNotEmpty())

        // Apply the migrations
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            *migrations
        )

        // Verify the migrations worked
        assertTrue("hospiceDiagnosisId column should exist",
            hasColumn(db, "patients", "hospiceDiagnosisId"))
        assertTrue("externalId column should exist",
            hasColumn(db, "patients", "externalId"))
        assertTrue("diagnostic_codes table should exist",
            hasTable(db, "diagnostic_codes"))

        // Verify database integrity
        assertTrue("Database should pass integrity check",
            migrationManager.verifyDatabaseIntegrity(db))
        db.close()
    }

    @Test
    fun testCompleteDirectMigration() {
        // Create a v1 database
        helper.createDatabase(TEST_DB, 1).apply {
            createBasicSchemaV1(this)
            close()
        }

        // Apply the direct 1->4 migration
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            DatabaseMigrations.MIGRATION_1_4
        )

        // Verify all expected changes are present
        assertTrue("externalId column should exist",
            hasColumn(db, "patients", "externalId"))
        assertTrue("index_patients_externalId should exist",
            hasIndex(db, "patients", "index_patients_externalId"))
        assertTrue("diagnostic_codes table should exist",
            hasTable(db, "diagnostic_codes"))
        assertTrue("patient_diagnoses table should exist",
            hasTable(db, "patient_diagnoses"))

        // Check that Room can open the migrated database
        verifyRoomCanOpenDatabase()

        db.close()
    }

    @Test
    fun testStepByStepMigration() {
        // Create a v1 database
        helper.createDatabase(TEST_DB, 1).apply {
            createBasicSchemaV1(this)
            close()
        }

        // Apply migrations one at a time
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true,
            DatabaseMigrations.MIGRATION_1_2)
        assertTrue("externalId column should exist after 1->2",
            hasColumn(dbV2, "patients", "externalId"))
        dbV2.close()

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true,
            DatabaseMigrations.MIGRATION_2_3)
        assertTrue("index_patients_externalId should exist after 2->3",
            hasIndex(dbV3, "patients", "index_patients_externalId"))
        dbV3.close()

        val dbV4 = helper.runMigrationsAndValidate(TEST_DB, 4, true,
            DatabaseMigrations.MIGRATION_3_4)
        assertTrue("diagnostic_codes table should exist after 3->4",
            hasTable(dbV4, "diagnostic_codes"))
        dbV4.close()

        // Verify final state
        verifyRoomCanOpenDatabase()
    }

    /**
     * Creates a basic v1 schema with required tables for testing
     */
    private fun createBasicSchemaV1(db: SupportSQLiteDatabase) {
        // Create patients table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS patients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                firstName TEXT NOT NULL,
                lastName TEXT NOT NULL,
                upi TEXT NOT NULL,
                dateOfBirth INTEGER,
                isMale INTEGER NOT NULL,
                medicareNumber TEXT,
                facilityId INTEGER,
                isHospice INTEGER NOT NULL DEFAULT 0,
                onCcm INTEGER NOT NULL DEFAULT 0,
                onPsych INTEGER NOT NULL DEFAULT 0,
                onPsyMed INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Create system_properties table (needed for recording migrations)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Add some test data
        db.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber,
                isHospice, onCcm, onPsych, onPsyMed,
                createdAt, updatedAt
            ) VALUES (
                'Test', 'User', 'testuser123', 1, '12345',
                0, 0, 0, 0,
                ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
        """)
    }

    /**
     * Verifies that Room can open the database after migration
     */
    private fun verifyRoomCanOpenDatabase() {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB
        ).build()

        try {
            assertTrue("Database should be open", db.isOpen)
        } finally {
            db.close()
        }
    }

    /**
     * Checks if a table has a specific column
     */
    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if a table exists in the database
     */
    private fun hasTable(db: SupportSQLiteDatabase, tableName: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    /**
     * Checks if an index exists for a table
     */
    private fun hasIndex(db: SupportSQLiteDatabase, tableName: String, indexName: String): Boolean {
        db.query("PRAGMA index_list($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == indexName) {
                    return true
                }
            }
        }
        return false
    }
}