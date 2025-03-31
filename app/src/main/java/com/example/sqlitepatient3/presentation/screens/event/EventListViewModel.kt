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
import kotlin.random.Random // *** ADDED import for Random ***

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

    // State for the confirmation message
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

    // *** MODIFIED default value from 30 to 7 ***
    private val _eventMinutes = MutableStateFlow(7)
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
                    // Load minutes from saved event, otherwise keep default (7)
                    _eventMinutes.value = event.eventMinutes.takeIf { it > 0 } ?: 7
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

        // TCM Specific Logic
        if (type == EventType.TCM && type != oldType) {
            _patientId.value?.let { currentPatientId ->
                checkAndSetRecentDischargeDate(currentPatientId)
            }
        } else if (type != EventType.TCM) {
            _hospDischargeDate.value = null
        }
    }

    private fun checkAndSetRecentDischargeDate(patientId: Long) {
        viewModelScope.launch {
            try {
                val recentEvents = getEventsByPatientUseCase(patientId).firstOrNull() ?: emptyList()
                val thirtyDaysAgo = LocalDate.now().minusDays(30)

                val recentTcmDischarge = recentEvents
                    .filter { it.eventType == EventType.TCM && it.hospDischargeDate != null }
                    .mapNotNull { it.hospDischargeDate }
                    .filter { !it.isBefore(thirtyDaysAgo) }
                    .maxOrNull()

                if (recentTcmDischarge != null) {
                    _hospDischargeDate.value = recentTcmDischarge
                } else {
                    if (eventId == null) { // Only clear if it's a new event
                        _hospDischargeDate.value = null
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error checking for recent discharge date: ${e.localizedMessage}"
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
        val currentTime = _eventDateTime.value.toLocalTime()
        _eventDateTime.value = LocalDateTime.of(date, currentTime)
    }

    // *** MODIFIED to ensure minimum value when user types ***
    fun setEventMinutes(minutes: Int) {
        // Ensure minutes don't go below a reasonable minimum, e.g., 1
        _eventMinutes.value = maxOf(1, minutes)
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

    fun clearSaveMessage() {
        _saveMessage.value = null
    }

    // *** ADDED function to decrement duration randomly ***
    fun decrementDurationRandomly() {
        val decrementAmount = Random.nextInt(3, 6) // Generates 3, 4, or 5
        val currentMinutes = _eventMinutes.value
        // Ensure minutes don't go below a reasonable minimum, e.g., 1
        _eventMinutes.value = maxOf(1, currentMinutes - decrementAmount)
    }

    // *** ADDED function to increment duration randomly ***
    fun incrementDurationRandomly() {
        val incrementAmount = Random.nextInt(3, 6) // Generates 3, 4, or 5
        val currentMinutes = _eventMinutes.value
        _eventMinutes.value = currentMinutes + incrementAmount
    }


    // Validation
    private fun isValid(): Boolean {
        if (_patientId.value == null) {
            _errorMessage.value = "Please select a patient"
            return false
        }

        if (_eventType.value == EventType.TCM && _hospDischargeDate.value == null) {
            _errorMessage.value = "Hospital discharge date is required for TCM events"
            return false
        }

        // Ensure event minutes are positive
        if (_eventMinutes.value <= 0) {
            _errorMessage.value = "Event duration must be positive"
            return false
        }

        return true
    }

    // Save Event Logic
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
                    visitType = if (_eventType.value == EventType.FACE_TO_FACE) _visitType.value else VisitType.NON_VISIT,
                    visitLocation = if (_eventType.value == EventType.FACE_TO_FACE) _visitLocation.value else VisitLocation.NONE,
                    eventMinutes = _eventMinutes.value,
                    noteText = _noteText.value.takeIf { it.isNotBlank() },
                    eventDateTime = _eventDateTime.value,
                    followUpRecurrence = _followUpRecurrence.value,
                    hospDischargeDate = if (_eventType.value == EventType.TCM) _hospDischargeDate.value else null,
                    eventBillDate = LocalDate.now() // Placeholder
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
                    // Update existing event
                    val existingEvent = getEventByIdUseCase(eventId)
                    if (existingEvent != null) {
                        updateEventUseCase(
                            eventToSave.copy(
                                createdAt = existingEvent.createdAt, // Preserve original creation timestamp
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

                val successMessage = if (eventId == null) "Event scheduled successfully" else "Event updated successfully"
                _saveMessage.value = successMessage

            } catch (e: Exception) {
                _errorMessage.value = "Error saving event: ${e.localizedMessage}"
                _saveMessage.value = null // Ensure message is null on error
            } finally {
                _isSaving.value = false
            }
        }
    }
}