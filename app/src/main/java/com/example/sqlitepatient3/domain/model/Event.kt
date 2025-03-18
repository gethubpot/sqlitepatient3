package com.example.sqlitepatient3.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model representing a medical event in the application.
 * This class is independent of any framework or database implementation.
 */
data class Event(
    val id: Long = 0,
    val patientId: Long,
    val eventDateTime: LocalDateTime,
    val eventBillDate: LocalDate,
    val eventMinutes: Int = 0,
    val noteText: String? = null,
    val cptCode: String? = null,
    val modifier: String? = null,
    val eventFile: String? = null,
    val eventType: EventType,
    val visitType: VisitType = VisitType.NON_VISIT,
    val visitLocation: VisitLocation = VisitLocation.NONE,
    val status: EventStatus = EventStatus.PENDING,
    val hospDischargeDate: LocalDate? = null,
    val ttddDate: LocalDate? = null,
    val monthlyBillingId: Long? = null,
    val followUpRecurrence: FollowUpRecurrence = FollowUpRecurrence.NONE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isFaceToFaceEncounter(): Boolean =
        eventType == EventType.FACE_TO_FACE && visitLocation != VisitLocation.NONE

    fun isBillable(): Boolean =
        when (eventType) {
            EventType.FACE_TO_FACE -> when(visitType) {
                VisitType.HOME_VISIT,
                VisitType.NURSING_FACILITY,
                VisitType.TELEHEALTH -> true
                else -> false
            }
            EventType.CCM -> eventMinutes >= 20
            EventType.TCM -> true
            EventType.HOSPICE -> true
            EventType.HOME_HEALTH -> eventMinutes >= 30
            else -> false
        }

    fun calculateNextFollowUpDate(): LocalDateTime? {
        if (followUpRecurrence == FollowUpRecurrence.NONE) return null

        val baseDateTime = ttddDate?.atStartOfDay() ?: eventDateTime

        return when (followUpRecurrence) {
            FollowUpRecurrence.WEEKLY -> baseDateTime.plusWeeks(1)
            FollowUpRecurrence.MONTHLY -> baseDateTime.plusMonths(1)
            FollowUpRecurrence.QUARTERLY -> baseDateTime.plusMonths(3)
            FollowUpRecurrence.SEMI_ANNUAL -> baseDateTime.plusMonths(6)
            FollowUpRecurrence.ANNUAL -> baseDateTime.plusYears(1)
            FollowUpRecurrence.NONE -> null
        }
    }

    fun getMinimumRequiredMinutes(): Int =
        when (eventType) {
            EventType.CCM -> 20
            EventType.HOME_HEALTH -> 30
            else -> 0
        }

    fun getTimeRequirementDescription(): String =
        when (eventType) {
            EventType.CCM -> "Requires minimum 20 minutes, 60+ minutes for complex care"
            EventType.HOME_HEALTH -> "Requires minimum 30 minutes per calendar month"
            EventType.TCM -> "Must be within 30 days of discharge"
            EventType.HOSPICE -> "Regular supervision required"
            else -> "No specific time requirement"
        }
}