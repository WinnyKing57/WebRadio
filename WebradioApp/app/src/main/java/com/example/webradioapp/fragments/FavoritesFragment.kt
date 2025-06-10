package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // Import for by viewModels()
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.services.StreamingService
// Removed: import com.example.webradioapp.utils.SharedPreferencesManager
import com.example.webradioapp.viewmodels.FavoritesViewModel
import com.example.webradioapp.viewmodels.StationViewModel // For toggling favorite from history list
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment() {

    private lateinit var recyclerViewFavorites: RecyclerView
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var favoritesAdapter: StationAdapter
    private lateinit var historyAdapter: StationAdapter
    // private lateinit var sharedPrefsManager: SharedPreferencesManager // Removed

    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private val stationViewModel: StationViewModel by viewModels() // For interaction from history items

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        // sharedPrefsManager = SharedPreferencesManager(requireContext()) // Removed

        recyclerViewFavorites = view.findViewById(R.id.recycler_view_favorites)
        recyclerViewHistory = view.findViewById(R.id.recycler_view_history)

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

        // History Adapter
        historyAdapter = StationAdapter(
            requireContext(),
            onPlayClicked = { station -> playStation(station) },
            onFavoriteToggle = { station ->
                // Use stationViewModel to toggle favorite status, which updates DB
                // The station object from adapter already has its current isFavorite state
                stationViewModel.toggleFavoriteStatus(station)
            }
        )
        recyclerViewHistory.layoutManager = LinearLayoutManager(context)
        recyclerViewHistory.adapter = historyAdapter
    }

    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    favoritesViewModel.favoriteStations.collect { stations ->
                        favoritesAdapter.submitList(stations)
                    }
                }
                launch {
                    favoritesViewModel.stationHistory.collect { stations ->
                        historyAdapter.submitList(stations)
                    }
                }
            }
        }
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
