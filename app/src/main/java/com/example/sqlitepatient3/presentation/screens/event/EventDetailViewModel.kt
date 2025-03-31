package com.example.sqlitepatient3.presentation.screens.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.usecase.event.GetEventByIdUseCase
import com.example.sqlitepatient3.domain.usecase.facility.GetFacilityByIdUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val getFacilityByIdUseCase: GetFacilityByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get event ID from navigation arguments
    private val eventId: Long = savedStateHandle.get<Long>("eventId")
        ?: throw IllegalArgumentException("Event ID is required for EventDetailScreen")

    // UI State
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    private val _facility = MutableStateFlow<Facility?>(null)
    val facility: StateFlow<Facility?> = _facility.asStateFlow()

    init {
        loadEventDetails()
    }

    private fun loadEventDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val eventResult = getEventByIdUseCase(eventId)
                _event.value = eventResult

                if (eventResult != null) {
                    // Load associated patient
                    val patientResult = getPatientByIdUseCase(eventResult.patientId)
                    _patient.value = patientResult

                    // Load associated facility if patient and facilityId exist
                    patientResult?.facilityId?.let { facilityId ->
                        val facilityResult = getFacilityByIdUseCase(facilityId)
                        _facility.value = facilityResult
                    }
                } else {
                    _errorMessage.value = "Event not found"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Error loading event details: ${e.localizedMessage}"
                _event.value = null
                _patient.value = null
                _facility.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Potential future actions: Delete event, edit event, etc.
    // fun deleteEvent() { ... }
    // fun markAsCompleted() { ... }
}