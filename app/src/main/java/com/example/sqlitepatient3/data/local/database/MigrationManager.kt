package com.example.sqlitepatient3.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.ArrayDeque

/**
 * Manages database migrations by tracking the database version and
 * automatically determining the optimal migration path between versions.
 */
class MigrationManager(private val context: Context) {
    companion object {
        private const val TAG = "MigrationManager"
        private const val PREF_NAME = "database_migration_prefs"
        private const val PREF_LAST_VERSION = "last_known_version"

        // Current database version - should match Room @Database version
        private const val CURRENT_VERSION = 4
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Get the last known database version or 0 if this is first run
     */
    private fun getLastKnownVersion(): Int {
        return prefs.getInt(PREF_LAST_VERSION, 0)
    }

    /**
     * Update the last known database version in preferences
     */
    private fun updateLastKnownVersion(version: Int) {
        prefs.edit().putInt(PREF_LAST_VERSION, version).apply()
        Log.d(TAG, "Updated last known database version to $version")
    }

    /**
     * Determines the necessary migrations to run based on the last known
     * database version and the current version.
     *
     * @return Array of Migration objects to apply
     */
    fun getMigrationsToRun(): Array<Migration> {
        val lastVersion = getLastKnownVersion()

        Log.d(TAG, "Last known version: $lastVersion, Current version: $CURRENT_VERSION")

        // If this is a new install or we're already at the current version, no migrations needed
        if (lastVersion == 0 || lastVersion == CURRENT_VERSION) {
            updateLastKnownVersion(CURRENT_VERSION)
            return emptyArray()
        }

        // Find the most efficient migration path
        return when {
            // Direct path from lastVersion to currentVersion if available
            DatabaseMigrations.ALL_MIGRATIONS.any {
                it.startVersion == lastVersion && it.endVersion == CURRENT_VERSION
            } -> {
                Log.d(TAG, "Found direct migration path from $lastVersion to $CURRENT_VERSION")
                DatabaseMigrations.ALL_MIGRATIONS.filter {
                    it.startVersion == lastVersion && it.endVersion == CURRENT_VERSION
                }.toTypedArray()
            }

            // Otherwise, find the sequence of migrations needed
            else -> {
                Log.d(TAG, "Finding optimal migration path from $lastVersion to $CURRENT_VERSION")
                findMigrationPath(lastVersion, CURRENT_VERSION)
            }
        }.also {
            // Log the migration path
            Log.d(TAG, "Migration path: ${it.joinToString(", ") {
                "v${it.startVersion} to v${it.endVersion}"
            }}")

            // Update the last known version after migrations
            updateLastKnownVersion(CURRENT_VERSION)
        }
    }

    /**
     * Finds the shortest path of migrations from start to end version.
     * Uses a breadth-first search algorithm to find the optimal path.
     */
    private fun findMigrationPath(startVersion: Int, endVersion: Int): Array<Migration> {
        // Simple graph search for the shortest path
        val availableMigrations = DatabaseMigrations.ALL_MIGRATIONS

        // Map of version -> list of direct migrations from that version
        val migrationGraph = availableMigrations.groupBy { it.startVersion }

        // Track visited versions and the path to them
        val visited = mutableSetOf<Int>()
        val queue = ArrayDeque<Pair<Int, List<Migration>>>()
        queue.add(Pair(startVersion, emptyList()))

        while (queue.isNotEmpty()) {
            val (currentVersion, path) = queue.removeFirst()

            if (currentVersion == endVersion) {
                return path.toTypedArray()
            }

            if (currentVersion in visited) continue
            visited.add(currentVersion)

            // Add all possible next migrations
            migrationGraph[currentVersion]?.forEach { migration ->
                if (migration.endVersion !in visited) {
                    queue.add(Pair(migration.endVersion, path + migration))
                }
            }
        }

        // If no path found, use default migrations (should never happen with well-defined migrations)
        Log.w(TAG, "No migration path found from $startVersion to $endVersion")
        return availableMigrations.filter {
            it.startVersion >= startVersion && it.endVersion <= endVersion
        }.sortedBy { it.startVersion }.toTypedArray()
    }

    /**
     * Verify database integrity after migration
     */
    fun verifyDatabaseIntegrity(database: SupportSQLiteDatabase): Boolean {
        return try {
            val cursor = database.query("PRAGMA integrity_check")
            cursor.use {
                if (it.moveToFirst()) {
                    val result = it.getString(0)
                    val isValid = result == "ok"
                    if (isValid) {
                        Log.d(TAG, "Database integrity check passed")
                    } else {
                        Log.e(TAG, "Database integrity check failed: $result")
                    }
                    isValid
                } else {
                    Log.e(TAG, "Database integrity check failed: empty result")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing integrity check", e)
            false
        }
    }
}