package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.network.model.toDomain
import com.example.webradioapp.services.StreamingService
// Removed: import com.example.webradioapp.utils.SampleData (no longer using sample data)
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class SearchFragment : Fragment() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerViewStations: RecyclerView
    private lateinit var stationAdapter: StationAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var etSearchCountry: TextInputEditText
    private lateinit var etSearchCategory: TextInputEditText

    private val apiService: RadioBrowserApiService by lazy { ApiClient.instance }
    private val stationViewModel: com.example.webradioapp.viewmodels.StationViewModel by viewModels()
    private val favoritesViewModel: com.example.webradioapp.viewmodels.FavoritesViewModel by viewModels() // To observe all favorites
    private var searchJob: Job? = null
    private var currentStationList: List<RadioStation> = emptyList()
    private var currentFavoritesSet: Set<String> = emptySet()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        searchView = view.findViewById(R.id.search_view)
        recyclerViewStations = view.findViewById(R.id.recycler_view_stations)
        progressBar = view.findViewById(R.id.progress_bar_search)
        tvError = view.findViewById(R.id.tv_search_error)
        etSearchCountry = view.findViewById(R.id.et_search_country)
        etSearchCategory = view.findViewById(R.id.et_search_category)

        setupRecyclerView()
        setupSearchView()
        observeFavoriteChanges()


        showEmptyState("Search for radio stations above.")

        return view
    }

    private fun setupRecyclerView() {
        stationAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station ->
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                activity?.startService(serviceIntent)
                // Also add to history via ViewModel
                stationViewModel.addStationToHistory(station)
            },
            onFavoriteToggle = { station ->
                stationViewModel.toggleFavoriteStatus(station)
                // The observer for favoriteStationsFlow should ideally update the list
                // and re-submit, or we need to manually update the item's state.
                // For now, the adapter's item will be rebound if list is resubmitted.
            }
        )
        recyclerViewStations.layoutManager = LinearLayoutManager(context)
        recyclerViewStations.adapter = stationAdapter
    }

    private fun observeFavoriteChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            favoritesViewModel.favoriteStations.collect { favoriteStations ->
                val favoriteIds = favoriteStations.map { it.id }.toSet()
                currentFavoritesSet = favoriteIds // Ajouter cette ligne

                // La logique suivante pour mettre à jour currentStationList et l'adapter est déjà là
                // et devrait utiliser 'favoriteIds' (qui est la même chose que currentFavoritesSet à ce point)
                // On peut la laisser telle quelle ou la faire utiliser currentFavoritesSet pour la cohérence
                // Pour l'instant, laissons-la utiliser 'favoriteIds' car c'est local à ce scope.
                // La principale utilité de currentFavoritesSet est pour performSearch.
                currentStationList = currentStationList.map { station ->
                    station.copy(isFavorite = favoriteIds.contains(station.id))
                }
                stationAdapter.submitList(currentStationList.toList())
            }
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchJob?.cancel() // Cancel previous job
                performSearch(query)
                searchView.clearFocus() // Hide keyboard
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel() // Cancel previous job
                // Debounce search: wait for user to stop typing for a bit
                if (!newText.isNullOrBlank() && newText.length >= 2) { // Minimum characters to search
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        delay(500) // Debounce delay
                        performSearch(newText)
                    }
                } else if (newText.isNullOrBlank()){
                    // Clear results if search text is empty
                     stationAdapter.submitList(emptyList())
                     currentStationList = emptyList()
                     showEmptyState("Search for radio stations above.")
                }
                return true
            }
        })
    }

    private fun performSearch(query: String?) { // query is the text from the main SearchView
        showLoading(true)
        tvError.visibility = View.GONE

        // Get values from the new fields
        val countryQuery = etSearchCountry.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val categoryQuery = etSearchCategory.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val nameQuery = query?.trim()?.takeIf { it.isNotEmpty() } // from existing search view

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Log the parameters being sent
                Log.d("SearchFragment", "Performing search with Name: $nameQuery, Country: $countryQuery, Category: $categoryQuery")

                val response = apiService.searchStations(
                    name = nameQuery,       // Use the processed nameQuery
                    country = countryQuery, // Pass the country query
                    tag = categoryQuery,    // Pass the category query (tag)
                    limit = 50,
                    hideBroken = true // Keep filtering out broken stations
                )
                if (response.isSuccessful) {
                    val apiStations = response.body() ?: emptyList()
                    // Map to domain and then merge favorite status
                    val favoriteIdsToUse = currentFavoritesSet

                    currentStationList = apiStations.mapNotNull { it.toDomain() }.map { station ->
                        station.copy(isFavorite = favoriteIdsToUse.contains(station.id))
                    }

                    if (currentStationList.isNotEmpty()) {
                        stationAdapter.submitList(currentStationList.toList()) // Submit new list
                        recyclerViewStations.visibility = View.VISIBLE
                        tvError.visibility = View.GONE
                    } else {
                        stationAdapter.submitList(emptyList())
                        // Construct a more informative message
                        val searchTerms = listOfNotNull(nameQuery, countryQuery, categoryQuery).joinToString(", ")
                        showError("No stations found for '$searchTerms'.")
                    }
                } else {
                    Log.e("SearchFragment", "API Error: ${response.code()} - ${response.message()}")
                    // Construct a more informative message
                    val searchTerms = listOfNotNull(nameQuery, countryQuery, categoryQuery).joinToString(", ")
                    showError("Error fetching stations for '$searchTerms': ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("SearchFragment", "Network/Conversion Error: ${e.message}", e)
                showError("Network error: ${e.localizedMessage}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        showLoading(false)
        recyclerViewStations.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
        // Toast.makeText(context, message, Toast.LENGTH_LONG).show() // Alternative or additional feedback
    }

    private fun showEmptyState(message: String) {
        showLoading(false)
        recyclerViewStations.visibility = View.GONE
        tvError.visibility = View.VISIBLE
        tvError.text = message
    }

    override fun onDestroyView() {
        searchJob?.cancel() // Cancel any running search when view is destroyed
        super.onDestroyView()
    }
}
