package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Added
import android.widget.ImageView // Added
import android.widget.SeekBar // Added
import android.widget.TextView
import android.widget.Toast // Added
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.content.Context // Added
import android.media.AudioManager // Added
import androidx.lifecycle.Observer // Added
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.StationRepository
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.network.model.toDomain
import com.example.webradioapp.services.StreamingService
import com.example.webradioapp.viewmodels.FavoritesViewModel
import com.example.webradioapp.viewmodels.FavoritesViewModelFactory
import com.example.webradioapp.viewmodels.StationViewModelFactory
import kotlinx.coroutines.launch
import android.util.Log

class HomeFragment : Fragment() {

    // Popular Stations UI and data
    private lateinit var tvPopularStationsTitle: TextView
    private lateinit var rvPopularStations: RecyclerView
    private lateinit var popularStationsAdapter: StationAdapter
    private val apiService: RadioBrowserApiService by lazy { ApiClient.instance }
    private val stationViewModel: com.example.webradioapp.viewmodels.StationViewModel by viewModels {
        val application = requireActivity().application
        val db = AppDatabase.getDatabase(application)
        val favoriteDao = db.favoriteStationDao()
        val historyDao = db.historyStationDao()
        val countryDao = db.countryDao()
        val genreDao = db.genreDao()
        val languageDao = db.languageDao()
        val apiServ = ApiClient.instance // Renamed to avoid conflict with outer scope variable

        StationViewModelFactory(
            application,
            favoriteDao,
            historyDao,
            countryDao,
            genreDao,
            languageDao,
            apiServ
        )
    }
    private val favoritesViewModel: FavoritesViewModel by viewModels {
        val application = requireActivity().application
        val db = AppDatabase.getDatabase(application)
        val stationRepository = StationRepository(
            application,
            db.favoriteStationDao(),
            db.historyStationDao(),
            db.countryDao(),
            db.genreDao(),
            db.languageDao(),
            ApiClient.instance
        )
        FavoritesViewModelFactory(application, stationRepository)
    }
    private var currentPopularStations: List<RadioStation> = emptyList()
    private var currentFavoritesSet: Set<String> = emptySet() // This will be the single source for favorite IDs

    // History Stations UI and data
    private lateinit var rvHistoryStations: RecyclerView
    private lateinit var historyStationsAdapter: StationAdapter
    private var currentHistoryStations: List<RadioStation> = emptyList()
    private lateinit var tvHistoryStationsTitle: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Popular Stations RecyclerView and Title
        tvPopularStationsTitle = view.findViewById(R.id.tv_popular_stations_title)
        rvPopularStations = view.findViewById(R.id.recycler_view_popular_stations)

        // Initialize History Stations RecyclerView and Title
        rvHistoryStations = view.findViewById(R.id.recycler_view_history_stations)
        tvHistoryStationsTitle = view.findViewById(R.id.tv_history_stations_title)
        tvHistoryStationsTitle.visibility = View.GONE // Initially hide

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("HomeFragment", "onViewCreated called")
        super.onViewCreated(view, savedInstanceState)
        setupPopularStationsRecyclerView()
        setupHistoryStationsRecyclerView() // New setup call

        observeFavoriteChanges() // Combined observer

