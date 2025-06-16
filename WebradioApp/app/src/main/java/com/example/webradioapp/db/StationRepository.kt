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

/**
 * Repository class for accessing and managing station data from the Room database.
 * It abstracts the data source (DAOs) from the ViewModels.
 *
 * @property favoriteStationDao DAO for favorite station operations.
 * @property historyStationDao DAO for history station operations.
 */
class StationRepository(
    private val favoriteStationDao: FavoriteStationDao,
    private val historyStationDao: HistoryStationDao,
    private val countryDao: CountryDao,
    private val genreDao: GenreDao,
    private val languageDao: LanguageDao,
    private val apiService: RadioBrowserApiService
) {

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
        // Simple strategy: Refresh if cache is empty.
        // A more advanced strategy could use timestamps or periodic refresh.
        if (countryDao.getAll().first().isEmpty()) {
            try {
                val response = apiService.getCountries()
                if (response.isSuccessful) {
                    val countriesFromApi = response.body() ?: emptyList()
                    if (countriesFromApi.isNotEmpty()) {
                        val countryEntities = countriesFromApi.map { networkCountry ->
                            CountryEntity(name = networkCountry.name, stationCount = networkCountry.stationcount)
                        }
                        countryDao.deleteAll() // Clear old cache
                        countryDao.insertAll(countryEntities)
                        Log.d("StationRepository", "Countries cache refreshed from API.")
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch countries: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching countries: ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Countries cache is not empty, no refresh needed now.")
        }
    }

    // Genre operations
    fun getGenres(): Flow<List<GenreEntity>> {
        return genreDao.getAll()
    }

    suspend fun refreshGenres() {
        if (genreDao.getAll().first().isEmpty()) {
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
                        Log.d("StationRepository", "Genres cache refreshed from API.")
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch genres (tags): ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching genres (tags): ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Genres cache is not empty, no refresh needed now.")
        }
    }

    // Language operations
    fun getLanguages(): Flow<List<LanguageEntity>> {
        return languageDao.getAll()
    }

    suspend fun refreshLanguages() {
        if (languageDao.getAll().first().isEmpty()) {
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
                        Log.d("StationRepository", "Languages cache refreshed from API.")
                    }
                } else {
                    Log.e("StationRepository", "Failed to fetch languages: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("StationRepository", "Exception when fetching languages: ${e.message}", e)
            }
        } else {
            Log.d("StationRepository", "Languages cache is not empty, no refresh needed now.")
        }
    }
}
