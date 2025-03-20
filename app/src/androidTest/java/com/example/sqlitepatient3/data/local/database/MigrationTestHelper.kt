package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlin.random.Random

class MigrationTestHelper {

    private val roomMigrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDbName = "migration-test-${Random.nextInt(10000)}"

    fun createDatabase(version: Int, populateData: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val db = roomMigrationHelper.createDatabase(testDbName, version)

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS system_properties (
                key TEXT NOT NULL PRIMARY KEY,
                value TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        populateData(db)
        return db
    }

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

    private fun getMigrationsFor(startVersion: Int, endVersion: Int): Array<Migration> {
        return DatabaseMigrations.ALL_MIGRATIONS.filter { migration ->
            migration.startVersion >= startVersion && migration.endVersion <= endVersion
        }.toTypedArray()
    }

    fun getTableNames(db: SupportSQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'").use { cursor ->
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0))
            }
        }
        return tables
    }

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

    fun validateDatabaseIntegrity(db: SupportSQLiteDatabase): Boolean {
        return db.query("PRAGMA integrity_check").use { cursor ->
            cursor.moveToFirst() && cursor.getString(0) == "ok"
        }
    }

    fun setupTestDataForVersion(db: SupportSQLiteDatabase, version: Int) {
        when (version) {
            1 -> setupVersion1TestData(db)
            2 -> setupVersion2TestData(db)
            3 -> setupVersion3TestData(db)
            else -> throw IllegalArgumentException("No test data setup defined for version $version")
        }
    }

    private fun setupVersion1TestData(db: SupportSQLiteDatabase) {
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

    private fun setupVersion2TestData(db: SupportSQLiteDatabase) {
        setupVersion1TestData(db)

        db.execSQL("UPDATE patients SET externalId = 'EXT-001' WHERE lastName = 'Doe'")
        db.execSQL("UPDATE patients SET externalId = 'EXT-002' WHERE lastName = 'Smith'")

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

    private fun setupVersion3TestData(db: SupportSQLiteDatabase) {
        setupVersion2TestData(db)

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
}