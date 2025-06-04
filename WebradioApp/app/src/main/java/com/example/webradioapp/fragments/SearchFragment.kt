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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
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

    private val apiService: RadioBrowserApiService by lazy { ApiClient.instance }
    private var searchJob: Job? = null

    // private val allStations: List<RadioStation> = SampleData.stations // Removed SampleData

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        searchView = view.findViewById(R.id.search_view)
        recyclerViewStations = view.findViewById(R.id.recycler_view_stations)
        progressBar = view.findViewById(R.id.progress_bar_search)
        tvError = view.findViewById(R.id.tv_search_error)

        setupRecyclerView()
        setupSearchView()

        // Load initial data or popular stations? For now, search will trigger loading.
        // performSearch(null) // Optionally perform an initial empty search for popular stations
        showEmptyState("Search for radio stations above.")

        return view
    }

    private fun setupRecyclerView() {
        stationAdapter = StationAdapter(
            requireContext(),
            emptyList(),
            onPlayClicked = { station ->
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                activity?.startService(serviceIntent)
            },
            onFavoriteToggle = { _, _ ->
                // Favorite toggle might require re-fetching or just updating the item in adapter
                // For simplicity, current adapter handles UI, SharedPreferencesManager handles storage.
            }
        )
        recyclerViewStations.layoutManager = LinearLayoutManager(context)
        recyclerViewStations.adapter = stationAdapter
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
                     stationAdapter.updateStations(emptyList())
                     showEmptyState("Search for radio stations above.")
                }
                return true
            }
        })
    }

    private fun performSearch(query: String?) {
        showLoading(true)
        tvError.visibility = View.GONE
        recyclerViewStations.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.searchStations(
                    name = if (query.isNullOrBlank()) null else query, // Search by name
                    // tag = if (query.isNullOrBlank()) "top" else null, // Example: search by tag "top" if query is blank
                    limit = 50 // Limit results for now
                )
                if (response.isSuccessful) {
                    val apiStations = response.body() ?: emptyList()
                    val domainStations = apiStations.mapNotNull { it.toDomain() } // mapNotNull filters out nulls from toDomain()

                    if (domainStations.isNotEmpty()) {
                        stationAdapter.updateStations(domainStations)
                        showLoading(false)
                        recyclerViewStations.visibility = View.VISIBLE
                    } else {
                        showError("No stations found for '$query'.")
                    }
                } else {
                    Log.e("SearchFragment", "API Error: ${response.code()} - ${response.message()}")
                    showError("Error fetching stations: ${response.message()}")
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
