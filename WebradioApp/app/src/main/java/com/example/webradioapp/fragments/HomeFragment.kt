package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import com.example.webradioapp.viewmodels.FavoritesViewModel
import com.example.webradioapp.viewmodels.StationViewModel
import kotlinx.coroutines.launch
import android.util.Log

class HomeFragment : Fragment() {

    private lateinit var etStreamUrl: EditText
    private lateinit var btnPlay: Button
    private lateinit var btnPause: Button
    private lateinit var tvHomePlaceholder: TextView // Added for placeholder management

    // Popular Stations UI and data
    private lateinit var rvPopularStations: RecyclerView
    private lateinit var popularStationsAdapter: StationAdapter
    private val apiService: RadioBrowserApiService by lazy { ApiClient.instance }
    private val stationViewModel: StationViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private var currentPopularStations: List<RadioStation> = emptyList()
    private var currentFavoritesSet: Set<String> = emptySet() // This will be the single source for favorite IDs

    // History Stations UI and data
    private lateinit var rvHistoryStations: RecyclerView
    private lateinit var historyStationsAdapter: StationAdapter
    private var currentHistoryStations: List<RadioStation> = emptyList()
    private lateinit var tvHistoryStationsTitle: TextView


    // A sample stream URL for quick testing
    private val defaultStreamUrl = "https://stream.radioparadise.com/flac"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        etStreamUrl = view.findViewById(R.id.et_stream_url)
        btnPlay = view.findViewById(R.id.btn_play)
        btnPause = view.findViewById(R.id.btn_pause)
        tvHomePlaceholder = view.findViewById(R.id.tv_home_placeholder) // Initialize placeholder

        // Initialize Popular Stations RecyclerView
        rvPopularStations = view.findViewById(R.id.recycler_view_popular_stations)

        // Initialize History Stations RecyclerView and Title
        rvHistoryStations = view.findViewById(R.id.recycler_view_history_stations)
        tvHistoryStationsTitle = view.findViewById(R.id.tv_history_stations_title)
        tvHistoryStationsTitle.visibility = View.GONE // Initially hide

        etStreamUrl.setText(defaultStreamUrl)

        btnPlay.setOnClickListener {
            val streamUrl = etStreamUrl.text.toString()
            if (streamUrl.isNotEmpty()) {
                // For direct URL play, we don't have a full RadioStation object.
                // Consider creating a temporary one or enhancing StreamingService.
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, streamUrl)
                    // Optionally, pass a basic RadioStation object if needed by the service for history/now playing
                    val tempStation = RadioStation(id = streamUrl, name = "Direct Stream", streamUrl = streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, tempStation)
                }
                activity?.startService(serviceIntent)
                // If you want to add direct streams to history via ViewModel, do it here
                // stationViewModel.addStationToHistory(RadioStation(id = streamUrl, name = "Direct Stream", streamUrl = streamUrl))
            }
        }

        btnPause.setOnClickListener {
            val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                action = StreamingService.ACTION_PAUSE
            }
            activity?.startService(serviceIntent)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPopularStationsRecyclerView()
        setupHistoryStationsRecyclerView() // New setup call

        observeFavoriteChanges() // Combined observer

        loadPopularStations()
        loadHistoryStations() // New data loading call
    }

    private fun setupPopularStationsRecyclerView() {
        popularStationsAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station ->
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                activity?.startService(serviceIntent)
                stationViewModel.addStationToHistory(station)
            },
            onFavoriteToggle = { station ->
                stationViewModel.toggleFavoriteStatus(station)
            }
        )
        rvPopularStations.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) // Horizontal scroll
        rvPopularStations.adapter = popularStationsAdapter
    }

    private fun setupHistoryStationsRecyclerView() {
        historyStationsAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station ->
                val serviceIntent = Intent(activity, StreamingService::class.java).apply {
                    action = StreamingService.ACTION_PLAY
                    putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
                    putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
                }
                activity?.startService(serviceIntent)
                stationViewModel.addStationToHistory(station) // Ensure this updates timestamp for history
            },
            onFavoriteToggle = { station ->
                stationViewModel.toggleFavoriteStatus(station)
            }
        )
        rvHistoryStations.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) // Horizontal scroll
        rvHistoryStations.adapter = historyStationsAdapter
    }

    // Combined observer for favorite changes
    private fun observeFavoriteChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            favoritesViewModel.favoriteStations.collect { favoriteStations ->
                currentFavoritesSet = favoriteStations.map { it.id }.toSet()

                // Update popular stations
                currentPopularStations = currentPopularStations.map { station ->
                    station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                }
                popularStationsAdapter.submitList(currentPopularStations.toList())

                // Update history stations
                currentHistoryStations = currentHistoryStations.map { station ->
                    station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                }
                historyStationsAdapter.submitList(currentHistoryStations.toList())
            }
        }
    }

    private fun loadPopularStations() {
        // Optional: Show loading indicator here
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Using getStationsByVotes for "popular" as direct "popular" endpoint might not exist or behave as expected.
                // Alternatives: getStationsByClickCount, or a curated list if API doesn't provide a direct "popular" sort.
                // Changed to getTopStations as requested
                val response = apiService.getTopStations(limit = 20)
                if (response.isSuccessful) {
                    val apiStations: List<com.example.webradioapp.network.model.ApiRadioStation> = response.body() ?: emptyList()
                    if (apiStations.isNotEmpty()) {
                        currentPopularStations = apiStations.mapNotNull { it.toDomain() }.map { station: RadioStation ->
                            station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                        }
                        popularStationsAdapter.submitList(currentPopularStations.toList())
                        tvHomePlaceholder.visibility = View.GONE // Hide placeholder if stations are loaded
                    } else {
                        // Handle empty list if needed, maybe show a specific message or keep placeholder
                         tvHomePlaceholder.text = "No popular stations available at the moment."
                         tvHomePlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("HomeFragment", "Failed to load popular stations: ${response.message()}")
                    // Optional: Show error message here
                     tvHomePlaceholder.text = "Could not load popular stations."
                     tvHomePlaceholder.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading popular stations", e)
                // Optional: Show error message here
                tvHomePlaceholder.text = "Error loading popular stations."
                tvHomePlaceholder.visibility = View.VISIBLE
            } finally {
                // Optional: Hide loading indicator here
            }
        }
    }

    private fun loadHistoryStations() {
        viewLifecycleOwner.lifecycleScope.launch {
            stationViewModel.recentlyPlayedStations.collect { stations: List<RadioStation> ->
                currentHistoryStations = stations.map { station: RadioStation ->
                    // Ensure favorite status is correctly mapped using the latest currentFavoritesSet
                    station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                }
                historyStationsAdapter.submitList(currentHistoryStations.toList())

                if (currentHistoryStations.isNotEmpty()) {
                    tvHistoryStationsTitle.visibility = View.VISIBLE
                    // Optionally hide main placeholder if other content is now visible
                    // This logic might need refinement based on overall content strategy
                    if (tvHomePlaceholder.visibility == View.VISIBLE && currentPopularStations.isNotEmpty()) {
                         // If popular stations are also loaded, main placeholder can definitely go.
                         // Or if history alone is enough to hide the main "Welcome" type message.
                        // For now, let's assume loading any list section is enough to hide the main placeholder.
                        // This was already done in loadPopularStations, so it might be redundant here
                        // unless popular stations failed but history succeeded.
                    }
                } else {
                    tvHistoryStationsTitle.visibility = View.GONE
                }
            }
        }
    }
}
