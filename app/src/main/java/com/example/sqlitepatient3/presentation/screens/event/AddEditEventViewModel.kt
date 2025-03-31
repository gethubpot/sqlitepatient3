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
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByPatientUseCase // Added
import com.example.sqlitepatient3.domain.usecase.event.UpdateEventUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetAllPatientsUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
// import java.time.temporal.ChronoUnit // Unused import
import javax.inject.Inject

@HiltViewModel
class AddEditEventViewModel @Inject constructor(
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getAllPatientsUseCase: GetAllPatientsUseCase,
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val getEventsByPatientUseCase: GetEventsByPatientUseCase, // Added
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Patient search properties
    private val _patientSearchQuery = MutableStateFlow("")
    val patientSearchQuery: StateFlow<String> = _patientSearchQuery.asStateFlow()

    val filteredPatients = combine(
        getAllPatientsUseCase(),
        _patientSearchQuery
    ) { patients, query ->
        if (query.isBlank()) {
            patients // Show all if query is blank (or handle differently if needed)
        } else {
            patients.filter {
                it.firstName.contains(query, ignoreCase = true) ||
                        it.lastName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setPatientSearchQuery(query: String) {
        _patientSearchQuery.value = query
    }

    // Event/Patient IDs from navigation
    private val eventId: Long? = savedStateHandle.get<Long>("eventId")?.takeIf { it != -1L }
    private val initialPatientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it != -1L }

    // UI state
    private val _isLoading = MutableStateFlow(eventId != null)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // REMOVED or repurpose _saveSuccess
    // private val _saveSuccess = MutableStateFlow(false)
    // val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // ** ADDED state for the confirmation message **
    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    // Hospital discharge date state
    private val _hospDischargeDate = MutableStateFlow<LocalDate?>(null)
    val hospDischargeDate: StateFlow<LocalDate?> = _hospDischargeDate.asStateFlow()

    // Patient related state
    private val _patientId = MutableStateFlow<Long?>(null)
    private val _selectedPatient = MutableStateFlow<Patient?>(null)
    val selectedPatient: StateFlow<Patient?> = _selectedPatient.asStateFlow()

    val patients = getAllPatientsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Form fields
    private val _eventType = MutableStateFlow(EventType.CCM)
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
            loadEvent(eventId)
        } else {
            initialPatientId?.let { setPatientId(it) }
            _isLoading.value = false
            // Ensure TCM check runs if default is CCM and patient ID is provided initially
            if (_eventType.value == EventType.TCM && initialPatientId != null) {
                checkAndSetRecentDischargeDate(initialPatientId)
            }
        }
    }

    private fun loadEvent(id: Long) {
        viewModelScope.launch {
            try {
                val event = getEventByIdUseCase(id)
                if (event != null) {
                    setPatientId(event.patientId)
                    _eventType.value = event.eventType
                    _visitType.value = event.visitType
                    _visitLocation.value = event.visitLocation
                    _eventDateTime.value = event.eventDateTime
                    _eventMinutes.value = event.eventMinutes
                    _noteText.value = event.noteText ?: ""
                    _followUpRecurrence.value = event.followUpRecurrence
                    _hospDischargeDate.value = event.hospDischargeDate // Load existing discharge date if editing
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

    fun setPatientId(id: Long?) {
        _patientId.value = id
        if (id != null) {
            viewModelScope.launch {
                try {
                    _selectedPatient.value = getPatientByIdUseCase(id)
                    _patientSearchQuery.value = "" // Clear search query on selection
                    // If event type is already TCM, check for recent discharge date
                    if (_eventType.value == EventType.TCM) {
                        checkAndSetRecentDischargeDate(id)
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "Error loading patient: ${e.localizedMessage}"
                }
            }
        } else {
            _selectedPatient.value = null
            _hospDischargeDate.value = null // Clear discharge date if patient is cleared
        }
    }

    fun setEventType(type: EventType) {
        val oldType = _eventType.value
        _eventType.value = type

        // --- MODIFIED: TCM Specific Logic ---
        if (type == EventType.TCM && type != oldType) {
            // When switching to TCM, check for recent discharge date for the selected patient
            _patientId.value?.let { currentPatientId ->
                checkAndSetRecentDischargeDate(currentPatientId)
            }
        } else if (type != EventType.TCM) {
            // If switching away from TCM, clear the discharge date
            _hospDischargeDate.value = null
        }
        // --- END MODIFIED ---
    }

    /**
     * Checks for the most recent TCM event with a discharge date within the last 30 days
     * for the given patient and sets the hospDischargeDate state if found.
     */
    private fun checkAndSetRecentDischargeDate(patientId: Long) {
        viewModelScope.launch {
            try {
                val recentEvents = getEventsByPatientUseCase(patientId).firstOrNull() ?: emptyList()
                val thirtyDaysAgo = LocalDate.now().minusDays(30)

                // Find the most recent TCM event with a discharge date within the last 30 days
                val recentTcmDischarge = recentEvents
                    .filter { it.eventType == EventType.TCM && it.hospDischargeDate != null }
                    .mapNotNull { it.hospDischargeDate } // Get non-null discharge dates
                    .filter { !it.isBefore(thirtyDaysAgo) } // Filter for dates within 30 days
                    .maxOrNull() // Find the most recent one

                if (recentTcmDischarge != null) {
                    _hospDischargeDate.value = recentTcmDischarge
                    // Optional: Display a message to the user if desired
                    // _errorMessage.value = "Recent discharge date found and pre-filled."
                } else {
                    // No recent discharge date found, ensure it's cleared unless editing an event that already has one
                    if (eventId == null) { // Only clear if it's a new event
                        _hospDischargeDate.value = null
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error checking for recent discharge date: ${e.localizedMessage}"
                // Ensure date is clear if an error occurs during check for a new event
                if (eventId == null) {
                    _hospDischargeDate.value = null
                }
            }
        }
    }


    fun setHospDischargeDate(date: LocalDate?) {
        _hospDischargeDate.value = date
    }

    fun setVisitType(type: VisitType) {
        _visitType.value = type
    }

    fun setVisitLocation(location: VisitLocation) {
        _visitLocation.value = location
    }

    fun setEventDate(date: LocalDate) {
        // Preserve the existing time, only change the date
        val currentTime = _eventDateTime.value.toLocalTime()
        _eventDateTime.value = LocalDateTime.of(date, currentTime)
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

    // ** ADDED function to clear the save message after it's shown **
    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    // Validation
    private fun isValid(): Boolean {
        if (_patientId.value == null) {
            _errorMessage.value = "Please select a patient"
            return false
        }

        // For TCM events, hospital discharge date is required
        if (_eventType.value == EventType.TCM && _hospDischargeDate.value == null) {
            _errorMessage.value = "Hospital discharge date is required for TCM events"
            return false
        }

        // Add other validation rules here if needed (e.g., eventMinutes > 0)

        return true
    }

    // --- MODIFIED Save Event Logic ---
    fun saveEvent() {
        if (!isValid()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                val currentPatientId = _patientId.value ?: return@launch // Ensure patientId is not null

                val eventToSave = Event(
                    id = eventId ?: 0, // Use 0 for new event, existing id for update
                    patientId = currentPatientId,
                    eventType = _eventType.value,
                    visitType = if (_eventType.value == EventType.FACE_TO_FACE) _visitType.value else VisitType.NON_VISIT, // Only save if F2F
                    visitLocation = if (_eventType.value == EventType.FACE_TO_FACE) _visitLocation.value else VisitLocation.NONE, // Only save if F2F
                    eventMinutes = _eventMinutes.value,
                    noteText = _noteText.value.takeIf { it.isNotBlank() },
                    eventDateTime = _eventDateTime.value,
                    followUpRecurrence = _followUpRecurrence.value,
                    hospDischargeDate = if (_eventType.value == EventType.TCM) _hospDischargeDate.value else null,
                    eventBillDate = LocalDate.now() // Placeholder - actual calculation might be needed or done at export
                    // Consider other fields like cptCode, modifier, status if they need setting/updating here
                )

                if (eventId == null) {
                    // Create new event
                    addEventUseCase(
                        patientId = eventToSave.patientId,
                        eventType = eventToSave.eventType,
                        visitType = eventToSave.visitType,
                        visitLocation = eventToSave.visitLocation,
                        eventMinutes = eventToSave.eventMinutes,
                        noteText = eventToSave.noteText,
                        eventDateTime = eventToSave.eventDateTime,
                        followUpRecurrence = eventToSave.followUpRecurrence
                    ).also { newEventId ->
                        // If it's a TCM event, update with hospital discharge date
                        if (eventToSave.eventType == EventType.TCM && eventToSave.hospDischargeDate != null) {
                            val newEvent = getEventByIdUseCase(newEventId)
                            if (newEvent != null) {
                                updateEventUseCase(
                                    newEvent.copy(
                                        hospDischargeDate = eventToSave.hospDischargeDate
                                    )
                                )
                            }
                        }
                    }

                } else {
                    // Update existing event - fetch the original creation date if needed
                    val existingEvent = getEventByIdUseCase(eventId)
                    if (existingEvent != null) {
                        updateEventUseCase(
                            eventToSave.copy(
                                createdAt = existingEvent.createdAt, // Preserve original creation timestamp
                                // Ensure other fields like cptCode, modifier, status are correctly copied if needed
                                cptCode = existingEvent.cptCode,
                                modifier = existingEvent.modifier,
                                status = existingEvent.status,
                                monthlyBillingId = existingEvent.monthlyBillingId,
                                eventFile = existingEvent.eventFile
                            )
                        )
                    } else {
                        _errorMessage.value = "Event not found for update"
                        _isSaving.value = false
                        _saveMessage.value = null // Clear message on error
                        return@launch
                    }
                }

                // Determine the success message
                val successMessage = if (eventId == null) "Event scheduled successfully" else "Event updated successfully"

                // **CHANGE**: Set the message instead of just saveSuccess
                _saveMessage.value = successMessage
                // _saveSuccess.value = true // Remove or keep if used elsewhere

            } catch (e: Exception) {
                _errorMessage.value = "Error saving event: ${e.localizedMessage}"
                _saveMessage.value = null // Ensure message is null on error
            } finally {
                _isSaving.value = false
            }
        }
    }
    // --- END MODIFIED Save Event Logic ---
}