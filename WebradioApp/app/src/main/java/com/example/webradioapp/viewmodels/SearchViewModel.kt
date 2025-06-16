package com.example.webradioapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.webradioapp.db.StationRepository
import com.example.webradioapp.db.entities.CountryEntity
import com.example.webradioapp.db.entities.GenreEntity
import com.example.webradioapp.db.entities.LanguageEntity
import com.example.webradioapp.model.RadioStation // Assuming this is your domain model for stations
import com.example.webradioapp.network.RadioBrowserApiService // Needed for search
import com.example.webradioapp.network.model.toDomain // For mapping ApiRadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import android.util.Log

// Assuming StationRepository needs Application context, use AndroidViewModel
// For simplicity now, assuming Repository is provided through DI or a factory that handles context
// If using Hilt, @HiltViewModel and @Inject constructor would be used.
class SearchViewModel(
    private val repository: StationRepository,
    private val apiService: RadioBrowserApiService // For direct search, or move search to repo too
) : ViewModel() {

    // StateFlow for Countries
    private val _countries = MutableStateFlow<List<CountryEntity>>(emptyList())
    val countries: StateFlow<List<CountryEntity>> = _countries.asStateFlow()

    // StateFlow for Genres
    private val _genres = MutableStateFlow<List<GenreEntity>>(emptyList())
    val genres: StateFlow<List<GenreEntity>> = _genres.asStateFlow()

    // StateFlow for Languages
    private val _languages = MutableStateFlow<List<LanguageEntity>>(emptyList())
    val languages: StateFlow<List<LanguageEntity>> = _languages.asStateFlow()

    // StateFlow for Search Results
    private val _searchResults = MutableStateFlow<List<RadioStation>>(emptyList())
    val searchResults: StateFlow<List<RadioStation>> = _searchResults.asStateFlow()

    private val _isLoadingFilters = MutableStateFlow(false)
    val isLoadingFilters: StateFlow<Boolean> = _isLoadingFilters.asStateFlow()

    private val _isLoadingSearch = MutableStateFlow(false)
    val isLoadingSearch: StateFlow<Boolean> = _isLoadingSearch.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()


    init {
        loadFilters()
    }

    fun loadFilters() {
        viewModelScope.launch {
            _isLoadingFilters.value = true
            try {
                // Refresh filter data from API (if cache is empty, as per current repo logic)
                repository.refreshCountries()
                repository.refreshGenres()
                repository.refreshLanguages()
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error refreshing filters", e)
                _errorMessage.value = "Error loading filters: ${e.localizedMessage}"
            } finally {
                // This doesn't mean loading is done, just refresh attempt.
                // Collection below will update the UI.
            }
        }

        viewModelScope.launch {
            repository.getCountries()
                .catch { e ->
                    Log.e("SearchViewModel", "Error collecting countries", e)
                    _errorMessage.value = "Error getting countries: ${e.localizedMessage}"
                }
                .collect { countryList -> _countries.value = countryList }
        }
        viewModelScope.launch {
            repository.getGenres()
                .catch { e ->
                    Log.e("SearchViewModel", "Error collecting genres", e)
                    _errorMessage.value = "Error getting genres: ${e.localizedMessage}"
                }
                .collect { genreList -> _genres.value = genreList }
        }
        viewModelScope.launch {
            repository.getLanguages()
                .catch { e ->
                    Log.e("SearchViewModel", "Error collecting languages", e)
                    _errorMessage.value = "Error getting languages: ${e.localizedMessage}"
                }
                .collect { languageList ->
                    _languages.value = languageList
                    _isLoadingFilters.value = false // Consider all filters loaded when last one is collected
                }
        }
    }

    fun searchStations(name: String?, countryCode: String?, genre: String?, language: String?) {
        viewModelScope.launch {
            _isLoadingSearch.value = true
            _errorMessage.value = null
            _searchResults.value = emptyList() // Clear previous results
            try {
                val response = apiService.searchStations(
                    name = name?.takeIf { it.isNotBlank() },
                    country = countryCode?.takeIf { it.isNotBlank() },
                    tag = genre?.takeIf { it.isNotBlank() },
                    language = language?.takeIf { it.isNotBlank() },
                    limit = 100, // Increased limit for better search
                    hideBroken = true
                )
                if (response.isSuccessful) {
                    val apiStations = response.body() ?: emptyList()
                    _searchResults.value = apiStations.mapNotNull { it.toDomain() }
                } else {
                    _errorMessage.value = "Search failed: ${response.message()}"
                    Log.e("SearchViewModel", "Search API error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search exception: ${e.localizedMessage}"
                Log.e("SearchViewModel", "Search exception", e)
            } finally {
                _isLoadingSearch.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
