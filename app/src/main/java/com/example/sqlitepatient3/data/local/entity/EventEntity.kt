package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.sqlitepatient3.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Room database entity representing a medical event.
 * This class maps directly to a table in the database.
 */
@Entity(
    tableName = "events",
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
        Index(value = ["eventDateTime"]),
        Index(value = ["status"]),
        Index(value = ["eventType"]),
        // Composite indices for common query patterns
        Index(value = ["patientId", "status"]),
        Index(value = ["patientId", "eventType"]),
        Index(value = ["eventType", "status"]),
        Index(value = ["eventDateTime", "status"]),
        Index(value = ["eventBillDate", "status"]),
        Index(value = ["patientId", "eventDateTime"]),
        Index(value = ["monthlyBillingId"]),
        Index(value = ["followUpRecurrence", "eventDateTime"])
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val patientId: Long,
    val eventDateTime: Long, // Stored as epoch millis
    val eventBillDate: Long, // Stored as epoch millis
    val eventMinutes: Int = 0,
    val noteText: String? = null,
    val cptCode: String? = null,
    val modifier: String? = null,
    val eventFile: String? = null,
    val eventType: String, // Store as string representation of EventType enum
    val visitType: String, // Store as string representation of VisitType enum
    val visitLocation: String, // Store as string representation of VisitLocation enum
    val status: String, // Store as string representation of EventStatus enum
    val hospDischargeDate: Long? = null, // Stored as epoch millis
    val ttddDate: Long? = null, // Stored as epoch millis
    val monthlyBillingId: Long? = null,
    val followUpRecurrence: String, // Store as string representation of FollowUpRecurrence enum
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert from Entity to Domain model
     */
    fun toDomainModel(): Event {
        return Event(
            id = id,
            patientId = patientId,
            eventDateTime = Instant.ofEpochMilli(eventDateTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
            eventBillDate = Instant.ofEpochMilli(eventBillDate).atZone(ZoneId.systemDefault()).toLocalDate(),
            eventMinutes = eventMinutes,
            noteText = noteText,
            cptCode = cptCode,
            modifier = modifier,
            eventFile = eventFile,
            eventType = EventType.valueOf(eventType),
            visitType = VisitType.valueOf(visitType),
            visitLocation = VisitLocation.valueOf(visitLocation),
            status = EventStatus.valueOf(status),
            hospDischargeDate = hospDischargeDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            ttddDate = ttddDate?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            },
            monthlyBillingId = monthlyBillingId,
            followUpRecurrence = FollowUpRecurrence.valueOf(followUpRecurrence),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Convert from Domain model to Entity
         */
        fun fromDomainModel(event: Event): EventEntity {
            return EventEntity(
                id = event.id,
                patientId = event.patientId,
                eventDateTime = event.eventDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                eventBillDate = event.eventBillDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                eventMinutes = event.eventMinutes,
                noteText = event.noteText,
                cptCode = event.cptCode,
                modifier = event.modifier,
                eventFile = event.eventFile,
                eventType = event.eventType.name,
                visitType = event.visitType.name,
                visitLocation = event.visitLocation.name,
                status = event.status.name,
                hospDischargeDate = event.hospDischargeDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                ttddDate = event.ttddDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                monthlyBillingId = event.monthlyBillingId,
                followUpRecurrence = event.followUpRecurrence.name,
                createdAt = event.createdAt,
                updatedAt = event.updatedAt
            )
        }
    }
}