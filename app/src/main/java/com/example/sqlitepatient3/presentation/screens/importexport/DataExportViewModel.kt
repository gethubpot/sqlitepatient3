package com.example.sqlitepatient3.presentation.screens.importexport

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.data.importexport.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Results from export operation
 */
data class ExportResult(
    val message: String,
    val success: Boolean,
    val fileUri: Uri? = null
)

@HiltViewModel
class DataExportViewModel @Inject constructor(
    private val csvExporter: CsvExporter
) : ViewModel() {

    private val TAG = "DataExportViewModel"

    // UI State
    private val _exportType = MutableStateFlow(ExportType.PATIENTS)
    val exportType: StateFlow<ExportType> = _exportType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _exportResult = MutableStateFlow<ExportResult?>(null)
    val exportResult: StateFlow<ExportResult?> = _exportResult.asStateFlow()

    fun setExportType(type: ExportType) {
        _exportType.value = type
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    /**
     * Export data to CSV
     * If outputUri is provided, write to that location
     * Otherwise, use the app's documents directory
     */
    suspend fun exportData(context: Context, outputUri: Uri? = null) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val fileUri = when (_exportType.value) {
                    ExportType.PATIENTS -> csvExporter.exportPatients(context, outputUri)
                    ExportType.FACILITIES -> csvExporter.exportFacilities(context, outputUri)
                }

                val message = if (outputUri != null) {
                    "${_exportType.value.name.lowercase()} exported successfully"
                } else {
                    "${_exportType.value.name.lowercase()} exported to app documents folder"
                }

                _exportResult.value = ExportResult(
                    message = message,
                    success = true,
                    fileUri = fileUri
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting data", e)

                _exportResult.value = ExportResult(
                    message = "Error exporting data: ${e.localizedMessage}",
                    success = false,
                    fileUri = null
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
}