package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.webradioapp.db.StationRepository

class StationViewModelFactory(
    private val application: Application,
    private val stationRepository: StationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StationViewModel(application, stationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
