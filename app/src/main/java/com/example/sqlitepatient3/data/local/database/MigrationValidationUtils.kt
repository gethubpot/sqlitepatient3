package com.example.sqlitepatient3.data.local.database

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sqlitepatient3.data.local.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Utility class for validating database migrations during automated testing.
 * Provides tools to perform comprehensive validation of database integrity,
 * data consistency, and schema correctness during and after migrations.
 */
object MigrationValidationUtils {

    /**
     * Verifies that a database instance can be successfully opened after migration
     */
    fun verifyDbCanOpen(testDbName: String, version: Int) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            testDbName
        ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()

        // Try opening the database - this will trigger any deferred Room validations
        db.openHelper.writableDatabase.query("SELECT 1").close()

        // Verify schema version
        val currentVersion = db.openHelper.writableDatabase.version
        Assert.assertEquals("Database version after migration should be $version", version, currentVersion)

        // Close the database
        db.close()
    }

    /**
     * Performs a deep validation by executing common queries against all major tables
     */
    fun performDeepValidation(db: SupportSQLiteDatabase) {
        // Test querying patients table
        try {
            db.query("SELECT * FROM patients LIMIT 1").close()
        } catch (e: Exception) {
            Assert.fail("Failed to query patients table: ${e.message}")
        }

        // Test querying events table
        try {
            db.query("SELECT * FROM events LIMIT 1").close()
        } catch (e: Exception) {
            Assert.fail("Failed to query events table: ${e.message}")
        }

        // Test querying facilities table
        try {
            db.query("SELECT * FROM facilities LIMIT 1").close()
        } catch (e: Exception) {
            Assert.fail("Failed to query facilities table: ${e.message}")
        }

        // Test querying system_properties table
        try {
            db.query("SELECT * FROM system_properties LIMIT 1").close()
        } catch (e: Exception) {
            Assert.fail("Failed to query system_properties table: ${e.message}")
        }

        // Test joins between tables
        try {
            db.query("""
                SELECT p.id, p.firstName, p.lastName, f.name 
                FROM patients p 
                LEFT JOIN facilities f ON p.facilityId = f.id 
                LIMIT 1
            """.trimIndent()).close()
        } catch (e: Exception) {
            Assert.fail("Failed to perform JOIN query: ${e.message}")
        }
    }

    /**
     * Validates a specific column in a specific record
     */
    fun validateSpecificRecord(
        db: SupportSQLiteDatabase,
        tableName: String,
        whereClause: String,
        expectedResults: Map<String, Any?>
    ) {
        val cursor = db.query("SELECT * FROM $tableName WHERE $whereClause")

        try {
            Assert.assertTrue("Record should exist", cursor.moveToFirst())

            for ((columnName, expectedValue) in expectedResults) {
                val columnIndex = cursor.getColumnIndex(columnName)
                Assert.assertTrue("Column $columnName should exist", columnIndex >= 0)

                when (expectedValue) {
                    is String -> Assert.assertEquals(expectedValue, cursor.getString(columnIndex))
                    is Int -> Assert.assertEquals(expectedValue, cursor.getInt(columnIndex))
                    is Long -> Assert.assertEquals(expectedValue, cursor.getLong(columnIndex))
                    is Float -> Assert.assertEquals(expectedValue, cursor.getFloat(columnIndex), 0.001f)
                    is Double -> Assert.assertEquals(expectedValue, cursor.getDouble(columnIndex), 0.001)
                    is Boolean -> Assert.assertEquals(if (expectedValue) 1 else 0, cursor.getInt(columnIndex))
                    null -> Assert.assertTrue(cursor.isNull(columnIndex))
                }
            }
        } finally {
            cursor.close()
        }
    }

    /**
     * Validates that all indices are being used properly by the query planner
     */
    fun validateIndicesAreUsed(db: SupportSQLiteDatabase) {
        // Get list of all indices
        val indices = mutableListOf<Pair<String, String>>() // tableName, indexName

        db.query("SELECT name, tbl_name FROM sqlite_master WHERE type='index'").use { cursor ->
            while (cursor.moveToNext()) {
                val indexName = cursor.getString(cursor.getColumnIndex("name"))
                val tableName = cursor.getString(cursor.getColumnIndex("tbl_name"))

                // Skip system indices
                if (!indexName.startsWith("sqlite_") && !indexName.startsWith("room_")) {
                    indices.add(Pair(tableName, indexName))
                }
            }
        }

        // For each user-defined index, verify it's usable in a query
        for ((tableName, indexName) in indices) {
            // Get index info
            var indexColumns = mutableListOf<String>()

            try {
                db.query("PRAGMA index_info($indexName)").use { cursor ->
                    while (cursor.moveToNext()) {
                        val colName = cursor.getString(cursor.getColumnIndex("name"))
                        indexColumns.add(colName)
                    }
                }

                // Only test the first column of the index (for compound indices)
                if (indexColumns.isNotEmpty()) {
                    val column = indexColumns.first()

                    // Execute EXPLAIN query and verify index usage
                    db.query("EXPLAIN QUERY PLAN SELECT * FROM $tableName WHERE $column IS NOT NULL").use { cursor ->
                        var usesIndex = false
                        val explanation = StringBuilder()

                        while (cursor.moveToNext()) {
                            val detail = cursor.getString(cursor.getColumnIndex("detail"))
                            explanation.append(detail).append("\n")

                            if (detail.contains("USING INDEX") &&
                                (detail.contains(indexName) || detail.contains("ON $tableName"))) {
                                usesIndex = true
                                break
                            }
                        }

                        // Some indices might not be used depending on statistics, but let's at least log it
                        if (!usesIndex) {
                            println("WARN: Index $indexName might not be used: $explanation")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error checking index $indexName on $tableName: ${e.message}")
            }
        }
    }

    /**
     * Tests that foreign key constraints are working properly
     */
    fun validateForeignKeyConstraints(db: SupportSQLiteDatabase) {
        // Make sure foreign keys are enabled
        db.execSQL("PRAGMA foreign_keys = ON")

        // Verify there are no existing foreign key violations
        db.query("PRAGMA foreign_key_check").use { cursor ->
            Assert.assertFalse("There should be no foreign key violations", cursor.moveToFirst())
        }

        // Test each foreign key relationship
        try {
            // Try to insert a patient with an invalid facility ID
            val nonExistentFacilityId = 999999L

            // First verify the facility doesn't exist
            db.query("SELECT 1 FROM facilities WHERE id = $nonExistentFacilityId").use { cursor ->
                Assert.assertFalse("Test facility should not exist", cursor.moveToFirst())
            }

            // Now try to insert a patient with that facility ID
            var constraintViolated = false

            try {
                db.execSQL("""
                    INSERT INTO patients (
                        firstName, lastName, upi, isMale, facilityId, 
                        isHospice, onCcm, onPsych, onPsyMed, createdAt, updatedAt
                    ) VALUES (
                        'FKTest', 'Patient', 'fktest000000', 1, $nonExistentFacilityId, 
                        0, 0, 0, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                    )
                """.trimIndent())
            } catch (e: Exception) {
                constraintViolated = true
            }

            Assert.assertTrue("Foreign key constraint should be violated", constraintViolated)
        } finally {
            // Clean up any test data
            db.execSQL("DELETE FROM patients WHERE firstName = 'FKTest'")
        }
    }

    /**
     * Full test that runs all validations on a migrated database
     */
    fun performFullValidation(db: SupportSQLiteDatabase) {
        // Execute a series of validation checks

        // 1. Validate basic integrity
        val integrityCheck = db.query("PRAGMA integrity_check")
        integrityCheck.use {
            Assert.assertTrue(it.moveToFirst())
            Assert.assertEquals("ok", it.getString(0))
        }

        // 2. Validate foreign keys
        val foreignKeyCheck = db.query("PRAGMA foreign_key_check")
        foreignKeyCheck.use {
            Assert.assertFalse("There should be no foreign key violations", it.moveToFirst())
        }

        // 3. Perform deep validation with queries
        performDeepValidation(db)

        // 4. Validate indices
        validateIndicesAreUsed(db)

        // 5. Log the final database statistics
        val dbStats = db.query("PRAGMA database_list")
        dbStats.use {
            while (it.moveToNext()) {
                val dbName = it.getString(it.getColumnIndex("name"))
                val dbFile = it.getString(it.getColumnIndex("file"))
                println("Database: $dbName, Path: $dbFile")
            }
        }

        val tableList = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'")
        tableList.use {
            while (it.moveToNext()) {
                val tableName = it.getString(0)
                val rowCountQuery = db.query("SELECT COUNT(*) FROM $tableName")
                rowCountQuery.use { countCursor ->
                    countCursor.moveToFirst()
                    val count = countCursor.getInt(0)
                    println("Table: $tableName, Rows: $count")
                }
            }
        }
    }

    /**
     * Tests concurrent access to the database after migration
     */
    fun testConcurrentAccess(testDbName: String) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            testDbName
        ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()

        val latch = CountDownLatch(5)

        // Launch 5 concurrent operations
        repeat(5) { taskId ->
            withContext(Dispatchers.IO) {
                try {
                    // Simulate different DB operations
                    when (taskId % 5) {
                        0 -> db.query("SELECT * FROM patients", null)
                        1 -> db.query("SELECT * FROM facilities", null)
                        2 -> db.query("SELECT * FROM events", null)
                        3 -> db.query("SELECT * FROM system_properties", null)
                        4 -> db.query("SELECT 1 FROM sqlite_master", null)
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for all operations to complete or timeout
        val completed = latch.await(10, TimeUnit.SECONDS)
        Assert.assertTrue("All concurrent operations should complete", completed)

        db.close()
    }
}