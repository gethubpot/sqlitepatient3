package com.example.sqlitepatient3.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.sqlitepatient3.data.local.entity.EventEntity
import com.example.sqlitepatient3.data.local.entity.PatientEntity
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.Patient

/**
 * Represents a one-to-many relationship between a patient and their events.
 * This class is used by Room to handle complex relationships between tables.
 */
data class PatientWithEvents(
    @Embedded val patient: PatientEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "patientId"
    )
    val events: List<EventEntity>
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Pair<Patient, List<Event>> {
        return Pair(
            patient.toDomainModel(),
            events.map { it.toDomainModel() }
        )
    }
}