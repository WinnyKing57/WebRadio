package com.example.webradioapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteStationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(station: RadioStation) // Will ensure isFavorite is true on the object

    @Query("UPDATE stations SET is_favorite = 0 WHERE id = :stationId")
    suspend fun removeFavoriteById(stationId: String)

    // Use this to update isFavorite flag on any station object
    @Query("UPDATE stations SET is_favorite = :isFavorite WHERE id = :stationId")
    suspend fun setFavoriteStatus(stationId: String, isFavorite: Boolean)

    // Insert or update a station, then separately set its favorite status
    // This is useful if a station object comes from API and isn't marked as favorite yet
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Ignore if station already exists, update fav status separately
    suspend fun insertStation(station: RadioStation): Long // Returns rowId, -1 if ignored

    // A combined method to add a station (if new) and mark as favorite
    // Or just mark as favorite if already exists.
    suspend fun addOrUpdateAndFavorite(station: RadioStation) {
        insertStation(station) // Add if not present, ignore if present.
        setFavoriteStatus(station.id, true) // Mark as favorite.
    }


    @Query("SELECT * FROM stations WHERE is_favorite = 1 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<RadioStation>>

    @Query("SELECT EXISTS(SELECT 1 FROM stations WHERE id = :stationId AND is_favorite = 1)")
    suspend fun isFavorite(stationId: String): Boolean

    // Get a single station to check its status (useful for UI updates)
    @Query("SELECT * FROM stations WHERE id = :stationId")
    fun getStationById(stationId: String): Flow<RadioStation?>
}
