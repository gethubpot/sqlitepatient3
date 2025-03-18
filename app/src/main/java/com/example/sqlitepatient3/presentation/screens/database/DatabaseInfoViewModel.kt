package com.example.sqlitepatient3.presentation.screens.database

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.data.local.database.AppDatabase
import com.example.sqlitepatient3.data.local.database.DatabaseUtils
import com.example.sqlitepatient3.data.local.database.SchemaDiffUtil
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DatabaseInfo(
    val version: Int = 3,  // Updated to match current version
    val lastMigrationDate: String? = null,
    val patientCount: Int = 0,
    val eventCount: Int = 0,
    val facilityCount: Int = 0,
    val databaseSizeBytes: Long = 0,
    val databaseSizeFormatted: String = "0 KB",
    val walSizeBytes: Long = 0,
    val walSizeFormatted: String = "0 KB",
    val integrityPassed: Boolean = true,
    val lastBackupDate: String? = null,
    val tableCount: Int = 0,
    val indexCount: Int = 0,
    val lastMaintenanceDate: String? = null
)

@HiltViewModel
class DatabaseInfoViewModel @Inject constructor(
    private val application: Application,
    private val patientRepository: PatientRepository,
    private val eventRepository: EventRepository,
    private val facilityRepository: FacilityRepository
) : AndroidViewModel(application) {

    private val TAG = "DatabaseInfoViewModel"

    private val _databaseInfo = MutableStateFlow(DatabaseInfo())
    val databaseInfo: StateFlow<DatabaseInfo> = _databaseInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _schemaInfo = MutableStateFlow<List<SchemaTableInfo>>(emptyList())
    val schemaInfo: StateFlow<List<SchemaTableInfo>> = _schemaInfo.asStateFlow()

    init {
        loadDatabaseInfo()
    }

    private fun loadDatabaseInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get database version
                val version = 3  // Hardcoded for now, but could retrieve from BuildConfig or room metadata

                // Get entity counts
                val patientCount = patientRepository.getPatientCount()
                val eventCount = countEvents()
                val facilityCount = facilityRepository.getFacilityCount()

                // Get database file information
                val dbFile = application.getDatabasePath("sqlitepatient3.db")
                val dbSize = dbFile.length()
                val dbSizeFormatted = formatFileSize(dbSize)

                // Get WAL file size if it exists
                val walFile = File(dbFile.path + "-wal")
                val walSize = if (walFile.exists()) walFile.length() else 0L
                val walSizeFormatted = formatFileSize(walSize)

                // Check database integrity
                val db = AppDatabase.getInstance(application).openHelper.writableDatabase
                val integrityPassed = DatabaseUtils.verifyDatabaseIntegrity(db)

                // Get last migration date from system properties (if available)
                val lastMigrationDate = getLastMigrationDate()

                // Get last backup date
                val lastBackupDate = getLastBackupDate()

                // Get last maintenance date
                val lastMaintenanceDate = getLastMaintenanceDate()

                // Get table and index counts
                val schema = SchemaDiffUtil.extractSchemaFromDatabase(db)
                val tableCount = schema.tables.size
                val indexCount = schema.tables.sumOf { it.indices.size }

                _databaseInfo.value = DatabaseInfo(
                    version = version,
                    lastMigrationDate = lastMigrationDate,
                    patientCount = patientCount,
                    eventCount = eventCount,
                    facilityCount = facilityCount,
                    databaseSizeBytes = dbSize,
                    databaseSizeFormatted = dbSizeFormatted,
                    walSizeBytes = walSize,
                    walSizeFormatted = walSizeFormatted,
                    integrityPassed = integrityPassed,
                    lastBackupDate = lastBackupDate,
                    tableCount = tableCount,
                    indexCount = indexCount,
                    lastMaintenanceDate = lastMaintenanceDate
                )

                // Load schema information
                loadSchemaInfo(db)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading database info", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads detailed schema information
     */
    private fun loadSchemaInfo(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        val tableInfoList = mutableListOf<SchemaTableInfo>()

        // Query for all tables
        val tablesCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%'")
        val tableNames = mutableListOf<String>()

        tablesCursor.use { cursor ->
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
        }

        // For each table, get column info and row count
        for (tableName in tableNames) {
            // Get row count
            val rowCountCursor = db.query("SELECT COUNT(*) FROM $tableName")
            var rowCount = 0
            rowCountCursor.use { cursor ->
                if (cursor.moveToFirst()) {
                    rowCount = cursor.getInt(0)
                }
            }

            // Get columns
            val columns = mutableListOf<SchemaColumnInfo>()
            val columnsCursor = db.query("PRAGMA table_info($tableName)")
            columnsCursor.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val notNullIndex = cursor.getColumnIndex("notnull")
                val pkIndex = cursor.getColumnIndex("pk")

                while (cursor.moveToNext()) {
                    columns.add(
                        SchemaColumnInfo(
                            name = cursor.getString(nameIndex),
                            type = cursor.getString(typeIndex),
                            notNull = cursor.getInt(notNullIndex) == 1,
                            primaryKey = cursor.getInt(pkIndex) > 0
                        )
                    )
                }
            }

            // Get indices
            val indices = mutableListOf<SchemaIndexInfo>()
            val indicesCursor = db.query("PRAGMA index_list($tableName)")
            indicesCursor.use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                val uniqueIndex = cursor.getColumnIndex("unique")

                while (cursor.moveToNext()) {
                    val indexName = cursor.getString(nameIndex)
                    val isUnique = cursor.getInt(uniqueIndex) == 1

                    indices.add(SchemaIndexInfo(indexName, isUnique))
                }
            }

            tableInfoList.add(
                SchemaTableInfo(
                    name = tableName,
                    rowCount = rowCount,
                    columns = columns,
                    indices = indices
                )
            )
        }

        _schemaInfo.value = tableInfoList
    }

    private suspend fun countEvents(): Int {
        // Since events are in a Flow, we need to capture a single value
        var count = 0
        try {
            // Query all events and count them
            val db = AppDatabase.getInstance(application).openHelper.readableDatabase
            val cursor = db.query("SELECT COUNT(*) FROM events")
            cursor.use {
                if (it.moveToFirst()) {
                    count = it.getInt(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting events", e)
        }
        return count
    }

    private suspend fun getLastMigrationDate(): String? {
        try {
            val db = AppDatabase.getInstance(application).openHelper.readableDatabase
            val cursor = db.query("SELECT value, updatedAt FROM system_properties WHERE key = 'last_migration'")
            cursor.use {
                if (it.moveToFirst()) {
                    val timestamp = it.getLong(1)
                    return formatDate(timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last migration date", e)
        }
        return null
    }

    private fun getLastBackupDate(): String? {
        // Check for backup directory
        val backupDir = File(application.filesDir, "database_backups")
        if (!backupDir.exists() || !backupDir.isDirectory) {
            return null
        }

        // Find the newest backup file
        val backupFiles = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith("sqlitepatient3_backup_") && file.name.endsWith(".zip")
        }

        if (backupFiles.isNullOrEmpty()) {
            return null
        }

        // Get the most recent backup
        val latestBackup = backupFiles.maxByOrNull { it.lastModified() }
        return latestBackup?.let { formatDate(it.lastModified()) }
    }

    private suspend fun getLastMaintenanceDate(): String? {
        try {
            val db = AppDatabase.getInstance(application).openHelper.readableDatabase
            val cursor = db.query("SELECT updatedAt FROM system_properties WHERE key = 'last_maintenance'")
            cursor.use {
                if (it.moveToFirst()) {
                    val timestamp = it.getLong(0)
                    return formatDate(timestamp)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last maintenance date", e)
        }
        return null
    }

    /**
     * Formats a file size in bytes to a human-readable string
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Formats a timestamp to a readable date string
     */
    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
        return format.format(date)
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = AppDatabase.getInstance(application).openHelper.writableDatabase
                val passed = DatabaseUtils.verifyDatabaseIntegrity(db)
                _databaseInfo.value = _databaseInfo.value.copy(integrityPassed = passed)
            } catch (e: Exception) {
                Log.e(TAG, "Error running integrity check", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun runVacuum() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AppDatabase.performMaintenance(application)
                // Reload the database info after maintenance
                loadDatabaseInfo()
            } catch (e: Exception) {
                Log.e(TAG, "Error running VACUUM", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun runAnalyze() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val db = AppDatabase.getInstance(application).openHelper.writableDatabase
                db.execSQL("ANALYZE")

                // Update maintenance timestamp
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO system_properties (key, value, updatedAt) 
                    VALUES ('last_maintenance', ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        System.currentTimeMillis().toString(),
                        System.currentTimeMillis().toString()
                    )
                )

                // Reload the database info
                loadDatabaseInfo()
            } catch (e: Exception) {
                Log.e(TAG, "Error running ANALYZE", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/**
 * Represents table information in the database schema
 */
data class SchemaTableInfo(
    val name: String,
    val rowCount: Int,
    val columns: List<SchemaColumnInfo>,
    val indices: List<SchemaIndexInfo>
)

/**
 * Represents column information in a table
 */
data class SchemaColumnInfo(
    val name: String,
    val type: String,
    val notNull: Boolean,
    val primaryKey: Boolean
)

/**
 * Represents index information for a table
 */
data class SchemaIndexInfo(
    val name: String,
    val unique: Boolean
)