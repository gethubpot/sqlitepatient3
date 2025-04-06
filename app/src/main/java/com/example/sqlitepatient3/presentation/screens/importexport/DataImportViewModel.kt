package com.example.sqlitepatient3.presentation.screens.importexport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns // Needed to get file name
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.data.importexport.CsvImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Define Enum here or import if defined elsewhere
enum class ImportType {
    PATIENTS, FACILITIES, DIAGNOSES,
    CODE_LIBRARY // <<<--- ADDED NEW IMPORT TYPE
}

@HiltViewModel
class DataImportViewModel @Inject constructor(
    private val csvImporter: CsvImporter
) : ViewModel() {

    private val TAG = "DataImportViewModel"

    // UI State
    private val _importType = MutableStateFlow(ImportType.PATIENTS) // Default to Patients
    val importType: StateFlow<ImportType> = _importType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null) // Result message for Snackbar
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    private val _fileName = MutableStateFlow("") // To display selected file name
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    // Store selected file URI
    private var selectedFileUri: Uri? = null

    fun setImportType(type: ImportType) {
        _importType.value = type
        // Reset file selection when changing type? Optional.
        // selectedFileUri = null
        // _fileName.value = ""
    }

    fun setSelectedFile(uri: Uri, context: Context) {
        selectedFileUri = uri
        // Try to get and display the file name
        _fileName.value = try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: uri.lastPathSegment ?: "Selected File" // Fallback if name not found
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name from URI: $uri", e)
            uri.lastPathSegment ?: "Selected File" // Fallback
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    /**
     * Initiates the data import process based on the selected ImportType.
     */
    fun importData(context: Context) {
        val uri = selectedFileUri
        if (uri == null) {
            _importResult.value = "No file selected. Please select a CSV file first."
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val resultMessage: String = try {
                when (_importType.value) {
                    ImportType.PATIENTS -> {
                        val (success, errors) = csvImporter.importPatients(context, uri)
                        "Patients import complete: $success imported, $errors errors."
                    }
                    ImportType.FACILITIES -> {
                        val (success, errors) = csvImporter.importFacilities(context, uri)
                        "Facilities import complete: $success imported, $errors errors."
                    }
                    ImportType.DIAGNOSES -> {
                        val (success, errors, skipped) = csvImporter.importDiagnoses(context, uri)
                        val skippedMsg = if (skipped > 0) ", $skipped skipped (unmatched patients)" else ""
                        "Diagnoses links import complete: $success linked, $errors errors$skippedMsg."
                    }
                    // --- ADDED CASE for CODE_LIBRARY ---
                    ImportType.CODE_LIBRARY -> {
                        val (updated, inserted, errors) = csvImporter.importDiagnosticCodeLibrary(context, uri)
                        "Code Library import complete: $updated updated, $inserted inserted, $errors errors."
                    }
                    // --- END ADDED CASE ---
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing data for type ${_importType.value}", e)
                "Import failed: ${e.localizedMessage ?: "Unknown error"}" // Provide specific error
            } finally {
                _isLoading.value = false
                // Maybe reset file selection after import?
                // selectedFileUri = null
                // _fileName.value = ""
            }
            _importResult.value = resultMessage // Set result message for snackbar
        }
    }
}