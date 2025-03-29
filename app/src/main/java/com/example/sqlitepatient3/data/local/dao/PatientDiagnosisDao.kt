package com.example.sqlitepatient3.data.local.dao

import androidx.room.*
import com.example.sqlitepatient3.data.local.entity.PatientDiagnosisEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the patient_diagnoses table.
 */
@Dao
interface PatientDiagnosisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatientDiagnosis(diagnosis: PatientDiagnosisEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatientDiagnoses(diagnoses: List<PatientDiagnosisEntity>): List<Long>

    @Update
    suspend fun updatePatientDiagnosis(diagnosis: PatientDiagnosisEntity)

    @Delete
    suspend fun deletePatientDiagnosis(diagnosis: PatientDiagnosisEntity)

    @Query("SELECT * FROM patient_diagnoses WHERE id = :diagnosisId")
    suspend fun getPatientDiagnosisById(diagnosisId: Long): PatientDiagnosisEntity?

    @Query("SELECT * FROM patient_diagnoses WHERE patientId = :patientId ORDER BY priority")
    fun getPatientDiagnosesByPatient(patientId: Long): Flow<List<PatientDiagnosisEntity>>

    @Query("SELECT * FROM patient_diagnoses WHERE patientId = :patientId AND active = 1 ORDER BY priority")
    fun getActivePatientDiagnosesByPatient(patientId: Long): Flow<List<PatientDiagnosisEntity>>

    @Query("SELECT * FROM patient_diagnoses WHERE patientId = :patientId AND isHospiceCode = 1 AND active = 1 ORDER BY priority")
    fun getHospiceDiagnosesByPatient(patientId: Long): Flow<List<PatientDiagnosisEntity>>

    @Query("SELECT * FROM patient_diagnoses WHERE icdCode = :icdCode")
    fun getPatientDiagnosesByIcdCode(icdCode: String): Flow<List<PatientDiagnosisEntity>>

    @Query("SELECT * FROM patient_diagnoses WHERE patientId = :patientId AND priority = :priority")
    suspend fun getPatientDiagnosisByPriority(patientId: Long, priority: Int): PatientDiagnosisEntity?

    @Query("SELECT COUNT(*) FROM patient_diagnoses WHERE patientId = :patientId")
    suspend fun getPatientDiagnosisCount(patientId: Long): Int

    @Query("DELETE FROM patient_diagnoses WHERE patientId = :patientId")
    suspend fun deleteAllPatientDiagnoses(patientId: Long)

    @Transaction
    @Query("UPDATE patient_diagnoses SET active = :active, updatedAt = :timestamp WHERE id = :diagnosisId")
    suspend fun updateActivationStatus(diagnosisId: Long, active: Boolean, timestamp: Long = System.currentTimeMillis())

    @Transaction
    @Query("UPDATE patient_diagnoses SET isHospiceCode = :isHospiceCode, updatedAt = :timestamp WHERE id = :diagnosisId")
    suspend fun updateHospiceStatus(diagnosisId: Long, isHospiceCode: Boolean, timestamp: Long = System.currentTimeMillis())

    @Transaction
    @Query("UPDATE patient_diagnoses SET resolvedDate = :resolvedDateMillis, active = 0, updatedAt = :timestamp WHERE id = :diagnosisId")
    suspend fun resolveDiagnosis(diagnosisId: Long, resolvedDateMillis: Long?, timestamp: Long = System.currentTimeMillis())
}