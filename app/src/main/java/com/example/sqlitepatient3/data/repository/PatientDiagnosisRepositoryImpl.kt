package com.example.sqlitepatient3.data.repository

import com.example.sqlitepatient3.data.local.dao.PatientDiagnosisDao
import com.example.sqlitepatient3.data.local.entity.PatientDiagnosisEntity
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.repository.PatientDiagnosisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientDiagnosisRepositoryImpl @Inject constructor(
    private val patientDiagnosisDao: PatientDiagnosisDao
) : PatientDiagnosisRepository {

    override fun getPatientDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisDao.getPatientDiagnosesByPatient(patientId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getActivePatientDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisDao.getActivePatientDiagnosesByPatient(patientId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getHospiceDiagnoses(patientId: Long): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisDao.getHospiceDiagnosesByPatient(patientId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDiagnosesByIcdCode(icdCode: String): Flow<List<PatientDiagnosis>> {
        return patientDiagnosisDao.getPatientDiagnosesByIcdCode(icdCode).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getPatientDiagnosisById(diagnosisId: Long): PatientDiagnosis? {
        return patientDiagnosisDao.getPatientDiagnosisById(diagnosisId)?.toDomainModel()
    }

    override suspend fun getPatientDiagnosisByPriority(patientId: Long, priority: Int): PatientDiagnosis? {
        return patientDiagnosisDao.getPatientDiagnosisByPriority(patientId, priority)?.toDomainModel()
    }

    override suspend fun getPatientDiagnosisCount(patientId: Long): Int {
        return patientDiagnosisDao.getPatientDiagnosisCount(patientId)
    }

    override suspend fun insertPatientDiagnosis(
        patientId: Long,
        icdCode: String,
        priority: Int,
        isHospiceCode: Boolean,
        diagnosisDate: LocalDate?,
        notes: String?
    ): Long {
        val diagnosis = PatientDiagnosisEntity(
            patientId = patientId,
            icdCode = icdCode,
            priority = priority,
            isHospiceCode = isHospiceCode,
            diagnosisDate = diagnosisDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            resolvedDate = null,
            notes = notes,
            active = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return patientDiagnosisDao.insertPatientDiagnosis(diagnosis)
    }

    override suspend fun updatePatientDiagnosis(diagnosis: PatientDiagnosis) {
        val entity = PatientDiagnosisEntity.fromDomainModel(diagnosis.copy(
            // Update timestamp
            updatedAt = System.currentTimeMillis()
        ))
        patientDiagnosisDao.updatePatientDiagnosis(entity)
    }

    override suspend fun deletePatientDiagnosis(diagnosis: PatientDiagnosis) {
        val entity = PatientDiagnosisEntity.fromDomainModel(diagnosis)
        patientDiagnosisDao.deletePatientDiagnosis(entity)
    }

    override suspend fun setDiagnosisActive(diagnosisId: Long, active: Boolean): Boolean {
        return try {
            patientDiagnosisDao.updateActivationStatus(diagnosisId, active)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun setHospiceStatus(diagnosisId: Long, isHospiceCode: Boolean): Boolean {
        return try {
            patientDiagnosisDao.updateHospiceStatus(diagnosisId, isHospiceCode)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun resolveDiagnosis(diagnosisId: Long, resolvedDate: LocalDate?): Boolean {
        return try {
            val resolvedDateMillis = resolvedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            patientDiagnosisDao.resolveDiagnosis(diagnosisId, resolvedDateMillis)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun deleteAllPatientDiagnoses(patientId: Long) {
        patientDiagnosisDao.deleteAllPatientDiagnoses(patientId)
    }
}