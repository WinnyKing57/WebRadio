package com.example.webradioapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryStationDao {

    // Insert a station if it doesn't exist, or ignore if it does.
    // The isFavorite flag from the passed station object will be written if it's a new insert.
    // If the station exists, its existing isFavorite status is preserved.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStationIfNotExists(station: RadioStation): Long // returns rowId, -1 if ignored

    // Update history timestamp and play count for an existing station.
    @Query("UPDATE stations SET last_played_timestamp = :timestamp, play_count = play_count + 1 WHERE id = :stationId")
    suspend fun updateHistoryTimestampAndIncrementPlayCount(stationId: String, timestamp: Long)

    // Combined transaction to add a station to history
    @Transaction
    suspend fun addStationToHistory(station: RadioStation, timestamp: Long) {
        // Ensure station object has its own favorite status correct if it's being inserted
        // Or, rely on a separate mechanism to update favorite status from a single source of truth.
        // For history, we mainly care about its existence and lastPlayedTimestamp.
        // The station object passed here might come from API without isFavorite set from DB.
        // So, insertStationIfNotExists is safer to avoid overwriting a known favorite status.
        val existingStation = getStationById(station.id)
        if (existingStation == null) {
            // New station, insert it. Ensure lastPlayedTimestamp and playCount are set.
            // The isFavorite status will be taken from the 'station' parameter.
            // If 'station' comes from an API call and doesn't have isFavorite status, it will be false.
            // This is generally fine for a new history entry.
            insertStationIfNotExists(station.copy(lastPlayedTimestamp = timestamp, playCount = 1))
        } else {
            // Station exists, just update its history details
            updateHistoryTimestampAndIncrementPlayCount(station.id, timestamp)
        }
    }

    // Helper to get a station by ID, not exposed as Flow, for internal DAO use
    @Query("SELECT * FROM stations WHERE id = :stationId LIMIT 1")
    suspend fun getStationById(stationId: String): RadioStation?

    @Query("SELECT * FROM stations WHERE last_played_timestamp > 0 ORDER BY last_played_timestamp DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int): Flow<List<RadioStation>>


//    @Query("SELECT * FROM stations WHERE last_played_timestamp > 0 ORDER BY last_played_timestamp DESC LIMIT :limit")
//    fun getStationHistory(limit: Int = 20): Flow<List<RadioStation>>
//
//    // For development/testing: Clear history (resets timestamp and count)
//    @Query("UPDATE stations SET last_played_timestamp = 0, play_count = 0")
//    suspend fun clearAllHistoryTimestamps()
}
