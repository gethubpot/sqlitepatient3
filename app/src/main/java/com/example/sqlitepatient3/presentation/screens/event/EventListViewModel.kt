package com.example.sqlitepatient3.presentation.screens.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.EventStatus
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.usecase.event.GetAllEventsUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByStatusUseCase
import com.example.sqlitepatient3.domain.usecase.event.GetEventsByTypeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val getAllEventsUseCase: GetAllEventsUseCase,
    private val getEventsByTypeUseCase: GetEventsByTypeUseCase,
    private val getEventsByStatusUseCase: GetEventsByStatusUseCase
) : ViewModel() {

    // UI state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Filter state
    private val _filterEventType = MutableStateFlow<EventType?>(null)
    val filterEventType: StateFlow<EventType?> = _filterEventType.asStateFlow()

    private val _filterStatus = MutableStateFlow<EventStatus?>(null)
    val filterStatus: StateFlow<EventStatus?> = _filterStatus.asStateFlow()

    // Sort options
    enum class SortOption {
        DATE_ASC, DATE_DESC, TYPE, STATUS
    }

    private val _currentSortOption = MutableStateFlow(SortOption.DATE_DESC)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<Event>> = combine(
        _searchQuery.debounce(300),
        _filterEventType,
        _filterStatus,
        _currentSortOption
    ) { query, eventType, status, sortOption ->
        _isLoading.value = true
        FilterParameters(query, eventType, status, sortOption)
    }.flatMapLatest { params ->
        // First, get the base list of events based on filters
        val baseFlow = when {
            params.eventType != null -> getEventsByTypeUseCase(params.eventType)
            params.status != null -> getEventsByStatusUseCase(params.status)
            else -> getAllEventsUseCase()
        }

        baseFlow.map { events ->
            // Apply search filter if query is not empty
            val searchFiltered = if (params.query.isNotBlank()) {
                // Filter events by search query
                // This implementation is simple - in a real app, you might want to search
                // across various fields, including patient name, notes, etc.
                events.filter { event ->
                    event.eventType.toString().contains(params.query, ignoreCase = true) ||
                            event.status.toString().contains(params.query, ignoreCase = true) ||
                            (event.noteText?.contains(params.query, ignoreCase = true) ?: false)
                }
            } else {
                events
            }

            // Apply secondary filter if needed
            val finalFiltered = when {
                params.eventType != null && params.status != null -> {
                    // If both filters are set, apply status filter to the type-filtered list
                    searchFiltered.filter { it.status == params.status }
                }
                params.eventType != null -> {
                    // Already filtered by event type
                    searchFiltered
                }
                params.status != null -> {
                    // Already filtered by status
                    searchFiltered
                }
                else -> {
                    // No filters applied
                    searchFiltered
                }
            }

            // Apply sorting
            val sorted = when (params.sortOption) {
                SortOption.DATE_ASC -> finalFiltered.sortedBy { it.eventDateTime }
                SortOption.DATE_DESC -> finalFiltered.sortedByDescending { it.eventDateTime }
                SortOption.TYPE -> finalFiltered.sortedBy { it.eventType.toString() }
                SortOption.STATUS -> finalFiltered.sortedBy { it.status.toString() }
            }

            _isLoading.value = false
            sorted
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Actions
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

    // Helper class for filter parameters
    data class FilterParameters(
        val query: String,
        val eventType: EventType?,
        val status: EventStatus?,
        val sortOption: SortOption
    )
}