package com.example.sqlitepatient3.domain.usecase.diagnosis

import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetPatientDiagnosesUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    operator fun invoke(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisRepository.getPatientDiagnoses(patientId)
    }
}

class GetActivePatientDiagnosesUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    operator fun invoke(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisRepository.getActivePatientDiagnoses(patientId)
    }
}

class GetHospiceDiagnosesUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    operator fun invoke(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisRepository.getHospiceDiagnoses(patientId)
    }
}

class GetDiagnosesByIcdCodeUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    operator fun invoke(icdCode: String): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisRepository.getDiagnosesByIcdCode(icdCode)
    }
}

class GetPatientDiagnosisByIdUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosisId: Long): PatientDiagnosis? {
        return patientDiagnosisRepository.getPatientDiagnosisById(diagnosisId)
    }
}

class GetPatientDiagnosisByPriorityUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(patientId: Long, priority: Int): PatientDiagnosis? {
        return patientDiagnosisRepository.getPatientDiagnosisByPriority(patientId, priority)
    }
}

class AddPatientDiagnosisUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(
        patientId: Long,
        icdCode: String,
        priority: Int,
        isHospiceCode: Boolean = false,
        diagnosisDate: LocalDate? = LocalDate.now(),
        notes: String? = null
    ): Long {
        return patientDiagnosisRepository.insertPatientDiagnosis(
            patientId = patientId,
            icdCode = icdCode,
            priority = priority,
            isHospiceCode = isHospiceCode,
            diagnosisDate = diagnosisDate,
            notes = notes
        )
    }
}

class UpdatePatientDiagnosisUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosis: PatientDiagnosis) {
        patientDiagnosisRepository.updatePatientDiagnosis(diagnosis)
    }
}

class DeletePatientDiagnosisUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosis: PatientDiagnosis) {
        patientDiagnosisRepository.deletePatientDiagnosis(diagnosis)
    }
}

class SetDiagnosisActiveUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosisId: Long, active: Boolean): Boolean {
        return patientDiagnosisRepository.setDiagnosisActive(diagnosisId, active)
    }
}

class SetHospiceStatusUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosisId: Long, isHospiceCode: Boolean): Boolean {
        return patientDiagnosisRepository.setHospiceStatus(diagnosisId, isHospiceCode)
    }
}

class ResolveDiagnosisUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(diagnosisId: Long, resolvedDate: LocalDate? = LocalDate.now()): Boolean {
        return patientDiagnosisRepository.resolveDiagnosis(diagnosisId, resolvedDate)
    }
}

class DeleteAllPatientDiagnosesUseCase @Inject constructor(
    private val patientDiagnosisRepository: PatientDiagnosisRepository
) {
    suspend operator fun invoke(patientId: Long) {
        patientDiagnosisRepository.deleteAllPatientDiagnoses(patientId)
    }
}