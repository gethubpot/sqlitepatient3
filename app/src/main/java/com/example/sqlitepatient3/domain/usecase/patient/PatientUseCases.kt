package com.example.sqlitepatient3.domain.usecase.patient

import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetAllPatientsUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    operator fun invoke(): Flow<List<Patient>> {
        return patientRepository.getAllPatients()
    }
}

class GetPatientByIdUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(id: Long): Patient? {
        return patientRepository.getPatientById(id)
    }
}

class SearchPatientsUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    operator fun invoke(query: String): Flow<List<Patient>> {
        return patientRepository.searchPatients(query)
    }
}

// --- MODIFIED Use Case ---
class AddPatientUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        dateOfBirth: LocalDate?,
        isMale: Boolean,
        facilityId: Long? = null,
        medicareNumber: String = "",
        // Add the new flag parameters:
        isHospice: Boolean,
        onCcm: Boolean,
        onPsych: Boolean,
        onPsyMed: Boolean
    ): Long {
        return patientRepository.insertPatient(
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            isMale = isMale,
            facilityId = facilityId,
            medicareNumber = medicareNumber,
            // Pass the new flags to the repository method:
            isHospice = isHospice,
            onCcm = onCcm,
            onPsych = onPsych,
            onPsyMed = onPsyMed
        )
    }
}
// --- END MODIFIED Use Case ---

class UpdatePatientUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(patient: Patient) {
        patientRepository.updatePatient(patient)
    }
}

class DeletePatientUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend operator fun invoke(patient: Patient) {
        patientRepository.deletePatient(patient)
    }
}

class UpdatePatientStatusesUseCase @Inject constructor(
    private val patientRepository: PatientRepository
) {
    suspend fun updateHospiceStatus(patientId: Long, isHospice: Boolean) {
        patientRepository.updatePatientHospiceStatus(patientId, isHospice)
    }

    suspend fun updateCcmStatus(patientId: Long, onCcm: Boolean) {
        patientRepository.updatePatientCcmStatus(patientId, onCcm)
    }

    suspend fun updatePsychStatus(patientId: Long, onPsych: Boolean) {
        patientRepository.updatePatientPsychStatus(patientId, onPsych)
    }

    suspend fun updatePsyMedStatus(patientId: Long, onPsyMed: Boolean, reviewDate: LocalDate? = null) {
        patientRepository.updatePatientPsyMedStatus(patientId, onPsyMed, reviewDate)
    }
}