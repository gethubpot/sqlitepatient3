package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sqlitepatient3.data.local.database.AppDatabase
import java.io.File
import java.io.IOException
import kotlin.random.Random

/**
 * Helper class for testing Room migrations to ensure database upgrades work correctly.
 * This is a utility wrapper around Room's MigrationTestHelper that provides additional
 * functionality for testing more complex migration scenarios.
 */
class MigrationTestHelper {

    /**
     * Internal Room MigrationTestHelper for executing migrations
     */
    private val roomMigrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test context for file operations
     */
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Temporary test database name
     */
    private val testDbName = "migration-test-${Random.nextInt(10000)}"

    /**
     * Creates a database at the given version with test data
     *
     * @param version The version of the database to create
     * @param populateData Lambda to populate test data into the database
     * @return SupportSQLiteDatabase The created database
     */
    fun createDatabase(version: Int, populateData: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val db = roomMigrationHelper.createDatabase(testDbName, version)

        // Create the system_properties table if it doesn't exist
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Populate with test data
        populateData(db)

        return db
    }

    /**
     * Migrates a database from the start version to the end version
     *
     * @param startVersion The starting version
     * @param endVersion The target version
     * @param validateMigration Whether to validate the migration
     * @return SupportSQLiteDatabase The migrated database
     */
    fun runMigration(
        startVersion: Int,
        endVersion: Int,
        validateMigration: Boolean = true
    ): SupportSQLiteDatabase {
        return roomMigrationHelper.runMigrationsAndValidate(
            testDbName,
            endVersion,
            validateMigration,
            *getMigrationsFor(startVersion, endVersion)
        )
    }

    /**
     * Gets the Migration objects needed to migrate from startVersion to endVersion
     */
    private fun getMigrationsFor(startVersion: Int, endVersion: Int): Array<androidx.room.migration.Migration> {
        return DatabaseMigrations.ALL_MIGRATIONS.filter { migration ->
            migration.startVersion >= startVersion && migration.endVersion <= endVersion
        }.toTypedArray()
    }

    /**
     * Gets a list of table names from the database
     */
    fun getTableNames(db: SupportSQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'").use { cursor ->
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
        }
        return tables
    }

    /**
     * Gets a list of column names for a given table
     */
    fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        return columns
    }

    /**
     * Gets a list of indexes for a given table
     */
    fun getTableIndexes(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val indexes = mutableListOf<String>()
        db.query("PRAGMA index_list($tableName)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(nameIndex))
            }
        }
        return indexes
    }

    /**
     * Validates that the database passes integrity check
     */
    fun validateDatabaseIntegrity(db: SupportSQLiteDatabase): Boolean {
        return db.query("PRAGMA integrity_check").use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "ok"
        }
    }

    /**
     * Executes SQL to set up test data for a specific version
     * This includes predefined data models for each database version
     */
    fun setupTestDataForVersion(db: SupportSQLiteDatabase, version: Int) {
        when (version) {
            1 -> setupVersion1TestData(db)
            2 -> setupVersion2TestData(db)
            3 -> setupVersion3TestData(db)
            else -> throw IllegalArgumentException("No test data setup defined for version $version")
        }
    }

    /**
     * Sets up test data for version 1 database
     */
    private fun setupVersion1TestData(db: SupportSQLiteDatabase) {
        // Create patients
        db.execSQL(
            """
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, dateOfBirth,
                isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
            ) VALUES (
                'John', 'Doe', 'doejoh000000', 1, '123456789A', ${System.currentTimeMillis() - 1000000000},
                0, 1, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, dateOfBirth,
                isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
            ) VALUES (
                'Jane', 'Smith', 'smijan000000', 0, '987654321B', ${System.currentTimeMillis() - 2000000000},
                1, 0, 1, 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
            """.trimIndent()
        )

        // Create facilities
        db.execSQL(
            """
            INSERT INTO facilities (
                name, isActive, address1, city, state, zipCode, createdAt, updatedAt
            ) VALUES (
                'General Hospital', 1, '123 Main St', 'Metropolis', 'NY', '10001', 
                ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
            """.trimIndent()
        )
    }

    /**
     * Sets up test data for version 2 database (includes externalId column)
     */
    private fun setupVersion2TestData(db: SupportSQLiteDatabase) {
        // Set up version 1 data first
        setupVersion1TestData(db)

        // Update patients with external IDs
        db.execSQL("UPDATE patients SET externalId = 'EXT-001' WHERE lastName = 'Doe'")
        db.execSQL("UPDATE patients SET externalId = 'EXT-002' WHERE lastName = 'Smith'")

        // Add a new patient with externalId directly
        db.execSQL(
            """
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, dateOfBirth,
                isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
            ) VALUES (
                'Robert', 'Johnson', 'johrob000000', 1, '555123456C', ${System.currentTimeMillis() - 3000000000},
                0, 0, 0, 0, 'EXT-003', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
            """.trimIndent()
        )
    }

    /**
     * Sets up test data for version 3 database (includes externalId index)
     */
    private fun setupVersion3TestData(db: SupportSQLiteDatabase) {
        // Set up version 2 data first
        setupVersion2TestData(db)

        // No additional data needed for v3 as it only adds an index
        // But we could add more test data if needed for future tests
        db.execSQL(
            """
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber, dateOfBirth,
                isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
            ) VALUES (
                'Alice', 'Williams', 'wilali000000', 0, '789012345D', ${System.currentTimeMillis() - 4000000000},
                1, 1, 0, 0, 'EXT-004', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
            """.trimIndent()
        )
    }

    /**
     * Closes and cleans up the test database
     */
    fun closeAndCleanup() {
        try {
            roomMigrationHelper.closeWhenFinished(true)
            context.deleteDatabase(testDbName)
        } catch (e: IOException) {
            // Just log the error, not much we can do at cleanup time
            e.printStackTrace()
        }
    }
}