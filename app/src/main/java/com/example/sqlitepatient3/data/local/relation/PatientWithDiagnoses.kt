package com.example.sqlitepatient3.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.data.local.entity.PatientDiagnosisEntity
import com.example.sqlitepatient3.domain.model.Patient
import com.example.sqlitepatient3.domain.model.PatientDiagnosis

/**
 * Represents a one-to-many relationship between a patient and their diagnoses.
 * This class is used by Room to handle complex relationships between tables.
 */
data class PatientWithDiagnoses(
    @Embedded val patient: PatientEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "patientId"
    )
    val diagnoses: List<PatientDiagnosisEntity>
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Pair<Patient, List<PatientDiagnosis>> {
        return Pair(
            patient.toDomainModel(),
            diagnoses.map { it.toDomainModel() }
        )
    }
}