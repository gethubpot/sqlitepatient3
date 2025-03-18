package com.example.sqlitepatient3.presentation.screens.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.data.local.database.DatabaseBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    // Enum defining the current operation
    enum class Operation {
        NONE, LOADING, CREATING, RESTORING, DELETING
    }

    private val backupManager = DatabaseBackupManager(application)

    // UI state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _backups = MutableStateFlow<List<File>>(emptyList())
    val backups: StateFlow<List<File>> = _backups.asStateFlow()

    private val _currentOperation = MutableStateFlow(Operation.LOADING)
    val currentOperation: StateFlow<Operation> = _currentOperation.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadBackups()
    }

    /**
     * Loads all available backups
     */
    private fun loadBackups() {
        viewModelScope.launch {
            _isLoading.value = true
            _currentOperation.value = Operation.LOADING

            try {
                val availableBackups = backupManager.getAvailableBackups()
                _backups.value = availableBackups
            } catch (e: Exception) {
                _message.value = "Failed to load backups: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _currentOperation.value = Operation.NONE
            }
        }
    }

    /**
     * Creates a new backup
     */
    suspend fun createBackup() {
        _isLoading.value = true
        _currentOperation.value = Operation.CREATING

        try {
            val backupFile = backupManager.backupDatabase()
            _message.value = "Backup created successfully"

            // Refresh the backup list
            val availableBackups = backupManager.getAvailableBackups()
            _backups.value = availableBackups
        } catch (e: Exception) {
            _message.value = "Failed to create backup: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
            _currentOperation.value = Operation.NONE
        }
    }

    /**
     * Restores data from a backup
     */
    suspend fun restoreBackup(backupFile: File) {
        _isLoading.value = true
        _currentOperation.value = Operation.RESTORING

        try {
            val success = backupManager.restoreDatabase(backupFile)
            if (success) {
                _message.value = "Database restored successfully"
            } else {
                _message.value = "Failed to restore database"
            }
        } catch (e: Exception) {
            _message.value = "Error during restore: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
            _currentOperation.value = Operation.NONE
        }
    }

    /**
     * Deletes a backup file
     */
    suspend fun deleteBackup(backupFile: File) {
        _isLoading.value = true
        _currentOperation.value = Operation.DELETING

        try {
            if (backupFile.delete()) {
                _message.value = "Backup deleted successfully"

                // Refresh the backup list
                val availableBackups = backupManager.getAvailableBackups()
                _backups.value = availableBackups
            } else {
                _message.value = "Failed to delete backup"
            }
        } catch (e: Exception) {
            _message.value = "Error deleting backup: ${e.localizedMessage}"
        } finally {
            _isLoading.value = false
            _currentOperation.value = Operation.NONE
        }
    }

    /**
     * Clears the current message
     */
    fun clearMessage() {
        _message.value = null
    }
}