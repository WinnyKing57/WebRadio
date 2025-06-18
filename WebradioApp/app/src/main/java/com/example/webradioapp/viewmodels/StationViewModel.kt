package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.StationRepository // Import Repository
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient // Assuming ApiClient provides radioBrowserApiService
import kotlinx.coroutines.flow.Flow
// Removed: MutableStateFlow and asStateFlow for _currentStationFavoriteStatus
import kotlinx.coroutines.launch

class StationViewModel(
    application: Application,
    private val repository: StationRepository // Injected
) : AndroidViewModel(application) {

    // Flow for recently played stations
    val recentlyPlayedStations: Flow<List<RadioStation>>
        get() = repository.getRecentlyPlayedStations(limit = 10)


    // Removed: _currentStationFavoriteStatus and currentStationFavoriteStatus
    // UI should observe getStationFavoriteFlow or the main list from FavoritesViewModel

    // init block removed as repository is now injected

    fun toggleFavoriteStatus(station: RadioStation) { // This method was in user feedback for MainActivity
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
