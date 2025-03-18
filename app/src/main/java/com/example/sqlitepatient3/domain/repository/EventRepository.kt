package com.example.sqlitepatient3.domain.repository

import com.example.sqlitepatient3.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository interface for accessing event data.
 */
interface EventRepository {
    // Read operations
    fun getAllEvents(): Flow<List<Event>>
    fun getEventsByPatient(patientId: Long): Flow<List<Event>>
    fun getEventsByType(eventType: EventType): Flow<List<Event>>
    fun getEventsByStatus(status: EventStatus): Flow<List<Event>>
    fun getEventsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Event>>

    suspend fun getEventById(id: Long): Event?

    // Write operations
    suspend fun insertEvent(
        patientId: Long,
        eventType: EventType,
        visitType: VisitType = VisitType.NON_VISIT,
        visitLocation: VisitLocation = VisitLocation.NONE,
        eventMinutes: Int = 0,
        noteText: String? = null,
        eventDateTime: LocalDateTime = LocalDateTime.now(),
        followUpRecurrence: FollowUpRecurrence = FollowUpRecurrence.NONE
    ): Long

    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(event: Event)
    suspend fun updateEventStatus(eventId: Long, newStatus: EventStatus)
    suspend fun deleteAllPatientEvents(patientId: Long)

    // Statistics operations
    suspend fun countEventsByTypeInPeriod(eventType: EventType, startDate: LocalDate, endDate: LocalDate): Int
    suspend fun countEventsByStatusInPeriod(status: EventStatus, startDate: LocalDate, endDate: LocalDate): Int

    // Helper methods
    fun getRecentEvents(daysCount: Int = 7): Flow<List<Event>>
    fun getUnbilledEvents(): Flow<List<Event>>
}