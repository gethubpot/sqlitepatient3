package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test.db"
    private val MIGRATION_1_2 = DatabaseMigrations.MIGRATION_1_2
    private val MIGRATION_2_3 = DatabaseMigrations.MIGRATION_2_3
    private val MIGRATION_1_3 = DatabaseMigrations.MIGRATION_1_3
    private val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)

    private val logger = Logger.getLogger(DatabaseMigrationTest::class.java.name)
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
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS patients (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                firstName TEXT NOT NULL,
                lastName TEXT NOT NULL,
                upi TEXT NOT NULL,
                isMale INTEGER NOT NULL,
                medicareNumber TEXT,
                facilityId INTEGER NOT NULL,
                isHospice INTEGER NOT NULL DEFAULT 0,
                onCcm INTEGER NOT NULL DEFAULT 0,
                onPsych INTEGER NOT NULL DEFAULT 0,
                onPsyMed INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (facilityId) REFERENCES facilities(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_patients_upi ON patients(upi)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_patients_facilityId ON patients(facilityId)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL
            )
        """)

        db.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber,
                facilityId, isHospice, onCcm, onPsych, onPsyMed,
                createdAt, updatedAt
            ) VALUES (
                'John', 'Doe', 'doejoh000000', 1, '12345',
                1, 0, 0, 0, 0,
                ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
            )
        """)
    }

    @Test
    fun testMigration1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        dbV2.query("SELECT * FROM patients WHERE lastName = 'Doe'").use { cursor ->
            assertTrue("Test data should exist after migration", cursor.moveToFirst())
            assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
            assertEquals("Doe", cursor.getString(cursor.getColumnIndexOrThrow("lastName")))
            assertEquals("doejoh000000", cursor.getString(cursor.getColumnIndexOrThrow("upi")))
        }

        val columns = getTableColumns(dbV2, "patients")
        assertTrue("externalId column should exist", "externalId" in columns)

        dbV2.query("SELECT value FROM system_properties WHERE key = 'last_migration'").use { cursor ->
            assertTrue("Migration metadata should be recorded", cursor.moveToFirst())
            assertEquals("1_to_2", cursor.getString(0))
        }

        verifyRoomCanOpenDatabase(2)
        dbV2.close()
    }

    @Test
    fun testMigration2To3() {
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        val dbV2 = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        dbV2.execSQL("""
            UPDATE patients SET externalId = 'EXT12345' WHERE lastName = 'Doe'
        """)

        dbV2.execSQL("""
            INSERT INTO patients (
                firstName, lastName, upi, isMale, medicareNumber,
                facilityId, isHospice, onCcm, onPsych, onPsyMed,
                createdAt, updatedAt, externalId
            ) VALUES (
                'Jane', 'Smith', 'smijan000000', 0, '67890',
                1, 0, 0, 0, 0,
                ${System.currentTimeMillis()}, ${System.currentTimeMillis()},
                'EXT67890'
            )
        """)

        dbV2.close()

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        dbV3.query("SELECT * FROM patients WHERE lastName = 'Smith'").use { cursor ->
            assertTrue("Test data should exist after migration", cursor.moveToFirst())
            assertEquals("Jane", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
            assertEquals("EXT67890", cursor.getString(cursor.getColumnIndexOrThrow("externalId")))
        }

        val indexes = getTableIndexes(dbV3, "patients")
        assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

        verifyRoomCanOpenDatabase(3)
        dbV3.close()
    }

    @Test
    fun testMigration1To3() {
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        val dbV3 = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_1_3)

        dbV3.query("SELECT * FROM patients WHERE lastName = 'Doe'").use { cursor ->
            assertTrue("Test data should exist after migration", cursor.moveToFirst())
            assertEquals("John", cursor.getString(cursor.getColumnIndexOrThrow("firstName")))
        }

        val columns = getTableColumns(dbV3, "patients")
        assertTrue("externalId column should exist", "externalId" in columns)

        val indexes = getTableIndexes(dbV3, "patients")
        assertTrue("Index on externalId should exist", indexes.any { it.contains("index_patients_externalId") })

        verifyRoomCanOpenDatabase(3)
        dbV3.close()
    }

    @Test
    fun testAllMigrations() {
        helper.createDatabase(TEST_DB, 1).apply {
            createV1Database(this)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 3, true, *ALL_MIGRATIONS)
        verifyRoomCanOpenDatabase(3)
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

    private fun getTableIndexes(db: SupportSQLiteDatabase, tableName: String): List<String> {
        return db.query("PRAGMA index_list($tableName)").use { cursor ->
            val indexes = mutableListOf<String>()
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                indexes.add(cursor.getString(nameIndex))
            }
            indexes
        }
    }
}