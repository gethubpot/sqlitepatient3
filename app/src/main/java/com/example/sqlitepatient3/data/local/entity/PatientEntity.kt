package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sqlitepatient3.domain.model.Patient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Room database entity representing a patient.
 * This class maps directly to a table in the database.
 */
@Entity(
    tableName = "patients",
    foreignKeys = [
        ForeignKey(
            entity = FacilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["facilityId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["upi"], unique = true),
        Index(value = ["facilityId"]),
        Index(value = ["lastName", "firstName"]),
        // Composite indices for common query patterns
        Index(value = ["facilityId", "isHospice"]),
        Index(value = ["facilityId", "onCcm"]),
        Index(value = ["facilityId", "onPsych"]),
        Index(value = ["isHospice", "onCcm", "onPsych"]), // For filtering by multiple statuses
        Index(value = ["onPsyMed", "psyMedReviewDate"]) // For medication review queries
    ]
)
data class PatientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val upi: String,
    val dateOfBirth: Long? = null,  // Stored as epoch millis
    val isMale: Boolean,
    val medicareNumber: String = "",
    val facilityId: Long? = null,
    val isHospice: Boolean = false,
    val onCcm: Boolean = false,
    val onPsych: Boolean = false,
    val onPsyMed: Boolean = false,
    val psyMedReviewDate: Long? = null,  // Stored as epoch millis
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Patient {
        return Patient(
            id = id,
            firstName = firstName,
            lastName = lastName,
            upi = upi,
            dateOfBirth = dateOfBirth?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            isMale = isMale,
            medicareNumber = medicareNumber,
            facilityId = facilityId,
            isHospice = isHospice,
            onCcm = onCcm,
            onPsych = onPsych,
            onPsyMed = onPsyMed,
            psyMedReviewDate = psyMedReviewDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Convert from Domain model to Entity
         */
        fun fromDomainModel(patient: Patient): PatientEntity {
            return PatientEntity(
                id = patient.id,
                firstName = patient.firstName,
                lastName = patient.lastName,
                upi = patient.upi,
                dateOfBirth = patient.dateOfBirth?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                isMale = patient.isMale,
                medicareNumber = patient.medicareNumber,
                facilityId = patient.facilityId,
                isHospice = patient.isHospice,
                onCcm = patient.onCcm,
                onPsych = patient.onPsych,
                onPsyMed = patient.onPsyMed,
                psyMedReviewDate = patient.psyMedReviewDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                createdAt = patient.createdAt,
                updatedAt = patient.updatedAt
            )
        }
    }
}