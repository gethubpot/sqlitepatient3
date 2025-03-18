package com.example.sqlitepatient3.data.local.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Test suite for database migrations.
 * This class contains comprehensive tests for all migration paths between database versions.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTestSuite {

    private lateinit var migrationHelper: MigrationTestHelper

    @Before
    fun setup() {
        migrationHelper = MigrationTestHelper()
    }

    @After
    fun tearDown() {
        migrationHelper.closeAndCleanup()
    }

    @Test
    fun testMigrationFrom1To2() {
        // Create version 1 database with test data
        val dbV1 = migrationHelper.createDatabase(1) { db ->
            // Add test data for v1
            db.execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, 
                    isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
                ) VALUES (
                    'John', 'Doe', 'doejoh000000', 1, '12345', 
                    0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """.trimIndent()
            )

            // Add a patient with null fields to ensure they survive the migration
            db.execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, dateOfBirth,
                    isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
                ) VALUES (
                    'Jane', 'Smith', 'smijan000000', 0, '67890', NULL,
                    1, 1, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """.trimIndent()
            )
        }

        // Close v1 database
        dbV1.close()

        // Migrate to v2
        val dbV2 = migrationHelper.runMigration(1, 2)

        // Verify data survived migration
        val cursor = dbV2.query("SELECT * FROM patients WHERE lastName = 'Doe'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("John", it.getString(it.getColumnIndex("firstName")))
            assertEquals("Doe", it.getString(it.getColumnIndex("lastName")))
            assertEquals("doejoh000000", it.getString(it.getColumnIndex("upi")))
            assertEquals(1, it.getInt(it.getColumnIndex("isMale")))
        }

        // Verify the Smith record with a NULL dateOfBirth survived
        val cursor2 = dbV2.query("SELECT * FROM patients WHERE lastName = 'Smith'")
        cursor2.use {
            assertTrue(it.moveToFirst())
            assertEquals("Jane", it.getString(it.getColumnIndex("firstName")))
            assertEquals(1, it.getInt(it.getColumnIndex("onCcm")))
            assertTrue(it.isNull(it.getColumnIndex("dateOfBirth")))
        }

        // Verify new column exists
        val columns = migrationHelper.getTableColumns(dbV2, "patients")
        assertTrue("externalId column should exist", "externalId" in columns)

        // Verify system properties was updated with migration info
        val propCursor = dbV2.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
        propCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("1_to_2", it.getString(0))
        }

        // Test inserting data using the new column
        dbV2.execSQL("UPDATE patients SET externalId = 'EXT001' WHERE lastName = 'Doe'")
        val updatedCursor = dbV2.query("SELECT externalId FROM patients WHERE lastName = 'Doe'")
        updatedCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("EXT001", it.getString(0))
        }

        dbV2.close()
    }

    @Test
    fun testMigrationFrom2To3() {
        // Create version 2 database with test data
        val dbV2 = migrationHelper.createDatabase(2) { db ->
            // Add test data with externalId field
            db.execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, 
                    isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
                ) VALUES (
                    'Robert', 'Johnson', 'johrob000000', 1, '55555', 
                    0, 0, 0, 0, 'EXT002', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """.trimIndent()
            )

            // Add a record with duplicate externalId to test index handling
            db.execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, 
                    isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
                ) VALUES (
                    'Sarah', 'Connor', 'consar000000', 0, '99999', 
                    0, 0, 0, 0, 'EXT003', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """.trimIndent()
            )
        }

        // Close v2 database
        dbV2.close()

        // Migrate to v3
        val dbV3 = migrationHelper.runMigration(2, 3)

        // Verify data survived migration
        val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Johnson'")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("Robert", it.getString(it.getColumnIndex("firstName")))
            assertEquals("EXT002", it.getString(it.getColumnIndex("externalId")))
        }

        // Verify index exists
        val indexes = migrationHelper.getTableIndexes(dbV3, "patients")
        assertTrue("externalId index should exist", indexes.any { it.contains("index_patients_externalId") })

        // Verify system properties was updated with migration info
        val propCursor = dbV3.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
        propCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("2_to_3", it.getString(0))
        }

        // Test that the index works by querying on externalId
        val indexCursor = dbV3.query("EXPLAIN QUERY PLAN SELECT * FROM patients WHERE externalId = 'EXT002'")
        var usesIndex = false
        indexCursor.use {
            while (it.moveToNext()) {
                val explanation = it.getString(it.getColumnIndex("detail"))
                if (explanation.contains("USING INDEX") && explanation.contains("externalId")) {
                    usesIndex = true
                    break
                }
            }
        }
        assertTrue("Query should use the externalId index", usesIndex)

        dbV3.close()
    }

    @Test
    fun testMigrationFrom1To3() {
        // Create version 1 database with test data
        val dbV1 = migrationHelper.createDatabase(1) { db ->
            // Set up more complex test data
            migrationHelper.setupTestDataForVersion(db, 1)
        }

        // Close v1 database
        dbV1.close()

        // Migrate directly from 1 to 3
        val dbV3 = migrationHelper.runMigration(1, 3)

        // Verify data survived migration
        val cursor = dbV3.query("SELECT COUNT(*) FROM patients")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertTrue("Patients should exist after migration", it.getInt(0) > 0)
        }

        // Verify both migrations were applied
        val columns = migrationHelper.getTableColumns(dbV3, "patients")
        assertTrue("externalId column should exist", "externalId" in columns)

        val indexes = migrationHelper.getTableIndexes(dbV3, "patients")
        assertTrue("externalId index should exist", indexes.any { it.contains("index_patients_externalId") })

        // Test that we can use the new column and index
        dbV3.execSQL("UPDATE patients SET externalId = 'CHAIN000' WHERE lastName = 'Doe'")
        val updatedCursor = dbV3.query("SELECT externalId FROM patients WHERE externalId = 'CHAIN000'")
        updatedCursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("CHAIN000", it.getString(0))
        }

        dbV3.close()
    }

    @Test
    fun testAllMigrationsAreValid() {
        // Test all migrations in sequence
        val dbV1 = migrationHelper.createDatabase(1) { db ->
            migrationHelper.setupTestDataForVersion(db, 1)
        }
        dbV1.close()

        // V1 -> V2
        val dbV2 = migrationHelper.runMigration(1, 2)
        assertTrue("Database should pass integrity check after 1->2 migration",
            migrationHelper.validateDatabaseIntegrity(dbV2))
        dbV2.close()

        // V2 -> V3
        val dbV3 = migrationHelper.runMigration(2, 3)
        assertTrue("Database should pass integrity check after 2->3 migration",
            migrationHelper.validateDatabaseIntegrity(dbV3))
        dbV3.close()
    }

    @Test
    fun testDatabaseAutoRecovery() {
        // Create version 3 database with test data
        val dbV3 = migrationHelper.createDatabase(3) { db ->
            migrationHelper.setupTestDataForVersion(db, 3)
        }

        // Simulate database corruption by dropping an index
        try {
            dbV3.execSQL("DROP INDEX index_patients_upi")
        } catch (e: Exception) {
            // If the index doesn't exist, that's fine
        }

        // Check that recovery mechanisms work
        assertTrue("Database auto-recovery should rebuild indexes",
            DatabaseUtils.verifyDatabaseIntegrity(dbV3))

        dbV3.close()
    }
}