        loadPopularStations()
        loadHistoryStations() // New data loading call
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomeFragment", "onResume called")
        // Consider if a manual refresh trigger here would be a temporary workaround,
        // but the goal is to understand why initial load fails.
        // For now, just log.
    }

    override fun onPause() {
        super.onPause()
        Log.d("HomeFragment", "onPause called")
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
        Log.d("HomeFragment", "observeFavoriteChanges() called")
        favoritesViewModel.favoriteStations.observe(viewLifecycleOwner, Observer { favoriteStationsList ->
            val favs = favoriteStationsList ?: emptyList() // Ensure non-null list
            currentFavoritesSet = favs.map { station -> station.id }.toSet()
            Log.d("HomeFragment", "observeFavoriteChanges - LiveData observed, favorites size: ${favs.size}")

            // Update popular stations if their favorite status might have changed
            val updatedPopularStations = currentPopularStations.map { station ->
                station.copy(isFavorite = currentFavoritesSet.contains(station.id))
            }
            var popularHasChanged = popularStationsAdapter.currentList.size != updatedPopularStations.size
            if (!popularHasChanged) {
                for (i in popularStationsAdapter.currentList.indices) {
                    if (popularStationsAdapter.currentList[i].isFavorite != updatedPopularStations.getOrNull(i)?.isFavorite) {
                        popularHasChanged = true
                        break
                    }
                }
            }
            if (popularHasChanged) {
                currentPopularStations = updatedPopularStations // Keep local copy in sync
                popularStationsAdapter.submitList(updatedPopularStations.toList())
            }

            // Update history stations if their favorite status might have changed
            val updatedHistoryStations = currentHistoryStations.map { station ->
                station.copy(isFavorite = currentFavoritesSet.contains(station.id))
            }
            var historyHasChanged = historyStationsAdapter.currentList.size != updatedHistoryStations.size
            if (!historyHasChanged) {
                 for (i in historyStationsAdapter.currentList.indices) {
                    if (historyStationsAdapter.currentList[i].isFavorite != updatedHistoryStations.getOrNull(i)?.isFavorite) {
                        historyHasChanged = true
                        break
                    }
                }
            }
            if (historyHasChanged) {
                currentHistoryStations = updatedHistoryStations // Keep local copy in sync
                historyStationsAdapter.submitList(updatedHistoryStations.toList())
            }
        })
    }

    private fun loadPopularStations() {
        Log.d("HomeFragment", "loadPopularStations() called")
        // Optional: Show loading indicator here
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("HomeFragment", "loadPopularStations - coroutine started")
            try {
                // Using getStationsByVotes for "popular" as direct "popular" endpoint might not exist or behave as expected.
                // Alternatives: getStationsByClickCount, or a curated list if API doesn't provide a direct "popular" sort.
                // Changed to getTopStations as requested
                val response = apiService.getTopStations(limit = 20)
                if (response.isSuccessful) {
                    Log.d("HomeFragment", "loadPopularStations - API call successful, body size: ${response.body()?.size}")
                    val apiStations: List<com.example.webradioapp.network.model.ApiRadioStation> = response.body() ?: emptyList()
                    if (apiStations.isNotEmpty()) {
                        currentPopularStations = apiStations.mapNotNull { it.toDomain() }.map { station: RadioStation ->
                            station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                        }
                        Log.d("HomeFragment", "loadPopularStations - submitting list to adapter, size: ${currentPopularStations.size}")
                        popularStationsAdapter.submitList(currentPopularStations.toList())
                        rvPopularStations.visibility = View.VISIBLE // Ensure RV is visible
                        tvPopularStationsTitle.visibility = View.VISIBLE // Ensure title is visible
                    } else {
                        // Handle empty list if needed, maybe show a specific message or keep placeholder
                        rvPopularStations.visibility = View.GONE // Hide RV if no stations
                        tvPopularStationsTitle.visibility = View.GONE // Hide title if no stations
                    }
                } else {
                    Log.e("HomeFragment", "loadPopularStations - API call failed: ${response.message()}")
                    // Optional: Show error message here
                    rvPopularStations.visibility = View.GONE // Hide RV on error
                    tvPopularStationsTitle.visibility = View.GONE // Hide title on error
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "loadPopularStations - Exception: ${e.message}", e)
                // Optional: Show error message here
                rvPopularStations.visibility = View.GONE // Hide RV on exception
                tvPopularStationsTitle.visibility = View.GONE // Hide title on exception
            } finally {
                // Optional: Hide loading indicator here
            }
        }
    }

    private fun loadHistoryStations() {
        Log.d("HomeFragment", "loadHistoryStations() called")
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("HomeFragment", "loadHistoryStations - coroutine started, collecting flow")
            stationViewModel.recentlyPlayedStations.collect { stations: List<RadioStation> ->
                Log.d("HomeFragment", "loadHistoryStations - collected history, size: ${stations.size}")
                currentHistoryStations = stations.map { station: RadioStation ->
                    // Ensure favorite status is correctly mapped using the latest currentFavoritesSet
                    station.copy(isFavorite = currentFavoritesSet.contains(station.id))
                }
                Log.d("HomeFragment", "loadHistoryStations - submitting list to adapter, size: ${currentHistoryStations.size}")
                historyStationsAdapter.submitList(currentHistoryStations.toList())

                if (currentHistoryStations.isNotEmpty()) {
                    tvHistoryStationsTitle.visibility = View.VISIBLE
                    // Optionally hide main placeholder if other content is now visible
                    // This logic might need refinement based on overall content strategy
                } else {
                    tvHistoryStationsTitle.visibility = View.GONE
                }
            }
        }
    }
}
