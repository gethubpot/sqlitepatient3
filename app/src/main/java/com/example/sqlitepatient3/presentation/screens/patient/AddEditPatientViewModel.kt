package com.example.sqlitepatient3.presentation.screens.patient

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.usecase.facility.GetActiveFacilitiesUseCase
import com.example.sqlitepatient3.domain.usecase.patient.AddPatientUseCase
import com.example.sqlitepatient3.domain.usecase.patient.GetPatientByIdUseCase
import com.example.sqlitepatient3.domain.usecase.patient.UpdatePatientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddEditPatientViewModel @Inject constructor(
    private val getPatientByIdUseCase: GetPatientByIdUseCase,
    private val addPatientUseCase: AddPatientUseCase,
    private val updatePatientUseCase: UpdatePatientUseCase,
    private val getActiveFacilitiesUseCase: GetActiveFacilitiesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // The patient ID from navigation arguments, or null if creating a new patient
    private val patientId: Long? = savedStateHandle.get<Long>("patientId")?.takeIf { it != -1L }

    // UI state
    private val _isLoading = MutableStateFlow(patientId != null)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // Form fields
    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _dateOfBirth = MutableStateFlow<LocalDate?>(null)
    val dateOfBirth: StateFlow<LocalDate?> = _dateOfBirth.asStateFlow()

    private val _isMale = MutableStateFlow(true)
    val isMale: StateFlow<Boolean> = _isMale.asStateFlow()

    private val _medicareNumber = MutableStateFlow("")
    val medicareNumber: StateFlow<String> = _medicareNumber.asStateFlow()

    private val _facilityId = MutableStateFlow<Long?>(null)
    val facilityId: StateFlow<Long?> = _facilityId.asStateFlow()

    private val _isHospice = MutableStateFlow(false)
    val isHospice: StateFlow<Boolean> = _isHospice.asStateFlow()

    private val _onCcm = MutableStateFlow(false)
    val onCcm: StateFlow<Boolean> = _onCcm.asStateFlow()

    private val _onPsych = MutableStateFlow(false)
    val onPsych: StateFlow<Boolean> = _onPsych.asStateFlow()

    private val _onPsyMed = MutableStateFlow(false)
    val onPsyMed: StateFlow<Boolean> = _onPsyMed.asStateFlow()

    // Available facilities for dropdown
    val facilities = getActiveFacilitiesUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        if (patientId != null) {
            // Load existing patient data
            loadPatient(patientId)
        } else {
            _isLoading.value = false
        }
    }

    private fun loadPatient(id: Long) {
        viewModelScope.launch {
            try {
                val patient = getPatientByIdUseCase(id)
                if (patient != null) {
                    // Update form fields with patient data
                    _firstName.value = patient.firstName
                    _lastName.value = patient.lastName
                    _dateOfBirth.value = patient.dateOfBirth
                    _isMale.value = patient.isMale
                    _medicareNumber.value = patient.medicareNumber
                    _facilityId.value = patient.facilityId
                    _isHospice.value = patient.isHospice
                    _onCcm.value = patient.onCcm
                    _onPsych.value = patient.onPsych
                    _onPsyMed.value = patient.onPsyMed
                } else {
                    _errorMessage.value = "Patient not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading patient: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Form field update functions
    fun setFirstName(value: String) {
        _firstName.value = value
    }

    fun setLastName(value: String) {
        _lastName.value = value
    }

    fun setDateOfBirth(value: LocalDate?) {
        _dateOfBirth.value = value
    }

    fun setIsMale(value: Boolean) {
        _isMale.value = value
    }

    fun setMedicareNumber(value: String) {
        _medicareNumber.value = value
    }

    fun setFacilityId(value: Long?) {
        _facilityId.value = value
    }

    fun setIsHospice(value: Boolean) {
        _isHospice.value = value
    }

    fun setOnCcm(value: Boolean) {
        _onCcm.value = value
    }

    fun setOnPsych(value: Boolean) {
        _onPsych.value = value
    }

    fun setOnPsyMed(value: Boolean) {
        _onPsyMed.value = value
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Validation
    private fun isValid(): Boolean {
        if (_firstName.value.isBlank()) {
            _errorMessage.value = "First name is required"
            return false
        }
        if (_lastName.value.isBlank()) {
            _errorMessage.value = "Last name is required"
            return false
        }
        return true
    }

    // Save patient
    fun savePatient() {
        if (!isValid()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                if (patientId == null) {
                    // Create new patient
                    val id = addPatientUseCase(
                        firstName = _firstName.value,
                        lastName = _lastName.value,
                        dateOfBirth = _dateOfBirth.value,
                        isMale = _isMale.value,
                        facilityId = _facilityId.value,
                        medicareNumber = _medicareNumber.value
                    )

                    // After successful insert, we need to get the newly created patient
                    // to update its status flags if they're different from defaults
                    val newPatient = getPatientByIdUseCase(id)
                    if (newPatient != null) {
                        // Check if we need to update any status flags
                        if (_isHospice.value || _onCcm.value || _onPsych.value || _onPsyMed.value) {
                            updatePatientUseCase(
                                newPatient.copy(
                                    isHospice = _isHospice.value,
                                    onCcm = _onCcm.value,
                                    onPsych = _onPsych.value,
                                    onPsyMed = _onPsyMed.value
                                )
                            )
                        }
                    }
                } else {
                    // Update existing patient
                    val patient = getPatientByIdUseCase(patientId)
                    if (patient != null) {
                        updatePatientUseCase(
                            patient.copy(
                                firstName = _firstName.value,
                                lastName = _lastName.value,
                                dateOfBirth = _dateOfBirth.value,
                                isMale = _isMale.value,
                                medicareNumber = _medicareNumber.value,
                                facilityId = _facilityId.value,
                                isHospice = _isHospice.value,
                                onCcm = _onCcm.value,
                                onPsych = _onPsych.value,
                                onPsyMed = _onPsyMed.value
                            )
                        )
                    } else {
                        _errorMessage.value = "Patient not found"
                        _isSaving.value = false
                        return@launch
                    }
                }

                // Success
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Error saving patient: ${e.localizedMessage}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}