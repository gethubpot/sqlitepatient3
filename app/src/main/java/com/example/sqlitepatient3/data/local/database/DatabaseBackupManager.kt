package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.sqlitepatient3.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages database backup and restore operations.
 * Provides functionality to create, manage, and restore database backups.
 */
class DatabaseBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "DatabaseBackupManager"
        private const val BACKUP_DIR = "database_backups"
        private const val BACKUP_FILENAME_PREFIX = "sqlitepatient3_backup_"
        private const val BACKUP_EXTENSION = ".zip"
        private const val WORK_NAME_BACKUP = "database_backup_work"

        // Exception for database corruption
        class DatabaseCorruptException(message: String) : Exception(message)
    }

    /**
     * Creates a backup of the current database
     * @return The created backup file
     */
    suspend fun backupDatabase(): File = withContext(Dispatchers.IO) {
        // Close any open connections
        AppDatabase.closeInstance()

        // Create backup directory if it doesn't exist
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        // Generate backup filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "$BACKUP_FILENAME_PREFIX$timestamp$BACKUP_EXTENSION")

        try {
            // Get the database file
            val dbFile = context.getDatabasePath("sqlitepatient3.db")
            val sharedPrefsDir = File(context.dataDir, "shared_prefs")

            // Create ZIP file containing the database and shared preferences
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Add database file to ZIP
                addFileToZip(zipOut, dbFile, "database/sqlitepatient3.db")

                // Add WAL and SHM files if they exist
                val walFile = File(dbFile.path + "-wal")
                if (walFile.exists()) {
                    addFileToZip(zipOut, walFile, "database/sqlitepatient3.db-wal")
                }

                val shmFile = File(dbFile.path + "-shm")
                if (shmFile.exists()) {
                    addFileToZip(zipOut, shmFile, "database/sqlitepatient3.db-shm")
                }

                // Add shared preferences if they exist
                if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                    sharedPrefsDir.listFiles()?.forEach { prefFile ->
                        if (prefFile.isFile && prefFile.name.endsWith(".xml")) {
                            addFileToZip(zipOut, prefFile, "preferences/${prefFile.name}")
                        }
                    }
                }
            }

            // Clean up old backups if there are more than 5
            cleanupOldBackups(backupDir, 5)

            // Log success
            Log.i(TAG, "Database backup created successfully at ${backupFile.path}")

            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating database backup", e)
            throw e
        } finally {
            // Reopen database connection
            AppDatabase.getInstance(context)
        }
    }

    /**
     * Restores the database from a backup file
     * @param backupFile The backup file to restore from
     * @return True if the restore was successful
     */
    suspend fun restoreDatabase(backupFile: File): Boolean = withContext(Dispatchers.IO) {
        // Close any open connections
        AppDatabase.closeInstance()

        // Create a temporary directory for extraction
        val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }

        try {
            // Extract the backup
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var zipEntry = zipIn.nextEntry
                while (zipEntry != null) {
                    val entryFile = File(tempDir, zipEntry.name)

                    // Create directories for entry if needed
                    entryFile.parentFile?.mkdirs()

                    if (!zipEntry.isDirectory) {
                        // Copy contents to the entry file
                        FileOutputStream(entryFile).use { fileOut ->
                            zipIn.copyTo(fileOut)
                        }
                    }

                    zipIn.closeEntry()
                    zipEntry = zipIn.nextEntry
                }
            }

            // Copy extracted database file to the app's database location
            val extractedDbFile = File(tempDir, "database/sqlitepatient3.db")
            if (extractedDbFile.exists()) {
                // Copy database file
                val dbFile = context.getDatabasePath("sqlitepatient3.db")
                extractedDbFile.copyTo(dbFile, overwrite = true)

                // Copy WAL and SHM files if they exist
                val extractedWalFile = File(tempDir, "database/sqlitepatient3.db-wal")
                if (extractedWalFile.exists()) {
                    val walFile = File(dbFile.path + "-wal")
                    extractedWalFile.copyTo(walFile, overwrite = true)
                }

                val extractedShmFile = File(tempDir, "database/sqlitepatient3.db-shm")
                if (extractedShmFile.exists()) {
                    val shmFile = File(dbFile.path + "-shm")
                    extractedShmFile.copyTo(shmFile, overwrite = true)
                }

                // Restore shared preferences if they exist
                val extractedPrefsDir = File(tempDir, "preferences")
                if (extractedPrefsDir.exists() && extractedPrefsDir.isDirectory) {
                    val sharedPrefsDir = File(context.dataDir, "shared_prefs")
                    extractedPrefsDir.listFiles()?.forEach { prefFile ->
                        if (prefFile.isFile && prefFile.name.endsWith(".xml")) {
                            prefFile.copyTo(File(sharedPrefsDir, prefFile.name), overwrite = true)
                        }
                    }
                }

                // Validate the restored database
                val db = AppDatabase.getInstance(context)
                val isValid = validateDatabase(db)

                if (!isValid) {
                    Log.e(TAG, "Restored database failed validation")
                    throw DatabaseCorruptException("Restored database failed integrity check")
                }

                Log.i(TAG, "Database restored successfully from ${backupFile.name}")
                return@withContext true
            } else {
                Log.e(TAG, "Database file not found in backup")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring database from backup", e)
            // Try to reopen the original database to prevent the app from crashing
            try {
                AppDatabase.getInstance(context)
            } catch (reopenException: Exception) {
                Log.e(TAG, "Failed to reopen database after failed restore", reopenException)
            }
            throw e
        } finally {
            // Clean up temporary directory
            tempDir.deleteRecursively()
        }
    }

    /**
     * Gets a list of available backups
     * @return List of backup files sorted by date (newest first)
     */
    fun getAvailableBackups(): List<File> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        if (!backupDir.exists()) {
            return emptyList()
        }

        return backupDir.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_FILENAME_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Schedules an automatic backup using WorkManager
     */
    fun scheduleBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val backupWorkRequest = OneTimeWorkRequestBuilder<DatabaseBackupWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_BACKUP,
            ExistingWorkPolicy.REPLACE,
            backupWorkRequest
        )
    }

    /**
     * Validates a database to ensure it's not corrupt
     */
    private fun validateDatabase(db: AppDatabase): Boolean {
        return try {
            val result = db.openHelper.readableDatabase.query("PRAGMA integrity_check")
            result.use {
                it.moveToFirst() && it.getString(0) == "ok"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database validation failed", e)
            false
        }
    }

    /**
     * Adds a file to a ZIP archive
     */
    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryPath: String) {
        if (!file.exists()) return

        zipOut.putNextEntry(ZipEntry(entryPath))
        FileInputStream(file).use { fileIn ->
            val buffer = ByteArray(1024)
            var len: Int
            while (fileIn.read(buffer).also { len = it } > 0) {
                zipOut.write(buffer, 0, len)
            }
        }
        zipOut.closeEntry()
    }

    /**
     * Cleans up old backups, keeping only the most recent ones
     */
    private fun cleanupOldBackups(backupDir: File, keepCount: Int) {
        val backups = backupDir.listFiles { file ->
            file.isFile && file.name.startsWith(BACKUP_FILENAME_PREFIX) && file.name.endsWith(BACKUP_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: return

        if (backups.size > keepCount) {
            backups.drop(keepCount).forEach { file ->
                try {
                    file.delete()
                    Log.d(TAG, "Deleted old backup: ${file.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to delete old backup: ${file.name}", e)
                }
            }
        }
    }

    /**
     * Worker class for performing database backups in the background
     */
    class DatabaseBackupWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ) : Worker(appContext, workerParams) {

        override fun doWork(): Result {
            val backupManager = DatabaseBackupManager(applicationContext)

            return try {
                // Launch backup in a coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    backupManager.backupDatabase()
                }

                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Backup worker failed", e)
                Result.failure()
            }
        }
    }
}