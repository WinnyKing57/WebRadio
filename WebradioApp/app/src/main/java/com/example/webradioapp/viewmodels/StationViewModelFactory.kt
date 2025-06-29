package com.example.webradioapp.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.webradioapp.db.StationRepository // Ensure this import is present

class StationViewModelFactory(
    private val application: Application,
    private val stationRepository: StationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // This now assumes StationViewModel's constructor will be (Application, StationRepository)
            return StationViewModel(application, stationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
