package com.example.sqlitepatient3.presentation.screens.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.usecase.patient.GetAllPatientsUseCase
import com.example.sqlitepatient3.domain.usecase.patient.SearchPatientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val getAllPatientsUseCase: GetAllPatientsUseCase,
    private val searchPatientsUseCase: SearchPatientsUseCase
) : ViewModel() {

    // UI state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Sort options
    enum class SortOption {
        NAME_ASC, NAME_DESC, DATE_ADDED_ASC, DATE_ADDED_DESC
    }

    private val _currentSortOption = MutableStateFlow(SortOption.NAME_ASC)
    val currentSortOption: StateFlow<SortOption> = _currentSortOption

    // Filter options
    private val _showHospiceOnly = MutableStateFlow(false)
    val showHospiceOnly: StateFlow<Boolean> = _showHospiceOnly

    private val _showCcmOnly = MutableStateFlow(false)
    val showCcmOnly: StateFlow<Boolean> = _showCcmOnly

    private val _showPsychOnly = MutableStateFlow(false)
    val showPsychOnly: StateFlow<Boolean> = _showPsychOnly

    @OptIn(ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<Patient>> = combine(
        _searchQuery.debounce(300),
        _currentSortOption,
        _showHospiceOnly,
        _showCcmOnly,
        _showPsychOnly
    ) { query, sortOption, hospiceOnly, ccmOnly, psychOnly ->
        _isLoading.value = true
        Quadruple(query, sortOption, hospiceOnly, ccmOnly, psychOnly)
    }.flatMapLatest { (query, sortOption, hospiceOnly, ccmOnly, psychOnly) ->
        val patientFlow = if (query.isBlank()) {
            getAllPatientsUseCase()
        } else {
            searchPatientsUseCase(query)
        }

        patientFlow.map { patients ->
            // Apply filters
            var filteredPatients = patients
            if (hospiceOnly) {
                filteredPatients = filteredPatients.filter { it.isHospice }
            }
            if (ccmOnly) {
                filteredPatients = filteredPatients.filter { it.onCcm }
            }
            if (psychOnly) {
                filteredPatients = filteredPatients.filter { it.onPsych }
            }

            // Apply sorting
            val sortedPatients = when (sortOption) {
                SortOption.NAME_ASC -> filteredPatients.sortedWith(compareBy { it.lastName + it.firstName })
                SortOption.NAME_DESC -> filteredPatients.sortedWith(compareByDescending { it.lastName + it.firstName })
                SortOption.DATE_ADDED_ASC -> filteredPatients.sortedBy { it.createdAt }
                SortOption.DATE_ADDED_DESC -> filteredPatients.sortedByDescending { it.createdAt }
            }

            _isLoading.value = false
            sortedPatients
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

    fun setSortOption(option: SortOption) {
        _currentSortOption.value = option
    }

    fun toggleHospiceFilter(showHospiceOnly: Boolean) {
        _showHospiceOnly.value = showHospiceOnly
    }

    fun toggleCcmFilter(showCcmOnly: Boolean) {
        _showCcmOnly.value = showCcmOnly
    }

    fun togglePsychFilter(showPsychOnly: Boolean) {
        _showPsychOnly.value = showPsychOnly
    }

    // Helper class for multiple values in flatMapLatest
    private data class Quadruple<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}