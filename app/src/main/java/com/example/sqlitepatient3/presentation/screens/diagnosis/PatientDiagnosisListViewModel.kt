package com.example.sqlitepatient3.presentation.screens.diagnosis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetActivePatientDiagnosesUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetPatientDiagnosesUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.SetDiagnosisActiveUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.SetHospiceStatusUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import com.example.sqlitepatient3.domain.usecase.patient.UpdatePatientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientDiagnosisListViewModel @Inject constructor(
    private val getPatientDiagnosesUseCase: GetPatientDiagnosesUseCase,
    private val getActivePatientDiagnosesUseCase: GetActivePatientDiagnosesUseCase,
    private val setDiagnosisActiveUseCase: SetDiagnosisActiveUseCase,
    private val setHospiceStatusUseCase: SetHospiceStatusUseCase,
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val updatePatientUseCase: UpdatePatientUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get patient ID from navigation arguments
    private val patientId: Long = savedStateHandle.get<Long>("patientId") ?:
    throw IllegalArgumentException("Patient ID is required")

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _patientWithDiagnoses = MutableStateFlow<Pair<Patient, List<PatientDiagnosis>>?>(null)
    val patientWithDiagnoses: StateFlow<Pair<Patient, List<PatientDiagnosis>>?> = _patientWithDiagnoses.asStateFlow()

    private val _showActiveOnly = MutableStateFlow(true)
    val showActiveOnly: StateFlow<Boolean> = _showActiveOnly.asStateFlow()

    init {
        loadPatientWithDiagnoses()
    }

    private fun loadPatientWithDiagnoses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_showActiveOnly.value) {
                    // Get only active diagnoses
                    getActivePatientDiagnosesUseCase(patientId).collectLatest { diagnoses ->
                        // Get patient separately since we're not using the relation here
                        val patient = getPatientByIdUseCase(patientId)
                        if (patient != null) {
                            _patientWithDiagnoses.value = Pair(patient, diagnoses)
                        } else {
                            _errorMessage.value = "Patient not found"
                        }
                    }
                } else {
                    // Get all diagnoses including inactive ones
                    getPatientDiagnosesUseCase(patientId).collectLatest { diagnoses ->
                        // Get patient separately since we're not using the relation here
                        val patient = getPatientByIdUseCase(patientId)
                        if (patient != null) {
                            _patientWithDiagnoses.value = Pair(patient, diagnoses)
                        } else {
                            _errorMessage.value = "Patient not found"
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patient diagnoses: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleActiveOnly(showActiveOnly: Boolean) {
        if (this._showActiveOnly.value != showActiveOnly) {
            this._showActiveOnly.value = showActiveOnly
            loadPatientWithDiagnoses()
        }
    }

    fun setDiagnosisActive(diagnosisId: Long, active: Boolean) {
        viewModelScope.launch {
            try {
                val success = setDiagnosisActiveUseCase(diagnosisId, active)
                if (!success) {
                    _errorMessage.value = "Failed to update diagnosis status"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating diagnosis: ${e.localizedMessage}"
            }
        }
    }

    fun setHospiceStatus(diagnosisId: Long, isHospiceCode: Boolean) {
        viewModelScope.launch {
            try {
                val success = setHospiceStatusUseCase(diagnosisId, isHospiceCode)
                if (success && isHospiceCode) {
                    // If we're setting this as a hospice code, make it the patient's primary hospice diagnosis
                    val patient = _patientWithDiagnoses.value?.first
                    if (patient != null) {
                        updatePatientUseCase(patient.copy(
                            hospiceDiagnosisId = diagnosisId
                        ))
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating hospice status: ${e.localizedMessage}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}