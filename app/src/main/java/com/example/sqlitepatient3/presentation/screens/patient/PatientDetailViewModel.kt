package com.example.sqlitepatient3.presentation.screens.patient

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetActivePatientDiagnosesUseCase
import com.example.sqlitepatient3.domain.usecase.diagnosis.GetDiagnosticCodeByIcdCodeUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByPatientUseCase
import com.example.sqlitepatient3.domain.usecase.facility.GetFacilityByIdUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

// Data class remains the same
data class PatientDiagnosisWithDescription(
    val diagnosis: PatientDiagnosis,
    val description: String?
)

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val getEventsByPatientUseCase: GetEventsByPatientUseCase,
    private val getActivePatientDiagnosesUseCase: GetActivePatientDiagnosesUseCase,
    private val getFacilityByIdUseCase: GetFacilityByIdUseCase,
    private val getDiagnosticCodeByIcdCodeUseCase: GetDiagnosticCodeByIcdCodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val patientId: Long = savedStateHandle.get<Long>("patientId") ?:
    throw IllegalArgumentException("Patient ID is required")

    // UI state remains the same
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    // --- MODIFIED: Expose facilityCode instead of facilityName ---
    private val _facilityCode = MutableStateFlow<String?>(null)
    val facilityCode: StateFlow<String?> = _facilityCode.asStateFlow()
    // --- END MODIFICATION ---

    private val _recentEvents = MutableStateFlow<List<Event>>(emptyList())
    val recentEvents: StateFlow<List<Event>> = _recentEvents.asStateFlow()

    private val _diagnosesWithDescriptions = MutableStateFlow<List<PatientDiagnosisWithDescription>>(emptyList())
    val diagnosesWithDescriptions: StateFlow<List<PatientDiagnosisWithDescription>> = _diagnosesWithDescriptions.asStateFlow()

    init {
        loadPatientData()
    }

    private fun loadPatientData() {
        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Fetch Patient
                val patientResult = getPatientByIdUseCase(patientId)
                if (patientResult == null) {
                    _errorMessage.value = "Patient not found"
                    _patient.value = null
                    _isLoading.value = false
                    return@launch
                }
                _patient.value = patientResult

                // 2. Fetch Facility Code
                patientResult.facilityId?.let { facId ->
                    try {
                        val facility = getFacilityByIdUseCase(facId)
                        // --- MODIFIED: Assign facilityCode ---
                        _facilityCode.value = facility?.facilityCode
                        // --- END MODIFICATION ---
                    } catch (e: Exception) {
                        Log.e("PatientDetailVM", "Error loading facility code", e)
                        _errorMessage.value = "Could not load facility details."
                    }
                }

                // 3. Fetch Diagnoses and Descriptions (No change needed here)
                try {
                    val diagnosesList = getActivePatientDiagnosesUseCase(patientId).first()
                    val diagnosesWithDescriptionsList = coroutineScope {
                        diagnosesList.map { diagnosis ->
                            async {
                                val diagnosticCode = getDiagnosticCodeByIcdCodeUseCase(diagnosis.icdCode)
                                PatientDiagnosisWithDescription(
                                    diagnosis = diagnosis,
                                    description = diagnosticCode?.description
                                )
                            }
                        }.awaitAll()
                    }
                    _diagnosesWithDescriptions.value = diagnosesWithDescriptionsList
                } catch (e: Exception) {
                    Log.e("PatientDetailVM", "Error loading diagnoses", e)
                    _errorMessage.value = "Could not load patient diagnoses."
                    _diagnosesWithDescriptions.value = emptyList()
                }

                // 4. Fetch Recent Events (No change needed here)
                try {
                    val eventsList = getEventsByPatientUseCase(patientId).first()
                    _recentEvents.value = eventsList.take(5)
                } catch (e: Exception) {
                    Log.e("PatientDetailVM", "Error loading events", e)
                    _errorMessage.value = "Could not load recent events."
                    _recentEvents.value = emptyList()
                }

                // 5. All essential data fetched, set loading false
                _isLoading.value = false

            } catch (e: CancellationException) {
                Log.w("PatientDetailVM", "Loading cancelled", e)
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("PatientDetailVM", "Error loading patient details", e)
                _errorMessage.value = "Failed to load patient details: ${e.localizedMessage}"
                _patient.value = null
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}