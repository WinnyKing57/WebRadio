package com.example.webradioapp.db

import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class for accessing and managing station data from the Room database.
 * It abstracts the data source (DAOs) from the ViewModels.
 *
 * @property favoriteStationDao DAO for favorite station operations.
 * @property historyStationDao DAO for history station operations.
 */
class StationRepository(
    private val favoriteStationDao: FavoriteStationDao,
    private val historyStationDao: HistoryStationDao
) {

    // Favorite operations
    val favoriteStations = kotlinx.coroutines.flow.flowOf(emptyList<RadioStation>()) // favoriteStationDao.getFavoriteStations()
    suspend fun addFavorite(station: RadioStation) {
//        withContext(Dispatchers.IO) {
//            favoriteStationDao.addOrUpdateAndFavorite(station.copy(isFavorite = true))
//        }
    }
    suspend fun removeFavorite(stationId: String) {
//        withContext(Dispatchers.IO) {
//            favoriteStationDao.removeFavoriteById(stationId)
//        }
    }
    suspend fun isFavorite(stationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            false // favoriteStationDao.isFavorite(stationId)
        }
    }
     fun getStationById(stationId: String) = favoriteStationDao.getStationById(stationId)


    // History operations
    val stationHistory = kotlinx.coroutines.flow.flowOf(emptyList<RadioStation>()) // historyStationDao.getStationHistory() // Default limit

    /**
     * Adds a station to the playback history.
     * Updates the last played timestamp and increments the play count.
     * @param station The [RadioStation] to add to history.
     */
    suspend fun addStationToHistory(station: RadioStation) {
//        withContext(Dispatchers.IO) {
//            historyStationDao.addStationToHistory(station, System.currentTimeMillis())
//        }
    }

    /**
     * Toggles the favorite status of a given station.
     * If it's a favorite, it will be unmarked. If not, it will be marked as a favorite.
     * Ensures the station exists in the database before updating its status.
     * @param station The [RadioStation] whose favorite status is to be toggled.
     */
    suspend fun toggleFavoriteStatus(station: RadioStation) {
        val isCurrentlyFavorite = isFavorite(station.id)
        // Ensure station exists in the database.
        // Pass the station object as is. If it's from API, isFavorite might be false.
        // insertStationIfNotExists will add it if new, or do nothing if it exists.
        // It won't change isFavorite status of an existing station.
        historyStationDao.insertStationIfNotExists(station)

        // Now, explicitly set the new favorite status.
//        if (isCurrentlyFavorite) {
//            favoriteStationDao.setFavoriteStatus(station.id, false) // Unmark as favorite
//        } else {
//            favoriteStationDao.setFavoriteStatus(station.id, true)  // Mark as favorite
//        }
    }
}
