package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
// Remove direct DAO imports if they are no longer used directly by StationViewModel
// import com.example.webradioapp.db.FavoriteStationDao
// import com.example.webradioapp.db.HistoryStationDao
// import com.example.webradioapp.db.dao.CountryDao
// import com.example.webradioapp.db.dao.GenreDao
// import com.example.webradioapp.db.dao.LanguageDao
// import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.db.StationRepository // Ensure this import is present
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.flow.Flow // For isStationFavorite return type
import kotlinx.coroutines.launch

class StationViewModel(
    application: Application,
    private val stationRepository: StationRepository // Injected
) : AndroidViewModel(application) {

    // The stationRepository is now injected, so no need for an init block
    // to create it from individual DAOs.

    // LiveData for stations, now fetched via the injected repository
    val allStations: LiveData<List<RadioStation>> = stationRepository.allPersistedStations.asLiveData()
    val favoriteStations: LiveData<List<RadioStation>> = stationRepository.favoriteStations.asLiveData() // If needed

    // Example: Exposing recently played stations from the repository
    val recentlyPlayedStations: Flow<List<RadioStation>> = stationRepository.getRecentlyPlayedStations(20) // Default limit 20


    // Methods that delegate to the repository
    fun addStationToHistory(station: RadioStation) {
        viewModelScope.launch {
            stationRepository.addStationToHistory(station)
        }
    }

    fun toggleFavoriteStatus(station: RadioStation) {
        viewModelScope.launch {
            stationRepository.toggleFavoriteStatus(station)
        }
    }

    fun isStationFavorite(stationId: String): Flow<RadioStation?> { // Assuming getStationById returns Flow<RadioStation?>
        return stationRepository.getStationById(stationId)
    }

    fun refreshCountries() {
        viewModelScope.launch {
            stationRepository.refreshCountries()
        }
    }

    fun refreshGenres() {
        viewModelScope.launch {
            stationRepository.refreshGenres()
        }
    }

    fun refreshLanguages() {
        viewModelScope.launch {
            stationRepository.refreshLanguages()
        }
    }

    // Add any other methods that StationViewModel previously had,
    // ensuring they now use the injected stationRepository.
    // For example, if there was a method to fetch stations from network:
    // fun fetchStationsFromNetwork() {
    //     viewModelScope.launch {
    //         // This would depend on how StationRepository exposes network fetching
    //         // e.g., stationRepository.fetchAndCacheAllStations()
    //     }
    // }
}
