package com.example.sqlitepatient3.presentation.screens.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetActivePatientDiagnosesUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByPatientUseCase
import com.example.sqlitepatient3.domain.usecase.facility.GetFacilityByIdUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val getEventsByPatientUseCase: GetEventsByPatientUseCase,
    private val getActivePatientDiagnosesUseCase: GetActivePatientDiagnosesUseCase,
    private val getFacilityByIdUseCase: GetFacilityByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: Long = savedStateHandle.get<Long>("patientId") ?:
    throw IllegalArgumentException("Patient ID is required")

    // UI state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    private val _facilityName = MutableStateFlow<String?>(null)
    val facilityName: StateFlow<String?> = _facilityName.asStateFlow()

    private val _recentEvents = MutableStateFlow<List<Event>>(emptyList())
    val recentEvents: StateFlow<List<Event>> = _recentEvents.asStateFlow()

    private val _diagnoses = MutableStateFlow<List<PatientDiagnosis>>(emptyList())
    val diagnoses: StateFlow<List<PatientDiagnosis>> = _diagnoses.asStateFlow()

    init {
        loadPatientData()
    }

    private fun loadPatientData() {
        viewModelScope.launch {
            try {
                // Load patient
                val patient = getPatientByIdUseCase(patientId)
                if (patient != null) {
                    _patient.value = patient

                    // Load associated facility if available
                    patient.facilityId?.let { facilityId ->
                        getFacilityByIdUseCase(facilityId)?.let { facility ->
                            _facilityName.value = facility.name
                        }
                    }

                    // Load recent events
                    getEventsByPatientUseCase(patientId)
                        .catch { e -> _errorMessage.value = "Error loading events: ${e.message}" }
                        .collectLatest { events ->
                            _recentEvents.value = events.take(5) // Only show 5 most recent
                        }

                    // Load diagnoses
                    getActivePatientDiagnosesUseCase(patientId)
                        .catch { e -> _errorMessage.value = "Error loading diagnoses: ${e.message}" }
                        .collectLatest { diagnoses ->
                            _diagnoses.value = diagnoses
                        }
                } else {
                    _errorMessage.value = "Patient not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patient: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}