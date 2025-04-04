package com.example.sqlitepatient3.domain.repository

import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for accessing patient data.
 */
interface PatientRepository {
    // Read operations
    fun getAllPatients(): Flow<List<Patient>>
    fun getPatientsByFacility(facilityId: Long): Flow<List<Patient>>
    fun getHospicePatients(): Flow<List<Patient>>
    fun getCcmPatients(): Flow<List<Patient>>
    fun getPsychPatients(): Flow<List<Patient>>
    fun searchPatients(query: String): Flow<List<Patient>>

    suspend fun getPatientById(id: Long): Patient?
    suspend fun getPatientByUpi(upi: String): Patient?
    suspend fun getPatientCount(): Int
    suspend fun getPatientCountByFacility(facilityId: Long): Int

    // --- MODIFIED Method Signature ---
    suspend fun insertPatient(
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
    ): Long
    // --- END MODIFIED Method Signature ---

    suspend fun updatePatient(patient: Patient)
    suspend fun deletePatient(patient: Patient)

    // Status updates
    suspend fun updatePatientHospiceStatus(patientId: Long, isHospice: Boolean)
    suspend fun updatePatientCcmStatus(patientId: Long, onCcm: Boolean)
    suspend fun updatePatientPsychStatus(patientId: Long, onPsych: Boolean)
    suspend fun updatePatientPsyMedStatus(patientId: Long, onPsyMed: Boolean, reviewDate: LocalDate? = null)

    // Relations
    fun getPatientWithEvents(patientId: Long): Flow<Pair<Patient, List<com.example.sqlitepatient3.domain.model.Event>>?>

    // New methods for diagnoses
    fun getPatientWithDiagnoses(patientId: Long): Flow<Pair<Patient, List<PatientDiagnosis>>?>
    suspend fun updatePatientHospiceDiagnosis(patientId: Long, diagnosisId: Long?): Boolean
}