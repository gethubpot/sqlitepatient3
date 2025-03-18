package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sqlitepatient3.domain.model.Facility

/**
 * Room database entity representing a healthcare facility or provider.
 * This class maps directly to a table in the database.
 */
@Entity(
    tableName = "facilities",
    indices = [
        Index(value = ["name"]),
        Index(value = ["facilityCode"], unique = true),
        // Additional indices for common query patterns
        Index(value = ["isActive"]),
        Index(value = ["lastName", "firstName"]),
        Index(value = ["city", "state"]),
        Index(value = ["npi"])
    ]
)
data class FacilityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val entityType: String? = null,
    val lastName: String? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val suffix: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val phoneNumber: String? = null,
    val faxNumber: String? = null,
    val email: String? = null,
    val npi: String? = null,
    val isActive: Boolean = true,
    val facilityCode: String? = null,  // Unique code for referencing this facility
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Facility {
        return Facility(
            id = id,
            name = name,
            entityType = entityType,
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            suffix = suffix,
            address1 = address1,
            address2 = address2,
            city = city,
            state = state,
            zipCode = zipCode,
            phoneNumber = phoneNumber,
            faxNumber = faxNumber,
            email = email,
            npi = npi,
            isActive = isActive,
            facilityCode = facilityCode,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Convert from Domain model to Entity
         */
        fun fromDomainModel(facility: Facility): FacilityEntity {
            return FacilityEntity(
                id = facility.id,
                name = facility.name,
                entityType = facility.entityType,
                lastName = facility.lastName,
                firstName = facility.firstName,
                middleName = facility.middleName,
                suffix = facility.suffix,
                address1 = facility.address1,
                address2 = facility.address2,
                city = facility.city,
                state = facility.state,
                zipCode = facility.zipCode,
                phoneNumber = facility.phoneNumber,
                faxNumber = facility.faxNumber,
                email = facility.email,
                npi = facility.npi,
                isActive = facility.isActive,
                facilityCode = facility.facilityCode,
                notes = facility.notes,
                createdAt = facility.createdAt,
                updatedAt = facility.updatedAt
            )
        }
    }
}