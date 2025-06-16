package com.example.webradioapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.webradioapp.db.StationRepository
import com.example.webradioapp.network.RadioBrowserApiService

class SearchViewModelFactory(
    private val repository: StationRepository,
    private val apiService: RadioBrowserApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(repository, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
