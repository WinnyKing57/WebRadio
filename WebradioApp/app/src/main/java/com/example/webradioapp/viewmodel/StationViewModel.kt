package com.example.webradioapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData // Added import
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.FavoriteStationDao
import com.example.webradioapp.db.HistoryStationDao
import com.example.webradioapp.model.RadioStation
import kotlinx.coroutines.launch

// A simple StationViewModel. In a real app, this would be more complex,
// potentially interacting with a remote API and a more sophisticated repository.
class StationViewModel(application: Application) : AndroidViewModel(application) {

    private val favoriteStationDao: FavoriteStationDao
    private val historyStationDao: HistoryStationDao
    private val stationRepository: com.example.webradioapp.db.StationRepository // Added
    // For simplicity, let's assume "allStations" for selection could come from favorites or history.
    // A more robust app would have a clearer source for "all selectable stations".
    val allStations: LiveData<List<RadioStation>> // This will be a combination or a specific source

    // Example: Combining favorites and history for selection.
    // This is a placeholder. A real app might have a dedicated table or API for all stations.
    private val _combinedStations = androidx.lifecycle.MediatorLiveData<List<RadioStation>>()
    val combinedStations: LiveData<List<RadioStation>> = _combinedStations


    init {
        val database = AppDatabase.getDatabase(application)
        favoriteStationDao = database.favoriteStationDao()
        historyStationDao = database.historyStationDao()
        stationRepository = com.example.webradioapp.db.StationRepository(favoriteStationDao, historyStationDao) // Added

        // Example: Populate allStations with favorites for now.
        // This should be replaced with a more robust way of getting selectable stations.
        // allStations = favoriteStationDao.getAllFavoriteStations() // Old line
        allStations = stationRepository.allPersistedStations.asLiveData() // Changed to use repository


        // --- Example of combining favorites and history ---
        // This is more complex and might not be what's needed for a simple "select any station"
        // For now, `allStations` above just points to favorites.
        /*
        val favoritesLiveData = favoriteStationDao.getAllFavoriteStations()
        val historyLiveData = historyStationDao.getHistoryStations() // Assuming this returns LiveData

        _combinedStations.addSource(favoritesLiveData) { favorites ->
            val history = historyLiveData.value ?: emptyList()
            _combinedStations.value = combineAndDeduplicate(favorites, history)
        }
        _combinedStations.addSource(historyLiveData) { history ->
            val favorites = favoritesLiveData.value ?: emptyList()
            _combinedStations.value = combineAndDeduplicate(favorites, history)
        }
        allStations = _combinedStations // Pointing to the combined list
        */
    }

    private fun combineAndDeduplicate(
        list1: List<RadioStation>?,
        list2: List<RadioStation>?
    ): List<RadioStation> {
        val combined = mutableListOf<RadioStation>()
        list1?.let { combined.addAll(it) }
        list2?.forEach { station ->
            if (!combined.any { it.id == station.id }) {
                combined.add(station)
            }
        }
        return combined.sortedBy { it.name }
    }

    // Add other station-related methods if needed, e.g., for fetching from a remote API
    fun refreshStations() {
        // Placeholder for logic to refresh station list from a remote source
        // For now, it does nothing as we are relying on DB
        viewModelScope.launch {
            // Example: apiService.getAllStations()...
        }
    }
}
