package com.example.sqlitepatient3.data.local.database

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Utility functions for database operations and maintenance.
 */
object DatabaseUtils {

    /**
     * Verifies the integrity of the SQLite database.
     * @param database The database to check
     * @return true if the database passes integrity check, false otherwise
     */
    fun verifyDatabaseIntegrity(database: SupportSQLiteDatabase): Boolean {
        val cursor = database.query("PRAGMA integrity_check")
        cursor.use {
            if (cursor.moveToFirst()) {
                val result = cursor.getString(0)
                return result == "ok"
            }
        }
        return false
    }

    /**
     * Performs database optimization by rebuilding the database file.
     * This reclaims storage space and defragments the database.
     */
    fun optimizeDatabase(database: SupportSQLiteDatabase) {
        database.execSQL("VACUUM")
        database.execSQL("ANALYZE")
    }
}