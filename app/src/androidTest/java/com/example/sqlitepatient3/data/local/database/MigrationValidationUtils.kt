package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.collections.iterator

/**
 * Utility for programmatically verifying database migrations against entity definitions.
 * This ensures that your Room entities exactly match your migration paths.
 */
class MigrationVerificationUtils {
    companion object {
        private const val TAG = "MigrationVerification"

        /**
         * Verifies entity classes against the database schema to ensure they match.
         * This is critical for catching mismatches between entity definitions and migrations.
         *
         * @param db The database to verify
         * @param entityClasses The Room entity classes to verify against the schema
         * @return A list of any discrepancies found
         */
        fun verifyEntitiesAgainstSchema(
            db: SupportSQLiteDatabase,
            vararg entityClasses: Class<*>
        ): List<String> {
            val discrepancies = mutableListOf<String>()

            for (entityClass in entityClasses) {
                try {
                    Log.i(TAG, "Verifying entity: ${entityClass.simpleName}")

                    // Get entity table name from @Entity annotation
                    val entityAnnotation = entityClass.getAnnotation(Entity::class.java)
                    val tableName = entityAnnotation?.tableName ?: entityClass.simpleName.lowercase()

                    // Get actual columns from database
                    val tableColumns = getTableColumns(db, tableName)
                    if (tableColumns.isEmpty()) {
                        discrepancies.add("Table $tableName not found in database for entity ${entityClass.simpleName}")
                        continue
                    }

                    // Get expected columns from entity class fields
                    val entityFields = getEntityFields(entityClass)

                    // Check for missing columns
                    for ((fieldName, fieldType) in entityFields) {
                        val column = tableColumns.find { it.name.equals(fieldName, ignoreCase = true) }
                        if (column == null) {
                            discrepancies.add("Column $fieldName (from ${entityClass.simpleName}) not found in table $tableName")
                        } else {
                            // Verify type compatibility
                            if (!isTypeCompatible(fieldType, column.type)) {
                                discrepancies.add(
                                    "Type mismatch for column $fieldName in table $tableName: " +
                                            "expected $fieldType but found ${column.type}"
                                )
                            }
                        }
                    }

                    // Check for extra columns
                    val entityFieldNames = entityFields.keys.map { it.lowercase() }
                    val extraColumns = tableColumns.filter {
                        !entityFieldNames.contains(it.name.lowercase()) &&
                                // Skip room shadow table columns like rowid
                                !it.name.startsWith("rowid") &&
                                !it.name.startsWith("room_")
                    }

                    if (extraColumns.isNotEmpty()) {
                        // This is just a warning, not an error
                        Log.w(TAG, "Extra columns in table $tableName not in entity ${entityClass.simpleName}: ${extraColumns.map { it.name }}")
                    }

                    // Verify indices if available
                    verifyEntityIndices(db, entityClass, tableName, discrepancies)

                } catch (e: Exception) {
                    Log.e(TAG, "Error verifying entity ${entityClass.simpleName}", e)
                    discrepancies.add("Error verifying entity ${entityClass.simpleName}: ${e.message}")
                }
            }

            return discrepancies
        }

        /**
         * Verifies that the database can be opened by Room and queried successfully.
         * This ensures the migrations produce a schema that Room can work with.
         */
        fun verifyRoomCanOpenAndQuery(
            context: Context,
            databaseName: String,
            testQuery: String = "SELECT 1"
        ): Boolean {
            var success = false

            try {
                // Create a temporary Room database instance
                val db = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    databaseName
                ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                    .build()

                try {
                    // Try to open the database
                    db.openHelper.writableDatabase

                    // Try to execute a basic query
                    db.openHelper.readableDatabase.query(testQuery).use { cursor ->
                        success = cursor.moveToFirst()
                    }

                    Log.i(TAG, "Room successfully opened and queried the database")
                } finally {
                    // Always close the database
                    db.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Room failed to open or query the database", e)
                return false
            }

            return success
        }

        /**
         * Performs a comprehensive migration verification by:
         * 1. Verifying entity classes against schema
         * 2. Verifying Room can open and query the database
         * 3. Verifying database integrity
         */
        fun performComprehensiveVerification(
            db: SupportSQLiteDatabase,
            context: Context,
            databaseName: String,
            vararg entityClasses: Class<*>
        ): Boolean {
            Log.i(TAG, "Starting comprehensive migration verification")

            // 1. Verify entity classes against schema
            val discrepancies = verifyEntitiesAgainstSchema(db, *entityClasses)
            if (discrepancies.isNotEmpty()) {
                Log.e(TAG, "Entity-schema verification failed with discrepancies:")
                discrepancies.forEach { Log.e(TAG, "  - $it") }
                return false
            }

            // 2. Verify Room can open and query
            if (!verifyRoomCanOpenAndQuery(context, databaseName)) {
                Log.e(TAG, "Room failed to open and query the database")
                return false
            }

            // 3. Verify database integrity
            if (!DatabaseUtils.verifyDatabaseIntegrity(db)) {
                Log.e(TAG, "Database integrity check failed")
                return false
            }

            // 4. Verify foreign key constraints
            db.execSQL("PRAGMA foreign_keys = ON")
            val fkCheckCursor = db.query("PRAGMA foreign_key_check")
            try {
                if (fkCheckCursor.moveToFirst()) {
                    Log.e(TAG, "Foreign key constraint violations found")
                    return false
                }
            } finally {
                fkCheckCursor.close()
            }

            Log.i(TAG, "Comprehensive migration verification passed successfully")
            return true
        }

        /**
         * Verifies concurrent database access after migrations.
         * This ensures the database can handle multi-threaded access.
         */
        fun testConcurrentAccess(context: Context, databaseName: String, threadCount: Int = 5): Boolean = runBlocking {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
                .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                .build()

            val latch = CountDownLatch(threadCount)
            var success = true

            try {
                // Launch multiple concurrent operations
                repeat(threadCount) { taskId ->
                    withContext(Dispatchers.IO) {
                        try {
                            // Simulate different DB operations
                            when (taskId % 5) {
                                0 -> db.patientDao().getPatientCount()
                                1 -> db.facilityDao().getActiveFacilityCount()
                                2 -> db.patientDao().getAllPatients().collect { /* Just start the flow */ }
                                3 -> db.systemPropertiesDao().getProperty("last_migration")
                                4 -> db.openHelper.readableDatabase.query("SELECT 1").close()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Concurrent access test failed on task $taskId", e)
                            success = false
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // Wait for all operations to complete or timeout
                val completed = latch.await(10, TimeUnit.SECONDS)
                if (!completed) {
                    Log.e(TAG, "Concurrent access test timed out")
                    success = false
                }
            } finally {
                db.close()
            }

            if (success) {
                Log.i(TAG, "Concurrent access test passed successfully")
            }

            return@runBlocking success
        }

        // Helper methods for entity verification

        /**
         * Gets column information for a table
         */
        private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<ColumnInfo> {
            val columns = mutableListOf<ColumnInfo>()

            db.query("PRAGMA table_info($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val notNullIndex = cursor.getColumnIndex("notnull")
                val pkIndex = cursor.getColumnIndex("pk")
                val dfltValueIndex = cursor.getColumnIndex("dflt_value")

                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && typeIndex >= 0) {
                        columns.add(
                            ColumnInfo(
                                name = cursor.getString(nameIndex),
                                type = cursor.getString(typeIndex),
                                notNull = if (notNullIndex >= 0) cursor.getInt(notNullIndex) == 1 else false,
                                primaryKey = if (pkIndex >= 0) cursor.getInt(pkIndex) > 0 else false,
                                defaultValue = if (dfltValueIndex >= 0 && !cursor.isNull(dfltValueIndex))
                                    cursor.getString(dfltValueIndex) else null
                            )
                        )
                    }
                }
            }

            return columns
        }

        /**
         * Gets fields from an entity class that should be mapped to database columns
         */
        private fun getEntityFields(entityClass: Class<*>): Map<String, String> {
            val fields = mutableMapOf<String, String>()

            // Process all fields including those from superclasses
            var currentClass: Class<*>? = entityClass
            while (currentClass != null && currentClass != Any::class.java) {
                for (field in currentClass.declaredFields) {
                    // Skip static fields and transient fields
                    if (Modifier.isStatic(field.modifiers) || Modifier.isTransient(field.modifiers)) {
                        continue
                    }

                    // Skip fields marked with @Ignore annotation
                    if (field.isAnnotationPresent(Ignore::class.java)) {
                        continue
                    }

                    // Get column name from @ColumnInfo annotation or field name
                    val columnName = field.getAnnotation(androidx.room.ColumnInfo::class.java)?.name ?: field.name

                    // Convert Java/Kotlin type to SQLite type
                    val sqliteType = getSqliteTypeForField(field)

                    fields[columnName] = sqliteType
                }

                currentClass = currentClass.superclass
            }

            return fields
        }

        /**
         * Gets the expected SQLite type for a Java/Kotlin field
         */
        private fun getSqliteTypeForField(field: Field): String {
            return when (field.type.name) {
                "int", "java.lang.Integer", "kotlin.Int" -> "INTEGER"
                "long", "java.lang.Long", "kotlin.Long" -> "INTEGER"
                "boolean", "java.lang.Boolean", "kotlin.Boolean" -> "INTEGER"
                "byte", "java.lang.Byte", "kotlin.Byte" -> "INTEGER"
                "short", "java.lang.Short", "kotlin.Short" -> "INTEGER"
                "float", "java.lang.Float", "kotlin.Float" -> "REAL"
                "double", "java.lang.Double", "kotlin.Double" -> "REAL"
                "java.lang.String", "kotlin.String" -> "TEXT"
                "java.util.Date", "java.time.LocalDate", "java.time.LocalDateTime" -> "INTEGER"
                "byte[]", "[B" -> "BLOB"
                else -> {
                    if (field.type.isEnum) {
                        "TEXT"  // Enums are typically stored as strings
                    } else {
                        "UNKNOWN"  // Custom types should be handled by type converters
                    }
                }
            }
        }

        /**
         * Checks if a Java/Kotlin type is compatible with a SQLite type
         */
        private fun isTypeCompatible(javaType: String, sqliteType: String): Boolean {
            // SQLite has dynamic typing, so INTEGER, REAL, TEXT, and BLOB can be compatible
            // in various ways. This is a simplified check.
            return when {
                javaType == "INTEGER" && (sqliteType.contains("INT", ignoreCase = true) ||
                        sqliteType.contains("BOOL", ignoreCase = true)) -> true
                javaType == "REAL" && (sqliteType.contains("REAL", ignoreCase = true) ||
                        sqliteType.contains("FLOAT", ignoreCase = true) ||
                        sqliteType.contains("DOUBLE", ignoreCase = true)) -> true
                javaType == "TEXT" && (sqliteType.contains("TEXT", ignoreCase = true) ||
                        sqliteType.contains("CHAR", ignoreCase = true) ||
                        sqliteType.contains("CLOB", ignoreCase = true)) -> true
                javaType == "BLOB" && sqliteType.contains("BLOB", ignoreCase = true) -> true
                javaType == "UNKNOWN" -> true  // Skip validation for unknown types
                else -> javaType.equals(sqliteType, ignoreCase = true)
            }
        }

        /**
         * Verifies entity indices against database indices
         */
        private fun verifyEntityIndices(
            db: SupportSQLiteDatabase,
            entityClass: Class<*>,
            tableName: String,
            discrepancies: MutableList<String>
        ) {
            // Get indices from entity class
            val entityIndices = mutableListOf<IndexInfo>()

            // Look for @Index annotations in the Entity
            val entityAnnotation = entityClass.getAnnotation(Entity::class.java)
            entityAnnotation?.indices?.forEach { indexAnnotation ->
                val columns = indexAnnotation.value
                val name = if (indexAnnotation.name.isNotEmpty())
                    indexAnnotation.name
                else
                    "index_${tableName}_${columns.joinToString("_")}"

                entityIndices.add(
                    IndexInfo(
                        name = name,
                        unique = indexAnnotation.unique,
                        columns = columns.toList()
                    )
                )
            }

            // Get actual indices from database
            val dbIndices = getTableIndices(db, tableName)

            // Check for missing indices (only check for uniqueness and columns, not names)
            for (entityIndex in entityIndices) {
                val matchingIndices = dbIndices.filter { dbIndex ->
                    dbIndex.unique == entityIndex.unique &&
                            dbIndex.columns.size == entityIndex.columns.size &&
                            dbIndex.columns.containsAll(entityIndex.columns.map { it.lowercase() })
                }

                if (matchingIndices.isEmpty()) {
                    discrepancies.add(
                        "Index from entity ${entityClass.simpleName} not found in table $tableName: " +
                                "columns=${entityIndex.columns}, unique=${entityIndex.unique}"
                    )
                }
            }
        }

        /**
         * Gets indices for a table
         */
        private fun getTableIndices(db: SupportSQLiteDatabase, tableName: String): List<IndexInfo> {
            val indices = mutableListOf<IndexInfo>()

            // Get list of indices
            db.query("PRAGMA index_list($tableName)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val uniqueIndex = cursor.getColumnIndex("unique")

                while (cursor.moveToNext() && nameIndex >= 0) {
                    val indexName = cursor.getString(nameIndex)
                    // Skip system indices and Room-generated indices
                    if (indexName.startsWith("sqlite_") || indexName.startsWith("room_")) {
                        continue
                    }

                    val isUnique = if (uniqueIndex >= 0) cursor.getInt(uniqueIndex) == 1 else false

                    // Get columns for this index
                    val columns = mutableListOf<String>()
                    db.query("PRAGMA index_info($indexName)").use { indexInfoCursor ->
                        val colNameIndex = indexInfoCursor.getColumnIndex("name")
                        while (indexInfoCursor.moveToNext() && colNameIndex >= 0) {
                            columns.add(indexInfoCursor.getString(colNameIndex).lowercase())
                        }
                    }

                    indices.add(
                        IndexInfo(
                            name = indexName,
                            unique = isUnique,
                            columns = columns
                        )
                    )
                }
            }

            return indices
        }
    }

    /**
     * Represents a column in the database schema
     */
    data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val primaryKey: Boolean,
        val defaultValue: String?
    )

    /**
     * Represents an index in the database schema
     */
    data class IndexInfo(
        val name: String,
        val unique: Boolean,
        val columns: List<String>
    )
}