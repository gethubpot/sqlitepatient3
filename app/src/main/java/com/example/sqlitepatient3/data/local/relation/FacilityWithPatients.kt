package com.example.sqlitepatient3.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sqlitepatient3.data.local.entity.FacilityEntity
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.domain.model.Facility
import com.example.sqlitepatient3.domain.model.Patient

/**
 * Represents a one-to-many relationship between a facility and its patients.
 * This class is used by Room to handle complex relationships between tables.
 */
data class FacilityWithPatients(
    @Embedded val facility: FacilityEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "facilityId"
    )
    val patients: List<PatientEntity>
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Pair<Facility, List<Patient>> {
        return Pair(
            facility.toDomainModel(),
            patients.map { it.toDomainModel() }
        )
    }
}