package com.example.sqlitepatient3.data.local.dao

import androidx.room.*
import com.example.sqlitepatient3.data.local.entity.FacilityEntity
import com.example.sqlitepatient3.data.local.relation.FacilityWithPatients
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the facilities table.
 */
@Dao
interface FacilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacility(facility: FacilityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacilities(facilities: List<FacilityEntity>): List<Long>

    @Update
    suspend fun updateFacility(facility: FacilityEntity)

    @Delete
    suspend fun deleteFacility(facility: FacilityEntity)

    @Query("SELECT * FROM facilities WHERE id = :facilityId")
    suspend fun getFacilityById(facilityId: Long): FacilityEntity?

    @Query("SELECT * FROM facilities ORDER BY name")
    fun getAllFacilities(): Flow<List<FacilityEntity>>

    @Query("SELECT * FROM facilities WHERE isActive = 1 ORDER BY name")
    fun getActiveFacilities(): Flow<List<FacilityEntity>>

    @Query("SELECT COUNT(*) FROM facilities")
    suspend fun getFacilityCount(): Int

    @Query("SELECT COUNT(*) FROM facilities WHERE isActive = 1")
    suspend fun getActiveFacilityCount(): Int

    @Query("SELECT * FROM facilities WHERE name LIKE :query OR lastName LIKE :query OR firstName LIKE :query OR facilityCode LIKE :query")
    fun searchFacilities(query: String): Flow<List<FacilityEntity>>

    @Transaction
    @Query("UPDATE facilities SET isActive = :isActive, updatedAt = :timestamp WHERE id = :facilityId")
    suspend fun updateActiveStatus(facilityId: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Transaction
    @Query("SELECT * FROM facilities WHERE id = :facilityId")
    fun getFacilityWithPatients(facilityId: Long): Flow<FacilityWithPatients?>
}