package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.StationRepository // Import Repository
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow
// Removed: MutableStateFlow and asStateFlow for _currentStationFavoriteStatus
import kotlinx.coroutines.launch

class StationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StationRepository

    // Flow for recently played stations
    val recentlyPlayedStations: Flow<List<RadioStation>> = repository.getRecentlyPlayedStations(limit = 10) // Example limit


    // Removed: _currentStationFavoriteStatus and currentStationFavoriteStatus
    // UI should observe getStationFavoriteFlow or the main list from FavoritesViewModel

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StationRepository(database.favoriteStationDao(), database.historyStationDao())
    }

    fun toggleFavoriteStatus(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavoriteStatus(station)
            // No need to manually update _currentStationFavoriteStatus here.
            // The change in DB will be picked up by Flows observed by UI.
        }
    }

    // This suspend function might be less useful if UI is observing Flows.
    // Keeping it if direct check is needed somewhere not suitable for Flow.
    suspend fun isFavorite(stationId: String): Boolean {
        return repository.isFavorite(stationId)
    }

    // Provides a Flow to observe a single station's details, including its isFavorite status
    fun getStationFlow(stationId: String): Flow<RadioStation?> {
        return repository.getStationById(stationId)
    }

    // Method to add to history, to be called from StreamingService or via this ViewModel if appropriate
    fun addStationToHistory(station: RadioStation) {
        viewModelScope.launch {
            repository.addStationToHistory(station)
        }
    }
}
