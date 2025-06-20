package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.webradioapp.db.FavoriteStationDao
import com.example.webradioapp.db.HistoryStationDao
import com.example.webradioapp.db.dao.CountryDao
import com.example.webradioapp.db.dao.GenreDao
import com.example.webradioapp.db.dao.LanguageDao
import com.example.webradioapp.network.RadioBrowserApiService

class StationViewModelFactory(
    private val application: Application,
    private val favoriteStationDao: FavoriteStationDao,
    private val historyStationDao: HistoryStationDao,
    private val countryDao: CountryDao,
    private val genreDao: GenreDao,
    private val languageDao: LanguageDao,
    private val apiService: RadioBrowserApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StationViewModel(
                application,
                favoriteStationDao,
                historyStationDao,
                countryDao,
                genreDao,
                languageDao,
                apiService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
