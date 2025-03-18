package com.example.sqlitepatient3.data.local.dao

import androidx.room.*
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.data.local.relation.PatientWithEvents
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the patients table.
 */
@Dao
interface PatientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity): Long

    @Update
    suspend fun updatePatient(patient: PatientEntity)

    @Delete
    suspend fun deletePatient(patient: PatientEntity)

    @Query("SELECT * FROM patients")
    fun getAllPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientById(patientId: Long): PatientEntity?

    @Query("SELECT * FROM patients WHERE upi = :upi")
    suspend fun getPatientByUpi(upi: String): PatientEntity?

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun getPatientCount(): Int

    @Query("SELECT * FROM patients WHERE facilityId = :facilityId")
    fun getPatientsByFacility(facilityId: Long): Flow<List<PatientEntity>>

    @Query("SELECT COUNT(*) FROM patients WHERE facilityId = :facilityId")
    suspend fun getPatientCountByFacility(facilityId: Long): Int

    @Query("SELECT * FROM patients WHERE isHospice = 1")
    fun getHospicePatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE onCcm = 1")
    fun getCcmPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE onPsych = 1")
    fun getPsychPatients(): Flow<List<PatientEntity>>

    @Query("SELECT * FROM patients WHERE lastName LIKE :query OR firstName LIKE :query OR upi LIKE :query")
    fun searchPatients(query: String): Flow<List<PatientEntity>>

    @Transaction
    @Query("UPDATE patients SET isHospice = :isHospice, updatedAt = :timestamp WHERE id = :patientId")
    suspend fun updateHospiceStatus(
        patientId: Long,
        isHospice: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("UPDATE patients SET onCcm = :onCcm, updatedAt = :timestamp WHERE id = :patientId")
    suspend fun updateCcmStatus(
        patientId: Long,
        onCcm: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("UPDATE patients SET onPsych = :onPsych, updatedAt = :timestamp WHERE id = :patientId")
    suspend fun updatePsychStatus(
        patientId: Long,
        onPsych: Boolean,
        timestamp: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("UPDATE patients SET onPsyMed = :onPsyMed, psyMedReviewDate = :reviewDate, updatedAt = :timestamp WHERE id = :patientId")
    suspend fun updatePsyMedStatus(
        patientId: Long,
        onPsyMed: Boolean,
        reviewDate: Long?,
        timestamp: Long = System.currentTimeMillis()
    )

    // Add the missing method
    @Transaction
    @Query("SELECT * FROM patients WHERE id = :patientId")
    fun getPatientWithEvents(patientId: Long): Flow<PatientWithEvents?>
}
