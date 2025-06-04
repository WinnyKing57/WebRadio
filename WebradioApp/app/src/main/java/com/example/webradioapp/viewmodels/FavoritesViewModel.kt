package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.StationRepository // Import Repository
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn // Import for stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StationRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StationRepository(database.favoriteStationDao(), database.historyStationDao())
    }

    val favoriteStations: Flow<List<RadioStation>> = repository.favoriteStations
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList()) // Convert to StateFlow or share
    val stationHistory: Flow<List<RadioStation>> = repository.stationHistory
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Combined toggle logic using repository
    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavoriteStatus(station)
        }
    }

    // Specifically to remove a favorite (e.g., from favorites list)
    fun removeFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.removeFavorite(station.id)
        }
    }

    // addStationToHistory is primarily a write operation, better handled by something
    // directly used by StreamingService (like the repository itself, or a dedicated PlayerViewModel).
    // We expose the Flow for observing history here.

     fun isStationFavorite(stationId: String): Flow<RadioStation?> { // For observing single station status
        return repository.getStationById(stationId)
    }
}
