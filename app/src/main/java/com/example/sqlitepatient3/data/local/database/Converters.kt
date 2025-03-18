package com.example.sqlitepatient3.data.local.database

import androidx.room.TypeConverter
import com.example.sqlitepatient3.domain.model.*
import java.time.*

/**
 * Type converters for Room database to handle conversion between complex types
 * and primitives that can be stored in SQLite.
 */
class Converters {
    // LocalDate converters
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    }

    // LocalDateTime converters
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? {
        return value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        }
    }

    // EventType converters
    @TypeConverter
    fun fromEventType(value: EventType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEventType(value: String?): EventType? {
        return value?.let {
            try {
                EventType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                EventType.OTHER
            }
        }
    }

    // VisitType converters
    @TypeConverter
    fun fromVisitType(value: VisitType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toVisitType(value: String?): VisitType? {
        return value?.let {
            try {
                VisitType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                VisitType.NON_VISIT
            }
        }
    }

    // VisitLocation converters
    @TypeConverter
    fun fromVisitLocation(value: VisitLocation?): String? {
        return value?.name
    }

    @TypeConverter
    fun toVisitLocation(value: String?): VisitLocation? {
        return value?.let {
            try {
                VisitLocation.valueOf(it)
            } catch (e: IllegalArgumentException) {
                VisitLocation.NONE
            }
        }
    }

    // EventStatus converters
    @TypeConverter
    fun fromEventStatus(value: EventStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toEventStatus(value: String?): EventStatus? {
        return value?.let {
            try {
                EventStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                EventStatus.PENDING
            }
        }
    }

    // FollowUpRecurrence converters
    @TypeConverter
    fun fromFollowUpRecurrence(value: FollowUpRecurrence?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFollowUpRecurrence(value: String?): FollowUpRecurrence? {
        return value?.let {
            try {
                FollowUpRecurrence.valueOf(it)
            } catch (e: IllegalArgumentException) {
                FollowUpRecurrence.NONE
            }
        }
    }
}