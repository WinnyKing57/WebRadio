package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.StationRepository // Ensure this import is present
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FavoritesViewModel(
    application: Application,
    private val repository: StationRepository // Changed from stationRepository to repository for consistency if needed, or keep as stationRepository
) : AndroidViewModel(application) {

    val favoriteStations: LiveData<List<RadioStation>> = repository.favoriteStations.asLiveData()

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavoriteStatus(station)
        }
    }

    fun removeFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.removeFavorite(station.id)
        }
    }

    fun isStationFavorite(stationId: String): Flow<RadioStation?> {
        return repository.getStationById(stationId)
    }

    // Ensure all other methods use `this.repository`
}
