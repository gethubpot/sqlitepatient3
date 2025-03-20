package com.example.sqlitepatient3.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sqlitepatient3.data.local.entity.SystemPropertyEntity

/**
 * Data Access Object for the system_properties table.
 */
@Dao
interface SystemPropertiesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setProperty(property: SystemPropertyEntity)

    @Query("SELECT value FROM system_properties WHERE `key` = :key")
    suspend fun getProperty(key: String): String?
}