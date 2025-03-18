package com.example.sqlitepatient3.data.local.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import java.io.FileInputStream

/**
 * Utility class for detecting schema differences between database versions.
 * This helps in analyzing schema changes and planning migrations.
 */
class SchemaDiffUtil {

    /**
     * Represents a difference between schema versions
     */
    sealed class SchemaDifference {
        data class TableAdded(val tableName: String) : SchemaDifference()
        data class TableRemoved(val tableName: String) : SchemaDifference()
        data class ColumnAdded(val tableName: String, val columnName: String, val columnType: String) : SchemaDifference()
        data class ColumnRemoved(val tableName: String, val columnName: String) : SchemaDifference()
        data class ColumnTypeChanged(
            val tableName: String,
            val columnName: String,
            val oldType: String,
            val newType: String
        ) : SchemaDifference()
        data class IndexAdded(val indexName: String, val tableName: String) : SchemaDifference()
        data class IndexRemoved(val indexName: String, val tableName: String) : SchemaDifference()
    }

    /**
     * Table structure with columns and indices
     */
    data class TableSchema(
        val name: String,
        val columns: List<ColumnInfo>,
        val indices: List<IndexInfo>
    )

    /**
     * Column information
     */
    data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val defaultValue: String?
    )

    /**
     * Index information
     */
    data class IndexInfo(
        val name: String,
        val unique: Boolean,
        val columns: List<String>
    )

    /**
     * Database schema with tables
     */
    data class DatabaseSchema(
        val version: Int,
        val tables: List<TableSchema>
    )

    companion object {
        /**
         * Compare schemas between two database versions
         */
        fun compareSchemas(oldVersion: Int, newVersion: Int, schemasDir: File): List<SchemaDifference> {
            val differences = mutableListOf<SchemaDifference>()

            val oldSchema = loadSchema(oldVersion, schemasDir)
            val newSchema = loadSchema(newVersion, schemasDir)

            if (oldSchema == null || newSchema == null) {
                throw IllegalArgumentException("Schema files not found for versions $oldVersion and/or $newVersion")
            }

            // Compare tables
            val oldTables = oldSchema.tables.map { it.name }.toSet()
            val newTables = newSchema.tables.map { it.name }.toSet()

            // Added tables
            differences.addAll((newTables - oldTables).map { SchemaDifference.TableAdded(it) })

            // Removed tables
            differences.addAll((oldTables - newTables).map { SchemaDifference.TableRemoved(it) })

            // For tables in both, check columns
            (oldTables intersect newTables).forEach { tableName ->
                val oldTable = oldSchema.tables.first { it.name == tableName }
                val newTable = newSchema.tables.first { it.name == tableName }

                // Compare columns
                compareColumns(tableName, oldTable.columns, newTable.columns, differences)

                // Compare indices
                compareIndices(tableName, oldTable.indices, newTable.indices, differences)
            }

            return differences
        }

        /**
         * Compares columns between old and new schemas
         */
        private fun compareColumns(
            tableName: String,
            oldColumns: List<ColumnInfo>,
            newColumns: List<ColumnInfo>,
            differences: MutableList<SchemaDifference>
        ) {
            val oldColumnNames = oldColumns.map { it.name }.toSet()
            val newColumnNames = newColumns.map { it.name }.toSet()

            // Added columns
            differences.addAll(
                (newColumnNames - oldColumnNames).map { columnName ->
                    val newColumn = newColumns.first { it.name == columnName }
                    SchemaDifference.ColumnAdded(tableName, columnName, newColumn.type)
                }
            )

            // Removed columns
            differences.addAll(
                (oldColumnNames - newColumnNames).map { columnName ->
                    SchemaDifference.ColumnRemoved(tableName, columnName)
                }
            )

            // Changed column types
            (oldColumnNames intersect newColumnNames).forEach { columnName ->
                val oldColumn = oldColumns.first { it.name == columnName }
                val newColumn = newColumns.first { it.name == columnName }

                if (oldColumn.type != newColumn.type) {
                    differences.add(
                        SchemaDifference.ColumnTypeChanged(
                            tableName,
                            columnName,
                            oldColumn.type,
                            newColumn.type
                        )
                    )
                }
            }
        }

        /**
         * Compares indices between old and new schemas
         */
        private fun compareIndices(
            tableName: String,
            oldIndices: List<IndexInfo>,
            newIndices: List<IndexInfo>,
            differences: MutableList<SchemaDifference>
        ) {
            val oldIndexNames = oldIndices.map { it.name }.toSet()
            val newIndexNames = newIndices.map { it.name }.toSet()

            // Added indices
            differences.addAll(
                (newIndexNames - oldIndexNames).map { indexName ->
                    SchemaDifference.IndexAdded(indexName, tableName)
                }
            )

            // Removed indices
            differences.addAll(
                (oldIndexNames - newIndexNames).map { indexName ->
                    SchemaDifference.IndexRemoved(indexName, tableName)
                }
            )
        }

        /**
         * Load schema from JSON schema file
         */
        private fun loadSchema(version: Int, schemasDir: File): DatabaseSchema? {
            val schemaFile = File(schemasDir, "$version.json")
            if (!schemaFile.exists()) {
                return null
            }

            // In a real implementation, you would parse the JSON schema file
            // Room generates these schema files when you set exportSchema = true
            // For simplicity, this method returns a mock schema in this example
            // You would need to use a JSON parser like Gson or Moshi to parse the actual schema files

            return parseSchemaFile(schemaFile)
        }

        /**
         * Parse a schema file into a DatabaseSchema object
         */
        private fun parseSchemaFile(schemaFile: File): DatabaseSchema {
            // This is a placeholder for actual JSON parsing
            // In a real implementation, you would parse the Room-generated JSON schema files
            // For example, using Gson:
            // return Gson().fromJson(FileReader(schemaFile), DatabaseSchema::class.java)

            // For this example, return a mock schema based on the filename (version)
            val version = schemaFile.nameWithoutExtension.toInt()

            return when (version) {
                1 -> createMockSchemaV1()
                2 -> createMockSchemaV2()
                3 -> createMockSchemaV3()
                else -> throw IllegalArgumentException("Unsupported schema version: $version")
            }
        }

        /**
         * Extract schema from live database
         */
        fun extractSchemaFromDatabase(db: SupportSQLiteDatabase): DatabaseSchema {
            val tables = mutableListOf<TableSchema>()

            // Get all table names
            val tableQuery = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'")
            val tableNames = mutableListOf<String>()

            tableQuery.use { cursor ->
                while (cursor.moveToNext()) {
                    tableNames.add(cursor.getString(0))
                }
            }

            // For each table, get columns and indices
            tableNames.forEach { tableName ->
                val columns = getTableColumns(db, tableName)
                val indices = getTableIndices(db, tableName)

                tables.add(TableSchema(tableName, columns, indices))
            }

            // Get database version
            var version = 1
            try {
                val versionCursor = db.query("PRAGMA user_version")
                versionCursor.use {
                    if (it.moveToFirst()) {
                        version = it.getInt(0)
                    }
                }
            } catch (e: Exception) {
                // Fallback to default version 1
            }

            return DatabaseSchema(version, tables)
        }

        /**
         * Get columns for a table
         */
        private fun getTableColumns(db: SupportSQLiteDatabase, tableName: String): List<ColumnInfo> {
            val columns = mutableListOf<ColumnInfo>()
            val cursor = db.query("PRAGMA table_info($tableName)")

            cursor.use {
                val nameIndex = it.getColumnIndex("name")
                val typeIndex = it.getColumnIndex("type")
                val notNullIndex = it.getColumnIndex("notnull")
                val defaultValueIndex = it.getColumnIndex("dflt_value")

                while (it.moveToNext()) {
                    columns.add(
                        ColumnInfo(
                            name = it.getString(nameIndex),
                            type = it.getString(typeIndex),
                            notNull = it.getInt(notNullIndex) == 1,
                            defaultValue = if (it.isNull(defaultValueIndex)) null else it.getString(defaultValueIndex)
                        )
                    )
                }
            }

            return columns
        }

        /**
         * Get indices for a table
         */
        private fun getTableIndices(db: SupportSQLiteDatabase, tableName: String): List<IndexInfo> {
            val indices = mutableListOf<IndexInfo>()
            val indexListCursor = db.query("PRAGMA index_list($tableName)")

            indexListCursor.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val uniqueIndex = cursor.getColumnIndex("unique")

                while (cursor.moveToNext()) {
                    val indexName = cursor.getString(nameIndex)
                    val isUnique = cursor.getInt(uniqueIndex) == 1

                    // Get columns in this index
                    val indexColumns = mutableListOf<String>()
                    val indexInfoCursor = db.query("PRAGMA index_info($indexName)")

                    indexInfoCursor.use { infoCursor ->
                        val columnNameIndex = infoCursor.getColumnIndex("name")

                        while (infoCursor.moveToNext()) {
                            indexColumns.add(infoCursor.getString(columnNameIndex))
                        }
                    }

                    indices.add(IndexInfo(indexName, isUnique, indexColumns))
                }
            }

            return indices
        }

        // Mock schemas for demonstration - in real code you would parse actual schema files
        private fun createMockSchemaV1(): DatabaseSchema {
            return DatabaseSchema(
                version = 1,
                tables = listOf(
                    TableSchema(
                        name = "patients",
                        columns = listOf(
                            ColumnInfo("id", "INTEGER", true, null),
                            ColumnInfo("firstName", "TEXT", true, null),
                            ColumnInfo("lastName", "TEXT", true, null),
                            ColumnInfo("upi", "TEXT", true, null),
                            ColumnInfo("dateOfBirth", "INTEGER", false, null),
                            ColumnInfo("isMale", "INTEGER", true, null),
                            ColumnInfo("medicareNumber", "TEXT", false, ""),
                            ColumnInfo("facilityId", "INTEGER", false, null),
                            ColumnInfo("isHospice", "INTEGER", true, "0"),
                            ColumnInfo("onCcm", "INTEGER", true, "0"),
                            ColumnInfo("onPsych", "INTEGER", true, "0"),
                            ColumnInfo("onPsyMed", "INTEGER", true, "0"),
                            ColumnInfo("psyMedReviewDate", "INTEGER", false, null),
                            ColumnInfo("createdAt", "INTEGER", true, null),
                            ColumnInfo("updatedAt", "INTEGER", true, null)
                        ),
                        indices = listOf(
                            IndexInfo("index_patients_upi", true, listOf("upi")),
                            IndexInfo("index_patients_facilityId", false, listOf("facilityId"))
                        )
                    )
                    // Other tables would be defined here
                )
            )
        }

        private fun createMockSchemaV2(): DatabaseSchema {
            return DatabaseSchema(
                version = 2,
                tables = listOf(
                    TableSchema(
                        name = "patients",
                        columns = listOf(
                            ColumnInfo("id", "INTEGER", true, null),
                            ColumnInfo("firstName", "TEXT", true, null),
                            ColumnInfo("lastName", "TEXT", true, null),
                            ColumnInfo("upi", "TEXT", true, null),
                            ColumnInfo("dateOfBirth", "INTEGER", false, null),
                            ColumnInfo("isMale", "INTEGER", true, null),
                            ColumnInfo("medicareNumber", "TEXT", false, ""),
                            ColumnInfo("facilityId", "INTEGER", false, null),
                            ColumnInfo("isHospice", "INTEGER", true, "0"),
                            ColumnInfo("onCcm", "INTEGER", true, "0"),
                            ColumnInfo("onPsych", "INTEGER", true, "0"),
                            ColumnInfo("onPsyMed", "INTEGER", true, "0"),
                            ColumnInfo("psyMedReviewDate", "INTEGER", false, null),
                            ColumnInfo("externalId", "TEXT", false, null), // New column in v2
                            ColumnInfo("createdAt", "INTEGER", true, null),
                            ColumnInfo("updatedAt", "INTEGER", true, null)
                        ),
                        indices = listOf(
                            IndexInfo("index_patients_upi", true, listOf("upi")),
                            IndexInfo("index_patients_facilityId", false, listOf("facilityId"))
                        )
                    )
                    // Other tables would be defined here
                )
            )
        }

        private fun createMockSchemaV3(): DatabaseSchema {
            return DatabaseSchema(
                version = 3,
                tables = listOf(
                    TableSchema(
                        name = "patients",
                        columns = listOf(
                            ColumnInfo("id", "INTEGER", true, null),
                            ColumnInfo("firstName", "TEXT", true, null),
                            ColumnInfo("lastName", "TEXT", true, null),
                            ColumnInfo("upi", "TEXT", true, null),
                            ColumnInfo("dateOfBirth", "INTEGER", false, null),
                            ColumnInfo("isMale", "INTEGER", true, null),
                            ColumnInfo("medicareNumber", "TEXT", false, ""),
                            ColumnInfo("facilityId", "INTEGER", false, null),
                            ColumnInfo("isHospice", "INTEGER", true, "0"),
                            ColumnInfo("onCcm", "INTEGER", true, "0"),
                            ColumnInfo("onPsych", "INTEGER", true, "0"),
                            ColumnInfo("onPsyMed", "INTEGER", true, "0"),
                            ColumnInfo("psyMedReviewDate", "INTEGER", false, null),
                            ColumnInfo("externalId", "TEXT", false, null),
                            ColumnInfo("createdAt", "INTEGER", true, null),
                            ColumnInfo("updatedAt", "INTEGER", true, null)
                        ),
                        indices = listOf(
                            IndexInfo("index_patients_upi", true, listOf("upi")),
                            IndexInfo("index_patients_facilityId", false, listOf("facilityId")),
                            IndexInfo("index_patients_externalId", false, listOf("externalId")) // New index in v3
                        )
                    )
                    // Other tables would be defined here
                )
            )
        }
    }
}