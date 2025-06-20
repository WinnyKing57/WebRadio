package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.FavoriteStationDao
import com.example.webradioapp.db.HistoryStationDao
import com.example.webradioapp.db.StationRepository
import com.example.webradioapp.db.dao.CountryDao
import com.example.webradioapp.db.dao.GenreDao
import com.example.webradioapp.db.dao.LanguageDao
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.RadioBrowserApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class StationViewModel(
    application: Application,
    favoriteStationDao: FavoriteStationDao,
    historyStationDao: HistoryStationDao,
    countryDao: CountryDao,
    genreDao: GenreDao,
    languageDao: LanguageDao,
    apiService: RadioBrowserApiService
) : AndroidViewModel(application) {

    private val repository: StationRepository

    init {
        repository = StationRepository(
            application.applicationContext,
            favoriteStationDao,
            historyStationDao,
            countryDao,
            genreDao,
            languageDao,
            apiService
        )
    }

    // Flow for recently played stations
    val recentlyPlayedStations: Flow<List<RadioStation>>
        get() = repository.getRecentlyPlayedStations(limit = 10)


    fun toggleFavoriteStatus(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavoriteStatus(station)
        }
    }

    suspend fun isFavorite(stationId: String): Boolean {
        return repository.isFavorite(stationId)
    }

    fun getStationFlow(stationId: String): Flow<RadioStation?> {
        return repository.getStationById(stationId)
    }

    fun addStationToHistory(station: RadioStation) {
        viewModelScope.launch {
            repository.addStationToHistory(station)
        }
    }
}
