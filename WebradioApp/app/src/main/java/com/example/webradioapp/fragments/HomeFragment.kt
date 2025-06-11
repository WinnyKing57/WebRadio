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

    private lateinit var tvHomePlaceholder: TextView // Added for placeholder management

    // Now Playing UI
    private lateinit var nowPlayingDetailsContainer: View
    private lateinit var ivNowPlayingStationIcon: ImageView
    private lateinit var tvNowPlayingStationName: TextView
    private lateinit var ibNowPlayingPlayPause: ImageButton
    private lateinit var seekbarNowPlayingVolume: SeekBar
    private lateinit var audioManager: AudioManager

    // Popular Stations UI and data
    private lateinit var tvPopularStationsTitle: TextView
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("HomeFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvHomePlaceholder = view.findViewById(R.id.tv_home_placeholder) // Initialize placeholder

        // Initialize Now Playing UI
        nowPlayingDetailsContainer = view.findViewById(R.id.now_playing_details_container)
        ivNowPlayingStationIcon = view.findViewById(R.id.iv_now_playing_station_icon)
        tvNowPlayingStationName = view.findViewById(R.id.tv_now_playing_station_name)
        ibNowPlayingPlayPause = view.findViewById(R.id.ib_now_playing_play_pause)
        seekbarNowPlayingVolume = view.findViewById(R.id.seekbar_now_playing_volume)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

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
        setupNowPlayingObservers() // New call
        setupNowPlayingControls() // New call

        loadPopularStations()
        loadHistoryStations() // New data loading call
    }

    private fun setupNowPlayingObservers() {
        StreamingService.isPlayingLiveData.observe(viewLifecycleOwner) { isPlaying ->
            if (nowPlayingDetailsContainer.visibility == View.VISIBLE) { // Only update if section is visible
                ibNowPlayingPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
            }
        }

        StreamingService.currentPlayingStationLiveData.observe(viewLifecycleOwner) { station ->
            if (station != null) {
                nowPlayingDetailsContainer.visibility = View.VISIBLE
                tvNowPlayingStationName.text = station.name
                // TODO: Load station.favicon into ivNowPlayingStationIcon (e.g., using Glide/Coil)
                // Update play/pause button state based on current isPlayingLiveData value
                val currentIsPlaying = StreamingService.isPlayingLiveData.value ?: false
                ibNowPlayingPlayPause.setImageResource(if (currentIsPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow)
                tvHomePlaceholder.visibility = View.GONE // If this section is active, general placeholder can hide.
            } else {
                nowPlayingDetailsContainer.visibility = View.GONE
                // Optionally, make tvHomePlaceholder visible again if no other content is showing
                // if (currentPopularStations.isEmpty() && currentHistoryStations.isEmpty()) {
                //    tvHomePlaceholder.visibility = View.VISIBLE
                // }
            }
        }

        StreamingService.playerErrorLiveData.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Display the error. Could be a Toast, or update a dedicated error TextView in the "Now Playing" area.
                Toast.makeText(requireContext(), "Playback Error: $it", Toast.LENGTH_LONG).show()
                // Optionally, reset the error in LiveData
                // StreamingService.playerErrorLiveData.postValue(null) // Be careful if multiple observers
            }
        }
    }

    private fun setupNowPlayingControls() {
        ibNowPlayingPlayPause.setOnClickListener {
            val currentIsPlaying = StreamingService.isPlayingLiveData.value ?: false
            val action = if (currentIsPlaying) StreamingService.ACTION_PAUSE else StreamingService.ACTION_PLAY
            Intent(activity, StreamingService::class.java).also { intent ->
                intent.action = action
                activity?.startService(intent)
            }
        }

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        seekbarNowPlayingVolume.max = maxVolume
        seekbarNowPlayingVolume.progress = currentVolume

        seekbarNowPlayingVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
        viewLifecycleOwner.lifecycleScope.launch {
            Log.d("HomeFragment", "observeFavoriteChanges - coroutine started, collecting flow")
            favoritesViewModel.favoriteStations.collect { favoriteStations ->
                Log.d("HomeFragment", "observeFavoriteChanges - collected favorites, size: ${favoriteStations.size}")
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
                        tvHomePlaceholder.visibility = View.GONE // Hide placeholder if stations are loaded
                    } else {
                        // Handle empty list if needed, maybe show a specific message or keep placeholder
                        rvPopularStations.visibility = View.GONE // Hide RV if no stations
                        tvPopularStationsTitle.visibility = View.GONE // Hide title if no stations
                        tvHomePlaceholder.text = "No popular stations available at the moment."
                        tvHomePlaceholder.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("HomeFragment", "loadPopularStations - API call failed: ${response.message()}")
                    // Optional: Show error message here
                    rvPopularStations.visibility = View.GONE // Hide RV on error
                    tvPopularStationsTitle.visibility = View.GONE // Hide title on error
                    tvHomePlaceholder.text = "Could not load popular stations."
                    tvHomePlaceholder.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "loadPopularStations - Exception: ${e.message}", e)
                // Optional: Show error message here
                rvPopularStations.visibility = View.GONE // Hide RV on exception
                tvPopularStationsTitle.visibility = View.GONE // Hide title on exception
                tvHomePlaceholder.text = "Error loading popular stations."
                tvHomePlaceholder.visibility = View.VISIBLE
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
