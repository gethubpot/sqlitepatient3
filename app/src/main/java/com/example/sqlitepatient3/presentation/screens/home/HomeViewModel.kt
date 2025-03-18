package com.example.sqlitepatient3.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.repository.EventRepository
import com.example.sqlitepatient3.domain.repository.FacilityRepository
import com.example.sqlitepatient3.domain.repository.PatientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val eventRepository: EventRepository,
    private val facilityRepository: FacilityRepository
) : ViewModel() {

    // Counters
    private val _patientCount = MutableStateFlow(0)
    val patientCount: StateFlow<Int> = _patientCount

    private val _eventCount = MutableStateFlow(0)
    val eventCount: StateFlow<Int> = _eventCount

    private val _facilityCount = MutableStateFlow(0)
    val facilityCount: StateFlow<Int> = _facilityCount

    init {
        // Fetch count of patients, events, and facilities
        viewModelScope.launch {
            _patientCount.value = patientRepository.getPatientCount()
        }

        viewModelScope.launch {
            eventRepository.getAllEvents()
                .map { it.size }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = 0
                )
                .collect { count ->
                    _eventCount.value = count
                }
        }

        viewModelScope.launch {
            _facilityCount.value = facilityRepository.getFacilityCount()
        }
    }

    // You could add additional functions for quick stats on the home screen
    fun refreshData() {
        viewModelScope.launch {
            _patientCount.value = patientRepository.getPatientCount()
            _facilityCount.value = facilityRepository.getFacilityCount()
        }
    }
}