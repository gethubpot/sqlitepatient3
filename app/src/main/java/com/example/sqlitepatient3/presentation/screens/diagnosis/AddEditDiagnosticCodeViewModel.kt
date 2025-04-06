package com.example.sqlitepatient3.presentation.screens.diagnosis // Adjust package if needed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.DiagnosticCode
import com.example.sqlitepatient3.domain.usecase.diagnosis.AddDiagnosticCodeUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetDiagnosticCodeByIdUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.UpdateDiagnosticCodeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditDiagnosticCodeViewModel @Inject constructor(
    private val getDiagnosticCodeByIdUseCase: GetDiagnosticCodeByIdUseCase,
    private val addDiagnosticCodeUseCase: AddDiagnosticCodeUseCase,
    private val updateDiagnosticCodeUseCase: UpdateDiagnosticCodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get code ID from navigation arguments (or null if adding)
    private val codeId: Long? = savedStateHandle.get<Long>("codeId")?.takeIf { it != -1L }

    // --- UI State ---
    private val _isLoading = MutableStateFlow(codeId != null) // Loading only if editing
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // --- Form Fields ---
    private val _icdCode = MutableStateFlow("")
    val icdCode: StateFlow<String> = _icdCode.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _shorthand = MutableStateFlow("")
    val shorthand: StateFlow<String> = _shorthand.asStateFlow() // Use empty string for null

    private val _isBillable = MutableStateFlow(true)
    val isBillable: StateFlow<Boolean> = _isBillable.asStateFlow()

    // Represents 'commonCode' field, mapping Int? to Boolean for UI
    private val _isCommon = MutableStateFlow(false)
    val isCommon: StateFlow<Boolean> = _isCommon.asStateFlow()

    init {
        if (codeId != null) {
            loadCode(codeId)
        } else {
            _isLoading.value = false // Not loading if adding new
        }
    }

    private fun loadCode(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val code = getDiagnosticCodeByIdUseCase(id)
                if (code != null) {
                    _icdCode.value = code.icdCode
                    _description.value = code.description
                    _shorthand.value = code.shorthand ?: ""
                    _isBillable.value = code.billable
                    // Map Int? commonCode to Boolean isCommon
                    _isCommon.value = code.commonCode != null && code.commonCode > 0
                } else {
                    _errorMessage.value = "Diagnostic code not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading code: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // --- Form Field Update Functions ---
    fun setIcdCode(value: String) {
        _icdCode.value = value
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setShorthand(value: String) {
        _shorthand.value = value
    }

    fun setIsBillable(value: Boolean) {
        _isBillable.value = value
    }

    fun setIsCommon(value: Boolean) {
        _isCommon.value = value
    }

    // --- Actions ---
    fun clearError() {
        _errorMessage.value = null
    }

    // Simple validation
    private fun isValid(): Boolean {
        if (_icdCode.value.isBlank()) {
            _errorMessage.value = "ICD-10 Code cannot be empty"
            return false
        }
        // Description check: Enforce non-blank description here if desired,
        // even if you allow blank during import. For manual entry/edit,
        // requiring a description might be good practice.
        if (_description.value.isBlank()) {
            _errorMessage.value = "Description cannot be empty"
            return false
        }
        return true
    }

    fun saveCode() {
        if (!isValid()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                // Map Boolean isCommon back to Int? commonCode for the model
                val commonCodeValue: Int? = if (_isCommon.value) 1 else null // Or 0 if you prefer

                val codeToSave = DiagnosticCode(
                    id = codeId ?: 0, // Use existing ID or 0 for new
                    icdCode = _icdCode.value.trim(),
                    description = _description.value.trim(),
                    shorthand = _shorthand.value.trim().takeIf { it.isNotEmpty() }, // Null if blank
                    billable = _isBillable.value,
                    commonCode = commonCodeValue
                )

                if (codeId == null) {
                    // Add new code
                    addDiagnosticCodeUseCase(
                        icdCode = codeToSave.icdCode,
                        description = codeToSave.description,
                        shorthand = codeToSave.shorthand,
                        billable = codeToSave.billable,
                        commonCode = codeToSave.commonCode
                    )
                } else {
                    // Update existing code
                    updateDiagnosticCodeUseCase(codeToSave)
                }
                _saveSuccess.value = true // Signal success for navigation

            } catch (e: Exception) {
                _errorMessage.value = "Error saving code: ${e.localizedMessage}"
                _saveSuccess.value = false // Ensure success is false on error
            } finally {
                _isSaving.value = false
            }
        }
    }
}