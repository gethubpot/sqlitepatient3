package com.example.sqlitepatient3.data.repository

import com.example.sqlitepatient3.data.local.dao.EventDao
import com.example.sqlitepatient3.data.local.entity.EventEntity
import com.example.sqlitepatient3.domain.model.Event
import com.example.sqlitepatient3.domain.model.EventStatus
import com.example.sqlitepatient3.domain.model.EventType
import com.example.sqlitepatient3.domain.model.FollowUpRecurrence
import com.example.sqlitepatient3.domain.model.VisitLocation
import com.example.sqlitepatient3.domain.model.VisitType
import com.example.sqlitepatient3.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun getAllEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEventsByPatient(patientId: Long): Flow<List<Event>> {
        return eventDao.getEventsByPatient(patientId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEventsByType(eventType: EventType): Flow<List<Event>> {
        return eventDao.getEventsByType(eventType.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEventsByStatus(status: EventStatus): Flow<List<Event>> {
        return eventDao.getEventsByStatus(status.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getEventsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<Event>> {
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return eventDao.getEventsBetweenDates(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getEventById(id: Long): Event? {
        return eventDao.getEventById(id)?.toDomainModel()
    }

    override suspend fun insertEvent(
        patientId: Long,
        eventType: EventType,
        visitType: VisitType,
        visitLocation: VisitLocation,
        eventMinutes: Int,
        noteText: String?,
        eventDateTime: LocalDateTime,
        followUpRecurrence: FollowUpRecurrence
    ): Long {
        val eventBillDate = eventDateTime.toLocalDate()

        val event = EventEntity(
            patientId = patientId,
            eventDateTime = eventDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            eventBillDate = eventBillDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                .toEpochMilli(),
            eventMinutes = eventMinutes,
            noteText = noteText,
            cptCode = calculateDefaultCptCode(eventType, visitType, eventMinutes),
            modifier = null,
            eventFile = null,
            eventType = eventType.name,
            visitType = visitType.name,
            visitLocation = visitLocation.name,
            status = EventStatus.PENDING.name,
            followUpRecurrence = followUpRecurrence.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        return eventDao.insertEvent(event)
    }

    override suspend fun updateEvent(event: Event) {
        val entity = EventEntity.Companion.fromDomainModel(event.copy(updatedAt = System.currentTimeMillis()))
        eventDao.updateEvent(entity)
    }

    override suspend fun deleteEvent(event: Event) {
        val entity = EventEntity.Companion.fromDomainModel(event)
        eventDao.deleteEvent(entity)
    }

    override suspend fun updateEventStatus(eventId: Long, newStatus: EventStatus) {
        eventDao.updateEventStatus(eventId, newStatus.name)
    }

    override suspend fun deleteAllPatientEvents(patientId: Long) {
        eventDao.deleteAllPatientEvents(patientId)
    }

    override suspend fun countEventsByTypeInPeriod(
        eventType: EventType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return eventDao.countEventsByTypeInPeriod(eventType.name, startTime, endTime)
    }

    override suspend fun countEventsByStatusInPeriod(
        status: EventStatus,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return eventDao.countEventsByStatusInPeriod(status.name, startTime, endTime)
    }

    override fun getRecentEvents(daysCount: Int): Flow<List<Event>> {
        val currentTime = System.currentTimeMillis()
        val pastTime = currentTime - (daysCount * 24 * 60 * 60 * 1000)
        return eventDao.getEventsBetweenDates(pastTime, currentTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getUnbilledEvents(): Flow<List<Event>> {
        return eventDao.getUnbilledEvents(EventStatus.COMPLETED.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    // Helper methods
    private fun calculateDefaultCptCode(eventType: EventType, visitType: VisitType, eventMinutes: Int): String? {
        return when (eventType) {
            EventType.FACE_TO_FACE -> when(visitType) {
                VisitType.HOME_VISIT -> "99348"
                VisitType.NURSING_FACILITY -> "99318"
                VisitType.TELEHEALTH -> "99457"
                else -> null
            }
            EventType.CCM -> when {
                eventMinutes >= 60 -> "99487"
                eventMinutes >= 20 -> "99490"
                else -> null
            }
            EventType.TCM -> "99495"
            EventType.HOSPICE -> "G0182"
            EventType.HOME_HEALTH -> if (eventMinutes >= 30) "G0181" else null
            else -> null
        }
    }
}