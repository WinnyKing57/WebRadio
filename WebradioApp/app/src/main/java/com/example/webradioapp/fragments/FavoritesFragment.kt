package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import for by viewModels()
import androidx.lifecycle.Observer // Added
// import androidx.lifecycle.Lifecycle // Removed if repeatOnLifecycle is removed
// import androidx.lifecycle.lifecycleScope // Removed if launch is removed
// import androidx.lifecycle.repeatOnLifecycle // Removed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.db.AppDatabase
import com.example.webradioapp.db.StationRepository
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.network.ApiClient
import com.example.webradioapp.network.RadioBrowserApiService
import com.example.webradioapp.services.StreamingService
// Removed: import com.example.webradioapp.utils.SharedPreferencesManager
import com.example.webradioapp.viewmodels.FavoritesViewModel
import com.example.webradioapp.viewmodels.FavoritesViewModelFactory
// Removed: import com.example.webradioapp.viewmodels.StationViewModel
// import kotlinx.coroutines.launch // Removed if not used elsewhere

class FavoritesFragment : Fragment() {

    private lateinit var recyclerViewFavorites: RecyclerView
    // private lateinit var recyclerViewHistory: RecyclerView // Removed
    private lateinit var favoritesAdapter: StationAdapter
    // private lateinit var historyAdapter: StationAdapter // Removed
    // private lateinit var sharedPrefsManager: SharedPreferencesManager // Removed

    private val favoritesViewModel: com.example.webradioapp.viewmodels.FavoritesViewModel by viewModels {
        val application = requireActivity().application
        val db = AppDatabase.getDatabase(application)

        val apiClientInstance = ApiClient.instance
        val apiService: com.example.webradioapp.network.RadioBrowserApiService = apiClientInstance

        val stationRepository = com.example.webradioapp.db.StationRepository(
            application,
            db.favoriteStationDao(),
            db.historyStationDao(),
            db.countryDao(),
            db.genreDao(),
            db.languageDao(),
            apiService
        )

        FavoritesViewModelFactory(application, stationRepository)
    }
    // private val stationViewModel: StationViewModel by viewModels() // Removed as it's no longer used

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        // sharedPrefsManager = SharedPreferencesManager(requireContext()) // Removed

        recyclerViewFavorites = view.findViewById(R.id.recycler_view_favorites)
        // recyclerViewHistory = view.findViewById(R.id.recycler_view_history) // Removed

        setupRecyclerViews()
        observeViewModelData()

        return view
    }

    // onResume is not needed for loading data with Flow, it will be collected when view is started.
    // override fun onResume() {
    //     super.onResume()
    //     // loadFavorites() // Handled by Flow
    //     // loadHistory() // Handled by Flow
    // }

    private fun setupRecyclerViews() {
        // Favorites Adapter
        favoritesAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station -> playStation(station) },
            onFavoriteToggle = { station ->
                favoritesViewModel.removeFavorite(station) // Explicitly remove from favorites list
            }
        )
        recyclerViewFavorites.layoutManager = LinearLayoutManager(context)
        recyclerViewFavorites.adapter = favoritesAdapter

        // History Adapter section removed
    }

    private fun observeViewModelData() {
        favoritesViewModel.favoriteStations.observe(viewLifecycleOwner, Observer { favoriteStations ->
            // The 'favoriteStations' here is List<RadioStation> from LiveData
            favoritesAdapter.submitList(favoriteStations ?: emptyList()) // Handle potential null from LiveData
        })
        // Observation for stationHistory (if it were LiveData) would be similar:
        // favoritesViewModel.stationHistory.observe(viewLifecycleOwner, Observer { historyStations ->
        //     historyAdapter.submitList(historyStations ?: emptyList())
        // })
    }

    // private fun loadFavorites() { ... } // Removed, handled by Flow
    // private fun loadHistory() { ... } // Removed, handled by Flow

    private fun playStation(station: RadioStation) {
        // When playing from history, ensure its history is updated
        // This might be better handled in StreamingService after confirming playback starts
        // For now, assume StreamingService will call a method to update history.
        // favoritesViewModel.addStationToHistory(station) // Let StreamingService handle this call
        val serviceIntent = Intent(activity, StreamingService::class.java).apply {
            action = StreamingService.ACTION_PLAY
            putExtra(StreamingService.EXTRA_STREAM_URL, station.streamUrl)
            putExtra(StreamingService.EXTRA_STATION_OBJECT, station)
        }
        activity?.startService(serviceIntent)
        // Consider also refreshing history here or let StreamingService handle it fully
        // For now, StreamingService handles adding to history.
    }
}
