package com.example.sqlitepatient3.domain.repository

import com.example.sqlitepatient3.domain.model.Facility
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for accessing facility data.
 */
interface FacilityRepository {
    // Read operations
    fun getAllFacilities(): Flow<List<Facility>>
    fun getActiveFacilities(): Flow<List<Facility>>
    fun searchFacilities(query: String): Flow<List<Facility>>

    suspend fun getFacilityById(id: Long): Facility?
    suspend fun getFacilityCount(): Int
    suspend fun getActiveFacilityCount(): Int

    // Write operations
    suspend fun insertFacility(facility: Facility): Long
    suspend fun insertFacilities(facilities: List<Facility>): List<Long>
    suspend fun updateFacility(facility: Facility)
    suspend fun deleteFacility(facility: Facility)

    suspend fun toggleFacilityActiveStatus(facilityId: Long): Boolean

    // Relations
    fun getFacilityWithPatients(facilityId: Long): Flow<Pair<Facility, List<com.example.sqlitepatient3.domain.model.Patient>>?>
}