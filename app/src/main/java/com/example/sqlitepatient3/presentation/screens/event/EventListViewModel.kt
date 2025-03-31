package com.example.sqlitepatient3.presentation.screens.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.EventStatus
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.usecase.event.GetAllEventsUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByStatusUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByTypeUseCase
import com.example.sqlitepatient3.domain.usecase.facility.GetAllFacilitiesUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetAllPatientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// Data class to hold combined Event, Patient Name, and Facility Code data
data class EventListItemData(
    val event: Event,
    val patientFirstName: String,
    val patientLastName: String,
    val facilityCode: String?
)

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getEventsByTypeUseCase: GetEventsByTypeUseCase,
    private val getEventsByStatusUseCase: GetEventsByStatusUseCase,
    private val getAllPatientsUseCase: GetAllPatientsUseCase,
    private val getAllFacilitiesUseCase: GetAllFacilitiesUseCase
) : ViewModel() {

    // --- UI State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Filter State ---
    private val _filterEventType = MutableStateFlow<EventType?>(null)
    val filterEventType: StateFlow<EventType?> = _filterEventType.asStateFlow()

    private val _filterStatus = MutableStateFlow<EventStatus?>(null)
    val filterStatus: StateFlow<EventStatus?> = _filterStatus.asStateFlow()

    // --- Sort Options ---
    enum class SortOption {
        DATE_ASC, DATE_DESC, TYPE, STATUS, PATIENT_NAME_ASC, PATIENT_NAME_DESC, FACILITY_CODE_ASC, FACILITY_CODE_DESC
    }

    private val _currentSortOption = MutableStateFlow(SortOption.DATE_DESC)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption.asStateFlow()

    // --- Main Events Flow (Corrected combine structure using listOf and array destructuring) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<EventListItemData>> = combine(
        // Pass flows as a List
        listOf(
            _searchQuery.debounce(300),
            _filterEventType,
            _filterStatus,
            _currentSortOption,
            getAllPatientsUseCase(),
            getAllFacilitiesUseCase()
        )
    ) { array -> // Lambda now takes a single 'array' argument
        // Extract and cast values from the array based on the order in listOf()
        // It's crucial that the types here match the types emitted by the flows
        @Suppress("UNCHECKED_CAST") // Suppress cast warning, ensure flow types are correct
        val query = array[0] as String
        @Suppress("UNCHECKED_CAST")
        val eventType = array[1] as EventType?
        @Suppress("UNCHECKED_CAST")
        val status = array[2] as EventStatus?
        @Suppress("UNCHECKED_CAST")
        val sortOption = array[3] as SortOption
        @Suppress("UNCHECKED_CAST")
        val patients = array[4] as List<Patient>
        @Suppress("UNCHECKED_CAST")
        val facilities = array[5] as List<Facility>

        _isLoading.value = true
        // Create maps
        val patientMap = patients.associateBy { it.id }
        val facilityMap = facilities.associateBy { it.id }
        // Return the parameters object
        FilterSortParameters(query, eventType, status, sortOption, patientMap, facilityMap)
    }.flatMapLatest { params -> // params is FilterSortParameters
        // Get the base list of events based on filters
        val baseFlow = when {
            params.eventType != null -> getEventsByTypeUseCase(params.eventType)
            params.status != null -> getEventsByStatusUseCase(params.status)
            else -> getAllEventsUseCase()
        }

        baseFlow.map { events ->
            // Apply secondary filter if needed
            val filteredEvents = when {
                params.eventType != null && params.status != null -> {
                    events.filter { it.status == params.status }
                }
                else -> events
            }

            // Map events to EventListItemData and apply search filter
            val mappedAndSearched = filteredEvents.mapNotNull { event ->
                params.patientMap[event.patientId]?.let { patient ->
                    val facilityCode = patient.facilityId?.let { facId ->
                        params.facilityMap[facId]?.facilityCode
                    }
                    EventListItemData(
                        event = event,
                        patientFirstName = patient.firstName,
                        patientLastName = patient.lastName,
                        facilityCode = facilityCode
                    )
                }
            }.filter { item -> // Apply search query AFTER mapping
                if (params.query.isNotBlank()) {
                    item.patientLastName.contains(params.query, ignoreCase = true) ||
                            item.patientFirstName.contains(params.query, ignoreCase = true) ||
                            (item.facilityCode?.contains(params.query, ignoreCase = true) ?: false) ||
                            item.event.eventType.toString().contains(params.query, ignoreCase = true) ||
                            item.event.status.toString().contains(params.query, ignoreCase = true) ||
                            (item.event.noteText?.contains(params.query, ignoreCase = true) ?: false)
                } else {
                    true // No query, include all items
                }
            }

            // Apply sorting
            val sorted = when (params.sortOption) {
                SortOption.DATE_ASC -> mappedAndSearched.sortedBy { it.event.eventDateTime }
                SortOption.DATE_DESC -> mappedAndSearched.sortedByDescending { it.event.eventDateTime }
                SortOption.TYPE -> mappedAndSearched.sortedBy { it.event.eventType.toString() }
                SortOption.STATUS -> mappedAndSearched.sortedBy { it.event.status.toString() }
                SortOption.PATIENT_NAME_ASC -> mappedAndSearched.sortedBy { it.patientLastName + it.patientFirstName }
                SortOption.PATIENT_NAME_DESC -> mappedAndSearched.sortedByDescending { it.patientLastName + it.patientFirstName }
                SortOption.FACILITY_CODE_ASC -> mappedAndSearched.sortedBy { it.facilityCode ?: "" } // Handle null facility codes
                SortOption.FACILITY_CODE_DESC -> mappedAndSearched.sortedByDescending { it.facilityCode ?: "" }
            }

            _isLoading.value = false
            sorted
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Actions ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterEventType(type: EventType?) {
        _filterEventType.value = type
    }

    fun setFilterStatus(status: EventStatus?) {
        _filterStatus.value = status
    }

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
    }

    // --- Helper class for parameters ---
    private data class FilterSortParameters(
        val query: String,
        val eventType: EventType?,
        val status: EventStatus?,
        val sortOption: SortOption,
        val patientMap: Map<Long, Patient>,
        val facilityMap: Map<Long, Facility>
    )
}