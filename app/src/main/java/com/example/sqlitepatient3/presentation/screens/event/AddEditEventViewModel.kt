package com.example.sqlitepatient3.presentation.screens.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.FollowUpRecurrence
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.VisitLocation
import com.example.sqlitepatient3.domain.model.VisitType
import com.example.sqlitepatient3.domain.usecase.event.AddEventUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventByIdUseCase
import com.example.sqlitepatient3.domain.usecase.event.UpdateEventUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetAllPatientsUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getAllPatientsUseCase: GetAllPatientsUseCase,
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The event ID from navigation arguments, or null if creating a new event
    private val eventId: Long? = savedStateHandle.get<Long>("eventId")?.takeIf { it != -1L }
    private val initialPatientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it != -1L }

    // UI state
    private val _isLoading = MutableStateFlow(eventId != null)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // Patient related state
    private val _patientId = MutableStateFlow<Long?>(null)
    private val _selectedPatient = MutableStateFlow<Patient?>(null)
    val selectedPatient: StateFlow<Patient?> = _selectedPatient.asStateFlow()

    // Get all patients for dropdown
    val patients = getAllPatientsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Form fields
    private val _eventType = MutableStateFlow(EventType.FACE_TO_FACE)
    val eventType: StateFlow<EventType> = _eventType.asStateFlow()

    private val _visitType = MutableStateFlow(VisitType.HOME_VISIT)
    val visitType: StateFlow<VisitType> = _visitType.asStateFlow()

    private val _visitLocation = MutableStateFlow(VisitLocation.PATIENT_HOME)
    val visitLocation: StateFlow<VisitLocation> = _visitLocation.asStateFlow()

    private val _eventDateTime = MutableStateFlow(LocalDateTime.now())
    val eventDateTime: StateFlow<LocalDateTime> = _eventDateTime.asStateFlow()

    private val _eventMinutes = MutableStateFlow(30)
    val eventMinutes: StateFlow<Int> = _eventMinutes.asStateFlow()

    private val _noteText = MutableStateFlow("")
    val noteText: StateFlow<String> = _noteText.asStateFlow()

    private val _followUpRecurrence = MutableStateFlow(FollowUpRecurrence.NONE)
    val followUpRecurrence: StateFlow<FollowUpRecurrence> = _followUpRecurrence.asStateFlow()

    init {
        if (eventId != null) {
            // Load existing event data
            loadEvent(eventId)
        } else {
            // For new events, set the patient if provided
            initialPatientId?.let { setPatientId(it) }
            _isLoading.value = false
        }
    }

    private fun loadEvent(id: Long) {
        viewModelScope.launch {
            try {
                val event = getEventByIdUseCase(id)
                if (event != null) {
                    // Load the associated patient
                    setPatientId(event.patientId)

                    // Update form fields with event data
                    _eventType.value = event.eventType
                    _visitType.value = event.visitType
                    _visitLocation.value = event.visitLocation
                    _eventDateTime.value = event.eventDateTime
                    _eventMinutes.value = event.eventMinutes
                    _noteText.value = event.noteText ?: ""
                    _followUpRecurrence.value = event.followUpRecurrence
                } else {
                    _errorMessage.value = "Event not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading event: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Form field update functions
    fun setPatientId(id: Long) {
        _patientId.value = id
        viewModelScope.launch {
            try {
                _selectedPatient.value = getPatientByIdUseCase(id)
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patient: ${e.localizedMessage}"
            }
        }
    }

    fun setEventType(type: EventType) {
        _eventType.value = type
    }

    fun setVisitType(type: VisitType) {
        _visitType.value = type
    }

    fun setVisitLocation(location: VisitLocation) {
        _visitLocation.value = location
    }

    fun setEventDate(date: LocalDate) {
        val currentTime = _eventDateTime.value.toLocalTime()
        _eventDateTime.value = LocalDateTime.of(date, currentTime)
    }

    fun setEventTime(time: LocalTime) {
        val currentDate = _eventDateTime.value.toLocalDate()
        _eventDateTime.value = LocalDateTime.of(currentDate, time)
    }

    fun setEventMinutes(minutes: Int) {
        _eventMinutes.value = minutes
    }

    fun setNoteText(text: String) {
        _noteText.value = text
    }

    fun setFollowUpRecurrence(recurrence: FollowUpRecurrence) {
        _followUpRecurrence.value = recurrence
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Validation
    private fun isValid(): Boolean {
        if (_patientId.value == null) {
            _errorMessage.value = "Please select a patient"
            return false
        }
        return true
    }

    // Save event
    fun saveEvent() {
        if (!isValid()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val patientId = _patientId.value ?: return@launch

                if (eventId == null) {
                    // Create new event
                    addEventUseCase(
                        patientId = patientId,
                        eventType = _eventType.value,
                        visitType = _visitType.value,
                        visitLocation = _visitLocation.value,
                        eventMinutes = _eventMinutes.value,
                        noteText = _noteText.value.takeIf { it.isNotBlank() },
                        eventDateTime = _eventDateTime.value,
                        followUpRecurrence = _followUpRecurrence.value
                    )
                } else {
                    // Update existing event
                    val event = getEventByIdUseCase(eventId)
                    if (event != null) {
                        updateEventUseCase(
                            event.copy(
                                patientId = patientId,
                                eventType = _eventType.value,
                                visitType = _visitType.value,
                                visitLocation = _visitLocation.value,
                                eventMinutes = _eventMinutes.value,
                                noteText = _noteText.value.takeIf { it.isNotBlank() },
                                eventDateTime = _eventDateTime.value,
                                followUpRecurrence = _followUpRecurrence.value
                            )
                        )
                    } else {
                        _errorMessage.value = "Event not found"
                        _isSaving.value = false
                        return@launch
                    }
                }

                // Success
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Error saving event: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}