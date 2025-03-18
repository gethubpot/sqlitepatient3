package com.example.sqlitepatient3.domain.model

/**
 * Enum representing the type of medical event
 */
enum class EventType {
    FACE_TO_FACE,
    CCM,                 // Chronic Care Management
    TCM,                 // Transitional Care Management
    HOSPICE,
    HOME_HEALTH,
    FOLLOW_UP,
    MEDICATION_REVIEW,
    PSY,                 // Psychiatric Services
    DNR,                 // Advance Care Planning
    OTHER;

    override fun toString(): String {
        return when (this) {
            FACE_TO_FACE -> "F2F"
            CCM -> "CCM"
            TCM -> "TCM"
            HOSPICE -> "G0"
            HOME_HEALTH -> "G01"
            FOLLOW_UP -> "FU"
            MEDICATION_REVIEW -> "Meds"
            PSY -> "Psy"
            DNR -> "DNR"
            OTHER -> "Other"
        }
    }
}

/**
 * Enum representing the visit type for a medical event
 */
enum class VisitType {
    NON_VISIT,
    HOME_VISIT,
    NURSING_FACILITY,
    TELEHEALTH,
    OFFICE_VISIT;

    override fun toString(): String {
        return when (this) {
            NON_VISIT -> "Non-Visit"
            HOME_VISIT -> "Home Visit"
            NURSING_FACILITY -> "Nursing Facility"
            TELEHEALTH -> "Telehealth"
            OFFICE_VISIT -> "Office Visit"
        }
    }
}

/**
 * Enum representing the location of a patient visit
 */
enum class VisitLocation {
    NONE,
    PATIENT_HOME,
    SKILLED_NURSING,
    ASSISTED_LIVING,
    GROUP_HOME,
    HOSPITAL,
    OFFICE;

    override fun toString(): String {
        return when (this) {
            NONE -> "None"
            PATIENT_HOME -> "Patient Home"
            SKILLED_NURSING -> "Skilled Nursing Facility"
            ASSISTED_LIVING -> "Assisted Living Facility"
            GROUP_HOME -> "Group Home"
            HOSPITAL -> "Hospital"
            OFFICE -> "Office"
        }
    }
}

/**
 * Enum representing the status of a medical event
 */
enum class EventStatus {
    PENDING,
    COMPLETED,
    BILLED,
    PAID,
    CANCELLED,
    NO_SHOW;

    override fun toString(): String {
        return when (this) {
            PENDING -> "Pending"
            COMPLETED -> "Completed"
            BILLED -> "Billed"
            PAID -> "Paid"
            CANCELLED -> "Cancelled"
            NO_SHOW -> "No Show"
        }
    }
}

/**
 * Enum representing the recurrence pattern for follow-up events
 */
enum class FollowUpRecurrence {
    NONE,
    WEEKLY,
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUAL,
    ANNUAL;

    override fun toString(): String {
        return when (this) {
            NONE -> "None"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            QUARTERLY -> "Quarterly"
            SEMI_ANNUAL -> "Semi-Annual"
            ANNUAL -> "Annual"
        }
    }
}