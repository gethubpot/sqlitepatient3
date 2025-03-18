package com.example.sqlitepatient3.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity for storing system-wide properties and settings.
 * Used for tracking database version history and application configuration.
 */
@Entity(tableName = "system_properties")
data class SystemPropertyEntity(
    @PrimaryKey
    val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)