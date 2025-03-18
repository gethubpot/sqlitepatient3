package com.example.sqlitepatient3.domain.usecase.event

import com.example.sqlitepatient3.domain.model.*
import com.example.sqlitepatient3.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class GetAllEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getAllEvents()
    }
}

class GetEventsByPatientUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(patientId: Long): Flow<List<Event>> {
        return eventRepository.getEventsByPatient(patientId)
    }
}

class GetEventsByTypeUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(eventType: EventType): Flow<List<Event>> {
        return eventRepository.getEventsByType(eventType)
    }
}

class GetEventsByStatusUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(status: EventStatus): Flow<List<Event>> {
        return eventRepository.getEventsByStatus(status)
    }
}

class GetEventsByDateRangeUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<Event>> {
        return eventRepository.getEventsBetweenDates(startDate, endDate)
    }
}

class GetRecentEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(daysCount: Int = 7): Flow<List<Event>> {
        return eventRepository.getRecentEvents(daysCount)
    }
}

class GetUnbilledEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getUnbilledEvents()
    }
}

class GetEventByIdUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(id: Long): Event? {
        return eventRepository.getEventById(id)
    }
}

class AddEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        patientId: Long,
        eventType: EventType,
        visitType: VisitType = VisitType.NON_VISIT,
        visitLocation: VisitLocation = VisitLocation.NONE,
        eventMinutes: Int = 0,
        noteText: String? = null,
        eventDateTime: LocalDateTime = LocalDateTime.now(),
        followUpRecurrence: FollowUpRecurrence = FollowUpRecurrence.NONE
    ): Long {
        return eventRepository.insertEvent(
            patientId = patientId,
            eventType = eventType,
            visitType = visitType,
            visitLocation = visitLocation,
            eventMinutes = eventMinutes,
            noteText = noteText,
            eventDateTime = eventDateTime,
            followUpRecurrence = followUpRecurrence
        )
    }
}

class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.updateEvent(event)
    }
}

class DeleteEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event) {
        eventRepository.deleteEvent(event)
    }
}

class UpdateEventStatusUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: Long, newStatus: EventStatus) {
        eventRepository.updateEventStatus(eventId, newStatus)
    }
}

class DeleteAllPatientEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(patientId: Long) {
        eventRepository.deleteAllPatientEvents(patientId)
    }
}

class GetEventStatisticsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend fun countEventsByTypeInPeriod(
        eventType: EventType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        return eventRepository.countEventsByTypeInPeriod(eventType, startDate, endDate)
    }

    suspend fun countEventsByStatusInPeriod(
        status: EventStatus,
        startDate: LocalDate,
        endDate: LocalDate
    ): Int {
        return eventRepository.countEventsByStatusInPeriod(status, startDate, endDate)
    }
}