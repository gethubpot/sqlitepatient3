package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sqlitepatient3.domain.model.PatientDiagnosis
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Room database entity representing a patient diagnosis.
 * This class maps directly to a table in the database.
 */
@Entity(
    tableName = "patient_diagnoses",
    foreignKeys = [
        ForeignKey(
            entity = PatientEntity::class,
            parentColumns = ["id"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["patientId"]),
        Index(value = ["icdCode"]),
        Index(value = ["priority"]),
        Index(value = ["isHospiceCode"]),
        Index(value = ["active"])
    ]
)
data class PatientDiagnosisEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val icdCode: String,
    val priority: Int,
    val isHospiceCode: Boolean = false,
    val diagnosisDate: Long? = null,  // Stored as epoch millis
    val resolvedDate: Long? = null,  // Stored as epoch millis
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): PatientDiagnosis {
        return PatientDiagnosis(
            id = id,
            patientId = patientId,
            icdCode = icdCode,
            priority = priority,
            isHospiceCode = isHospiceCode,
            diagnosisDate = diagnosisDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            resolvedDate = resolvedDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            notes = notes,
            active = active
        )
    }

    companion object {
        /**
         * Convert from Domain model to Entity
         */
        fun fromDomainModel(diagnosis: PatientDiagnosis): PatientDiagnosisEntity {
            return PatientDiagnosisEntity(
                id = diagnosis.id,
                patientId = diagnosis.patientId,
                icdCode = diagnosis.icdCode,
                priority = diagnosis.priority,
                isHospiceCode = diagnosis.isHospiceCode,
                diagnosisDate = diagnosis.diagnosisDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                resolvedDate = diagnosis.resolvedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                notes = diagnosis.notes,
                active = diagnosis.active,
                createdAt = System.currentTimeMillis(), // Always use current time for new records
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}