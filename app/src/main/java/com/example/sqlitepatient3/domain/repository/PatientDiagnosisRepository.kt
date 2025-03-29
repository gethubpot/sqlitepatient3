package com.example.sqlitepatient3.domain.repository

import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for accessing patient diagnosis data.
 */
interface PatientDiagnosisRepository {
    // Read operations
    fun getPatientDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>>
    fun getActivePatientDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>>
    fun getHospiceDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>>
    fun getDiagnosesByIcdCode(icdCode: String): Flow<List<PatientDiagnosis>>

    suspend fun getPatientDiagnosisById(diagnosisId: Long): PatientDiagnosis?
    suspend fun getPatientDiagnosisByPriority(patientId: Long, priority: Int): PatientDiagnosis?
    suspend fun getPatientDiagnosisCount(patientId: Long): Int

    // Write operations
    suspend fun insertPatientDiagnosis(
        patientId: Long,
        icdCode: String,
        priority: Int,
        isHospiceCode: Boolean = false,
        diagnosisDate: LocalDate? = LocalDate.now(),
        notes: String? = null
    ): Long

    suspend fun updatePatientDiagnosis(diagnosis: PatientDiagnosis)
    suspend fun deletePatientDiagnosis(diagnosis: PatientDiagnosis)

    // Status operations
    suspend fun setDiagnosisActive(diagnosisId: Long, active: Boolean): Boolean
    suspend fun setHospiceStatus(diagnosisId: Long, isHospiceCode: Boolean): Boolean
    suspend fun resolveDiagnosis(diagnosisId: Long, resolvedDate: LocalDate? = LocalDate.now()): Boolean

    // Bulk operations
    suspend fun deleteAllPatientDiagnoses(patientId: Long)
}