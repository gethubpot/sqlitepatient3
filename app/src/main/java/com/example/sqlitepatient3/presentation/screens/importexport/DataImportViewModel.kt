package com.example.sqlitepatient3.presentation.screens.importexport

import android.content.Context
import android.net.Uri
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

enum class ImportType {
    PATIENTS, FACILITIES, DIAGNOSES
}

@HiltViewModel
class DataImportViewModel @Inject constructor(
    private val csvImporter: CsvImporter
) : ViewModel() {

    private val TAG = "DataImportViewModel"

    // UI State
    private val _importType = MutableStateFlow(ImportType.PATIENTS)
    val importType: StateFlow<ImportType> = _importType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    // Selected file
    private var selectedFileUri: Uri? = null

    fun setImportType(type: ImportType) {
        _importType.value = type
    }

    fun setSelectedFile(uri: Uri, context: Context) {
        selectedFileUri = uri

        // Get file name
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex("_display_name")
            cursor.moveToFirst()
            val name = cursor.getString(nameIndex)
            _fileName.value = name
        }
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun importData(context: Context) {
        if (selectedFileUri == null) {
            _importResult.value = "No file selected"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = when (_importType.value) {
                    ImportType.PATIENTS -> importPatients(context)
                    ImportType.FACILITIES -> importFacilities(context)
                    ImportType.DIAGNOSES -> importDiagnoses(context)
                }

                _importResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "Error importing data", e)
                _importResult.value = "Error: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun importPatients(context: Context): String {
        val uri = selectedFileUri ?: return "No file selected"

        return try {
            val (successCount, errorCount) = csvImporter.importPatients(context, uri)
            "Import complete: $successCount patients imported, $errorCount errors"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun importFacilities(context: Context): String {
        val uri = selectedFileUri ?: return "No file selected"

        return try {
            val (successCount, errorCount) = csvImporter.importFacilities(context, uri)
            "Import complete: $successCount facilities imported, $errorCount errors"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private suspend fun importDiagnoses(context: Context): String {
        val uri = selectedFileUri ?: return "No file selected"

        return try {
            val (diagnosisSuccessCount, diagnosisErrorCount, unmatchedPatientCount) =
                csvImporter.importDiagnoses(context, uri)

            val resultBuilder = StringBuilder("Import complete: ")
            resultBuilder.append("$diagnosisSuccessCount diagnoses imported, ")
            resultBuilder.append("$diagnosisErrorCount errors")

            if (unmatchedPatientCount > 0) {
                resultBuilder.append(", $unmatchedPatientCount diagnoses skipped due to unmatched patients")
            }

            resultBuilder.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}