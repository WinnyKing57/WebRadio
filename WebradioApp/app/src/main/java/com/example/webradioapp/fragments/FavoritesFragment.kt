package com.example.webradioapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.services.StreamingService
import com.example.webradioapp.utils.SharedPreferencesManager

class FavoritesFragment : Fragment() {

    private lateinit var recyclerViewFavorites: RecyclerView
    private lateinit var recyclerViewHistory: RecyclerView
    private lateinit var favoritesAdapter: StationAdapter
    private lateinit var historyAdapter: StationAdapter
    private lateinit var sharedPrefsManager: SharedPreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        sharedPrefsManager = SharedPreferencesManager(requireContext())

        recyclerViewFavorites = view.findViewById(R.id.recycler_view_favorites)
        recyclerViewHistory = view.findViewById(R.id.recycler_view_history)

        setupRecyclerViews()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
        loadHistory()
    }

    private fun setupRecyclerViews() {
        // Favorites Adapter
        favoritesAdapter = StationAdapter(
            requireContext(),
            emptyList(),
            onPlayClicked = { station -> playStation(station) },
            onFavoriteToggle = { station, isFavorite ->
                if (!isFavorite) { // Station was removed from favorites
                    sharedPrefsManager.removeFavorite(station.id) // Ensure it's removed
                    loadFavorites() // Refresh the list
                } else {
                    sharedPrefsManager.addFavorite(station) // Ensure it's added (should be redundant if logic is correct)
                    loadFavorites()
                }
            }
            // showFavoriteButton can be true, will act as "remove" if already favorite
        )
        recyclerViewFavorites.layoutManager = LinearLayoutManager(context)
        recyclerViewFavorites.adapter = favoritesAdapter

        // History Adapter
        historyAdapter = StationAdapter(
            requireContext(),
            emptyList(),
            onPlayClicked = { station -> playStation(station) },
            onFavoriteToggle = { station, isFavorite ->
                // This will add/remove from favorites and update star icon
                // If added, it won't appear in history's list but star will update if this station is also in favs
                // If removed, star will update.
                // No direct refresh of history list needed here unless fav status affects history display
                if (isFavorite) sharedPrefsManager.addFavorite(station) else sharedPrefsManager.removeFavorite(station.id)
                historyAdapter.notifyDataSetChanged() // to update star states if any history item is also a fav
                favoritesAdapter.updateStations(sharedPrefsManager.getFavoriteStations()) // also refresh fav list in case
            }
        )
        recyclerViewHistory.layoutManager = LinearLayoutManager(context)
        recyclerViewHistory.adapter = historyAdapter
    }

    private fun loadFavorites() {
        val favoriteStations = sharedPrefsManager.getFavoriteStations()
        favoritesAdapter.updateStations(favoriteStations)
    }

    private fun loadHistory() {
        val historyStations = sharedPrefsManager.getStationHistory()
        historyAdapter.updateStations(historyStations)
    }

    private fun playStation(station: RadioStation) {
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
