package com.example.webradioapp.db

import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.example.webradioapp.db.dao.CountryDao
import com.example.webradioapp.db.dao.GenreDao
import com.example.webradioapp.db.dao.LanguageDao
import com.example.webradioapp.db.entities.CountryEntity
import com.example.webradioapp.db.entities.GenreEntity
import com.example.webradioapp.db.entities.LanguageEntity
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.network.Country // Network model
import com.example.webradioapp.network.Tag     // Network model for Genres
import com.example.webradioapp.network.Language // Network model
import kotlinx.coroutines.flow.first // To check if cache is empty
import android.util.Log // For logging
import android.content.Context // Added for SharedPreferencesManager
import com.example.webradioapp.utils.SharedPreferencesManager // Added for SharedPreferencesManager

/**
 * Repository class for accessing and managing station data from the Room database.
 * It abstracts the data source (DAOs) from the ViewModels.
 *
 * @property favoriteStationDao DAO for favorite station operations.
 * @property historyStationDao DAO for history station operations.
 */
class StationRepository(
    private val context: Context, // Added context
    private val favoriteStationDao: FavoriteStationDao,
    private val historyStationDao: HistoryStationDao,
    private val countryDao: CountryDao,
    private val genreDao: GenreDao,
    private val languageDao: LanguageDao,
    private val apiService: RadioBrowserApiService
) {

    private companion object {
        const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    // Favorite operations
    val favoriteStations = favoriteStationDao.getFavoriteStations()
    val allPersistedStations: kotlinx.coroutines.flow.Flow<List<com.example.webradioapp.model.RadioStation>> = favoriteStationDao.getAllStations() // Added
    suspend fun addFavorite(station: RadioStation) {
        withContext(Dispatchers.IO) {
            favoriteStationDao.addOrUpdateAndFavorite(station.copy(isFavorite = true))
        }
    }
    suspend fun removeFavorite(stationId: String) {
        withContext(Dispatchers.IO) {
            favoriteStationDao.removeFavoriteById(stationId)
        }
    }
    suspend fun isFavorite(stationId: String): Boolean {
        return withContext(Dispatchers.IO) {
            favoriteStationDao.isFavorite(stationId)
        }
    }
     fun getStationById(stationId: String) = favoriteStationDao.getStationById(stationId)


    // History operations
    val stationHistory = kotlinx.coroutines.flow.flowOf(emptyList<RadioStation>()) // historyStationDao.getStationHistory() // Default limit

    fun getRecentlyPlayedStations(limit: Int): Flow<List<RadioStation>> {
        return historyStationDao.getRecentlyPlayed(limit)
    }

    /**
     * Adds a station to the playback history.
     * Updates the last played timestamp and increments the play count.
     * @param station The [RadioStation] to add to history.
     */
    suspend fun addStationToHistory(station: RadioStation) {
        withContext(Dispatchers.IO) {
            historyStationDao.addStationToHistory(station, System.currentTimeMillis())
        }
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
        if (isCurrentlyFavorite) {
            favoriteStationDao.setFavoriteStatus(station.id, false) // Unmark as favorite
        } else {
            favoriteStationDao.setFavoriteStatus(station.id, true)  // Mark as favorite
        }
    }

    // Country operations
    fun getCountries(): Flow<List<CountryEntity>> {
        return countryDao.getAll()
    }

    suspend fun refreshCountries() {
        val lastUpdate = SharedPreferencesManager.getLastUpdateTimestamp(context, "last_update_countries") // Use appropriate key
        val isCacheStale = System.currentTimeMillis() - lastUpdate > CACHE_EXPIRY_MS
        val isCacheEmpty = countryDao.getAll().first().isEmpty()

        if (isCacheStale || isCacheEmpty) {
            Log.d("StationRepository", "Refreshing countries. Stale: $isCacheStale, Empty: $isCacheEmpty")
            try {
                val response = apiService.getCountries()
                if (response.isSuccessful) {
                    val countriesFromApi = response.body() ?: emptyList()
                    if (countriesFromApi.isNotEmpty()) {
                        countryDao.deleteAll()
                        countryDao.insertAll(countriesFromApi.map { CountryEntity(name = it.name, stationCount = it.stationcount) })
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_countries", System.currentTimeMillis())
                        Log.d("StationRepository", "Countries cache refreshed from API.")
                    } else if (isCacheEmpty) { // If API returns empty but cache was also empty, clear timestamp to try again sooner
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_countries", 0L)
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch countries: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching countries: ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Countries cache is fresh, no refresh needed now.")
        }
    }

    // Genre operations
    fun getGenres(): Flow<List<GenreEntity>> {
        return genreDao.getAll()
    }

    suspend fun refreshGenres() {
        val lastUpdate = SharedPreferencesManager.getLastUpdateTimestamp(context, "last_update_genres")
        val isCacheStale = System.currentTimeMillis() - lastUpdate > CACHE_EXPIRY_MS
        val isCacheEmpty = genreDao.getAll().first().isEmpty()

        if (isCacheStale || isCacheEmpty) {
            Log.d("StationRepository", "Refreshing genres. Stale: $isCacheStale, Empty: $isCacheEmpty")
            try {
                // Using getTags() from API for genres
                val response = apiService.getTags(limit = 200) // Fetch a reasonable number of tags
                if (response.isSuccessful) {
                    val tagsFromApi = response.body() ?: emptyList()
                    if (tagsFromApi.isNotEmpty()) {
                        val genreEntities = tagsFromApi.map { networkTag ->
                            GenreEntity(name = networkTag.name, stationCount = networkTag.stationcount)
                        }
                        genreDao.deleteAll()
                        genreDao.insertAll(genreEntities)
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_genres", System.currentTimeMillis())
                        Log.d("StationRepository", "Genres cache refreshed from API.")
                    } else if (isCacheEmpty) {
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_genres", 0L)
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch genres (tags): ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching genres (tags): ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Genres cache is fresh, no refresh needed now.")
        }
    }

    // Language operations
    fun getLanguages(): Flow<List<LanguageEntity>> {
        return languageDao.getAll()
    }

    suspend fun refreshLanguages() {
        val lastUpdate = SharedPreferencesManager.getLastUpdateTimestamp(context, "last_update_languages")
        val isCacheStale = System.currentTimeMillis() - lastUpdate > CACHE_EXPIRY_MS
        val isCacheEmpty = languageDao.getAll().first().isEmpty()

        if (isCacheStale || isCacheEmpty) {
            Log.d("StationRepository", "Refreshing languages. Stale: $isCacheStale, Empty: $isCacheEmpty")
            try {
                val response = apiService.getLanguages()
                if (response.isSuccessful) {
                    val languagesFromApi = response.body() ?: emptyList()
                    if (languagesFromApi.isNotEmpty()) {
                        val languageEntities = languagesFromApi.map { networkLanguage ->
                            LanguageEntity(name = networkLanguage.name, stationCount = networkLanguage.stationcount)
                        }
                        languageDao.deleteAll()
                        languageDao.insertAll(languageEntities)
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_languages", System.currentTimeMillis())
                        Log.d("StationRepository", "Languages cache refreshed from API.")
                    } else if (isCacheEmpty) {
                        SharedPreferencesManager.setLastUpdateTimestamp(context, "last_update_languages", 0L)
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch languages: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching languages: ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Languages cache is fresh, no refresh needed now.")
        }
    }
}
