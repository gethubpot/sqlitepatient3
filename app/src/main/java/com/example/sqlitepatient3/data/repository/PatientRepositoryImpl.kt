package com.example.sqlitepatient3.data.repository

import com.example.sqlitepatient3.data.local.dao.PatientDao
import com.example.sqlitepatient3.data.local.dao.PatientDiagnosisDao
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import com.example.sqlitepatient3.domain.repository.PatientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepositoryImpl @Inject constructor(
    private val patientDao: PatientDao,
    private val patientDiagnosisDao: PatientDiagnosisDao // Keep this if needed elsewhere, otherwise can be removed if not used
) : PatientRepository {

    override fun getAllPatients(): Flow<List<Patient>> {
        return patientDao.getAllPatients().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getPatientsByFacility(facilityId: Long): Flow<List<Patient>> {
        return patientDao.getPatientsByFacility(facilityId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getHospicePatients(): Flow<List<Patient>> {
        return patientDao.getHospicePatients().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getCcmPatients(): Flow<List<Patient>> {
        return patientDao.getCcmPatients().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getPsychPatients(): Flow<List<Patient>> {
        return patientDao.getPsychPatients().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchPatients(query: String): Flow<List<Patient>> {
        val wildcardQuery = "%$query%"
        return patientDao.searchPatients(wildcardQuery).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getPatientById(id: Long): Patient? {
        return patientDao.getPatientById(id)?.toDomainModel()
    }

    override suspend fun getPatientByUpi(upi: String): Patient? {
        return patientDao.getPatientByUpi(upi)?.toDomainModel()
    }

    override suspend fun getPatientCount(): Int {
        return patientDao.getPatientCount()
    }

    override suspend fun getPatientCountByFacility(facilityId: Long): Int {
        return patientDao.getPatientCountByFacility(facilityId)
    }

    // --- MODIFIED Method Implementation ---
    override suspend fun insertPatient(
        firstName: String,
        lastName: String,
        dateOfBirth: LocalDate?,
        isMale: Boolean,
        facilityId: Long?,
        medicareNumber: String,
        // Add the new flag parameters:
        isHospice: Boolean,
        onCcm: Boolean,
        onPsych: Boolean,
        onPsyMed: Boolean
    ): Long {
        // Generate UPI
        val upi = Patient.generateUpi(lastName, firstName, dateOfBirth)

        val patient = PatientEntity(
            firstName = firstName,
            lastName = lastName,
            upi = upi,
            dateOfBirth = dateOfBirth?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            isMale = isMale,
            facilityId = facilityId,
            medicareNumber = medicareNumber,
            // Use the passed-in values for flags:
            isHospice = isHospice,
            onCcm = onCcm,
            onPsych = onPsych,
            onPsyMed = onPsyMed,
            // Keep defaults for other fields not passed in
            psyMedReviewDate = null, // Assuming this isn't set during initial insert
            hospiceDiagnosisId = null, // Assuming this isn't set during initial insert
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return patientDao.insertPatient(patient)
    }
    // --- END MODIFIED Method Implementation ---

    override suspend fun updatePatient(patient: Patient) {
        val entity = PatientEntity.fromDomainModel(patient.copy(updatedAt = System.currentTimeMillis()))
        patientDao.updatePatient(entity)
    }

    override suspend fun deletePatient(patient: Patient) {
        val entity = PatientEntity.fromDomainModel(patient)
        patientDao.deletePatient(entity)
    }

    override suspend fun updatePatientHospiceStatus(patientId: Long, isHospice: Boolean) {
        patientDao.updateHospiceStatus(patientId, isHospice)
    }

    override suspend fun updatePatientCcmStatus(patientId: Long, onCcm: Boolean) {
        patientDao.updateCcmStatus(patientId, onCcm)
    }

    override suspend fun updatePatientPsychStatus(patientId: Long, onPsych: Boolean) {
        patientDao.updatePsychStatus(patientId, onPsych)
    }

    override suspend fun updatePatientPsyMedStatus(patientId: Long, onPsyMed: Boolean, reviewDate: LocalDate?) {
        val reviewDateMillis = reviewDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        patientDao.updatePsyMedStatus(patientId, onPsyMed, reviewDateMillis)
    }

    override fun getPatientWithEvents(patientId: Long): Flow<Pair<Patient, List<Event>>?> {
        return patientDao.getPatientWithEvents(patientId).mapNotNull { it?.toDomainModel() }
    }

    // New methods for diagnoses
    override fun getPatientWithDiagnoses(patientId: Long): Flow<Pair<Patient, List<PatientDiagnosis>>?> {
        return patientDao.getPatientWithDiagnoses(patientId).mapNotNull { it?.toDomainModel() }
    }

    override suspend fun updatePatientHospiceDiagnosis(patientId: Long, diagnosisId: Long?): Boolean {
        return try {
            patientDao.updateHospiceDiagnosis(patientId, diagnosisId)
            true
        } catch (e: Exception) {
            false
        }
    }
}