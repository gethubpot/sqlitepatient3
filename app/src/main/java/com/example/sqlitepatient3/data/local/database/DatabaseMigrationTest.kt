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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.logging.Logger

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test.db"
    private val MIGRATION_1_2 = DatabaseMigrations.MIGRATION_1_2
    private val MIGRATION_2_3 = DatabaseMigrations.MIGRATION_2_3
    private val MIGRATION_1_3 = DatabaseMigrations.MIGRATION_1_3
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)

    // Logger for debugging test issues
    private val logger = Logger.getLogger(DatabaseMigrationTest::class.java.name)

    // Single context reference for consistency
    private val context = ApplicationProvider.getApplicationContext<Context>()

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
    }

    @After
    fun cleanup() {
        try {
            context.deleteDatabase(TEST_DB)
        } catch (e: Exception) {
            logger.warning("Failed to clean up test database: ${e.message}")
        }
    }

    /**
     * Creates the initial database structure for version 1
     */
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

        // Create necessary indices
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_patients_upi ON patients(upi)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_patients_facilityId ON patients(facilityId)")

        // Create system_properties table for tracking migrations
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        // Insert test data
        db.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, 
                isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
            ) VALUES (
                'John', 'Doe', 'doejoh000000', 1, '12345', 
                0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
        """)
    }

    @Test
    fun testMigration1To2() {
        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Migrate to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

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

        // Verify Room can open the database after migration
        verifyRoomCanOpenDatabase(2)

        dbV2.close()
    }

    @Test
    fun testMigration2To3() {
        // Create v2 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Apply migration 1->2 first
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Add test data with externalId
        dbV2.execSQL("""
            UPDATE patients SET externalId = 'EXT12345' WHERE lastName = 'Doe'
        """)

        // Add another patient with externalId
        dbV2.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, 
                isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
            ) VALUES (
                'Jane', 'Smith', 'smijan000000', 0, '67890', 
                0, 1, 0, 0, 'EXT67890', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
        """)

        dbV2.close()

        // Migrate to v3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Verify data survived migration
        val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Smith'")
        cursor.use {
            assertTrue("Test data should exist after migration", cursor.moveToFirst())
            assertEquals("Jane", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
            assertEquals("EXT67890", cursor.getString(cursor.getColumnIndexOrThrow("externalId")))
        }

        // Verify index exists
        val indexes = getTableIndexes(dbV3, "patients")
        assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

        // Verify Room can open the database after migration
        verifyRoomCanOpenDatabase(3)

        dbV3.close()
    }

    @Test
    fun testMigration1To3() {
        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Migrate directly from 1 to 3 using the direct migration
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_3)

        // Verify data survived migration
        val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Doe'")
        cursor.use {
            assertTrue("Test data should exist after migration", cursor.moveToFirst())
            assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
        }

        // Verify new column exists
        val columns = getTableColumns(dbV3, "patients")
        assertTrue("externalId column should exist", "externalId" in columns)

        // Verify index exists
        val indexes = getTableIndexes(dbV3, "patients")
        assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

        // Verify Room can open the database after migration
        verifyRoomCanOpenDatabase(3)

        dbV3.close()
    }

    @Test
    fun testAllMigrations() {
        // Create earliest version of the database
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Test migrating to the latest version using all migrations
        helper.runMigrationsAndValidate(TEST_DB, 3, true, *ALL_MIGRATIONS)

        // Verify Room can open the database after migration
        verifyRoomCanOpenDatabase(3)
    }

    @Test
    fun testDatabaseIntegrityAfterMigrations() {
        // Create earliest version of the database
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        // Apply all migrations
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, *ALL_MIGRATIONS)

        // Verify integrity
        assertTrue("Database should pass integrity check",
            DatabaseUtils.verifyDatabaseIntegrity(dbV3))

        // Verify foreign key constraints are working
        dbV3.execSQL("PRAGMA foreign_keys = ON")
        val fkCheckCursor = dbV3.query("PRAGMA foreign_key_check")
        fkCheckCursor.use {
            assertFalse("There should be no foreign key violations", it.moveToFirst())
        }

        dbV3.close()
    }

    // Helper method to verify Room can open the database
    private fun verifyRoomCanOpenDatabase(expectedVersion: Int) {
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
            val cursor = db.openHelper.readableDatabase.query("SELECT 1")
            cursor.use {
                assertTrue("Database should be queryable", it.moveToFirst())
            }
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

    // Helper method to check if a column has the expected type
    private fun verifyColumnType(db: SupportSQLiteDatabase, tableName: String, columnName: String, expectedType: String) {
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")

            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) {
                    assertEquals("Column $columnName should have type $expectedType",
                        expectedType, cursor.getString(typeIndex))
                    return
                }
            }
            fail("Column $columnName not found in table $tableName")
        }
    }
}