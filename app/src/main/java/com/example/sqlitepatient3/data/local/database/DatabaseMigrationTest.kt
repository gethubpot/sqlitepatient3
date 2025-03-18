package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sqlitepatient3.data.local.database.AppDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"
    private val MIGRATION_1_2 = DatabaseMigrations.MIGRATION_1_2
    private val MIGRATION_2_3 = DatabaseMigrations.MIGRATION_2_3
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)

    private lateinit var context: Context

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cleanup() {
        context.deleteDatabase(TEST_DB)
    }

    @Test
    fun testMigration1To2() {
        // Create v1 database with test data
        val dbV1 = helper.createDatabase(TEST_DB, 1).apply {
            // Add test data with minimal required fields
            execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, 
                    isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
                ) VALUES (
                    'John', 'Doe', 'doejoh000000', 1, '12345', 
                    0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """
            )
            close()
        }

        // Migrate to v2
        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Verify data survived migration
        val cursor = dbV2.query("SELECT * FROM patients WHERE lastName = 'Doe'")
        cursor.use {
            assertTrue(cursor.moveToFirst())
            assertEquals("John", cursor.getString(cursor.getColumnIndex("firstName")))
            assertEquals("Doe", cursor.getString(cursor.getColumnIndex("lastName")))
            assertEquals("doejoh000000", cursor.getString(cursor.getColumnIndex("upi")))
        }

        // Verify new column exists
        val columns = getTableColumns(dbV2, "patients")
        assertTrue("externalId" in columns)

        // Verify system properties was updated with migration info
        val propCursor = dbV2.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
        propCursor.use {
            assertTrue(propCursor.moveToFirst())
            assertEquals("1_to_2", propCursor.getString(0))
        }

        dbV2.close()
    }

    @Test
    fun testMigration2To3() {
        // Create v2 database with test data
        val dbV2 = helper.createDatabase(TEST_DB, 2).apply {
            // Add test data including the externalId column from v2
            execSQL(
                """
                INSERT INTO patients (
                    firstName, lastName, upi, isMale, medicareNumber, 
                    isHospice, onCcm, onPsych, onPsyMed, externalId, createdAt, updatedAt
                ) VALUES (
                    'Jane', 'Smith', 'smijan000000', 0, '67890', 
                    0, 1, 0, 0, 'EXT12345', ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                )
                """
            )
            close()
        }

        // Migrate to v3
        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Verify data survived migration
        val cursor = dbV3.query("SELECT * FROM patients WHERE lastName = 'Smith'")
        cursor.use {
            assertTrue(cursor.moveToFirst())
            assertEquals("Jane", cursor.getString(cursor.getColumnIndex("firstName")))
            assertEquals("EXT12345", cursor.getString(cursor.getColumnIndex("externalId")))
        }

        // Verify index exists
        val indexes = getTableIndexes(dbV3, "patients")
        assertTrue(indexes.any { it.contains("index_patients_externalId") })

        // Verify system properties was updated with migration info
        val propCursor = dbV3.query("SELECT value FROM system_properties WHERE key = 'last_migration'")
        propCursor.use {
            assertTrue(propCursor.moveToFirst())
            assertEquals("2_to_3", propCursor.getString(0))
        }

        dbV3.close()
    }

    @Test
    fun testAllMigrations() {
        // Create earliest version of the database
        helper.createDatabase(TEST_DB, 1).close()

        // Test migrating to the latest version
        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build()

        // Verify database is open
        assertTrue(db.isOpen)

        // Close db
        db.close()
    }

    // Helper method to get table columns
    private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        db.query("PRAGMA table_info($tableName)").use { cursor ->
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndex("name")))
            }
        }
        return columns
    }

    // Helper method to get table indexes
    private fun getTableIndexes(db: SupportSQLiteDatabase, tableName: String): List<String> {
        val indexes = mutableListOf<String>()
        db.query("PRAGMA index_list($tableName)").use { cursor ->
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(cursor.getColumnIndex("name")))
            }
        }
        return indexes
    }
}