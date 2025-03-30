package com.example.sqlitepatient3.data.local.dao

import androidx.room.*
import com.example.sqlitepatient3.data.local.entity.EventEntity
import com.example.sqlitepatient3.data.local.relation.PatientWithEvents
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the events table___>
 */
@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>): List<Long>

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): EventEntity?

    @Query("SELECT * FROM events ORDER BY eventDateTime DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE patientId = :patientId ORDER BY eventDateTime DESC")
    fun getEventsByPatient(patientId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventType = :eventType ORDER BY eventDateTime DESC")
    fun getEventsByType(eventType: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE status = :status ORDER BY eventDateTime DESC")
    fun getEventsByStatus(status: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventDateTime BETWEEN :startTime AND :endTime ORDER BY eventDateTime")
    fun getEventsBetweenDates(startTime: Long, endTime: Long): Flow<List<EventEntity>>

    @Transaction
    @Query("UPDATE events SET status = :newStatus, updatedAt = :timestamp WHERE id = :eventId")
    suspend fun updateEventStatus(eventId: Long, newStatus: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM events WHERE patientId = :patientId")
    suspend fun deleteAllPatientEvents(patientId: Long)

    // Statistics and reporting queries
    @Query("SELECT COUNT(*) FROM events WHERE eventType = :eventType AND eventDateTime BETWEEN :startTime AND :endTime")
    suspend fun countEventsByTypeInPeriod(eventType: String, startTime: Long, endTime: Long): Int

    @Query("SELECT COUNT(*) FROM events WHERE status = :status AND eventDateTime BETWEEN :startTime AND :endTime")
    suspend fun countEventsByStatusInPeriod(status: String, startTime: Long, endTime: Long): Int

    // For potential billing purposes
    @Query("SELECT * FROM events WHERE status = :status AND monthlyBillingId IS NULL")
    fun getUnbilledEvents(status: String): Flow<List<EventEntity>>

    // Relations
    @Transaction
    @Query("SELECT * FROM patients WHERE id = :patientId")
    fun getPatientWithEvents(patientId: Long): Flow<PatientWithEvents?>
}