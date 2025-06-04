package com.example.webradioapp.fragments // Placing in fragments package as it's closely tied to SearchFragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.utils.SharedPreferencesManager

class StationAdapter(
    private val context: Context, // Added context for SharedPreferencesManager
    private var stations: List<RadioStation>,
    private val onPlayClicked: (RadioStation) -> Unit,
    private val onFavoriteToggle: (RadioStation, Boolean) -> Unit, // Callback for favorite toggle
    private val showFavoriteButton: Boolean = true // To hide favorite button in FavoritesFragment if needed
) : RecyclerView.Adapter<StationAdapter.StationViewHolder>() {

    private val sharedPrefsManager: SharedPreferencesManager by lazy {
        SharedPreferencesManager(context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        holder.bind(station)
    }

    override fun getItemCount(): Int = stations.size

    fun updateStations(newStations: List<RadioStation>) {
        stations = newStations
        notifyDataSetChanged() // Consider using DiffUtil for better performance
    }

    inner class StationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_station_name)
        private val genreTextView: TextView = itemView.findViewById(R.id.tv_station_genre)
        private val playButton: Button = itemView.findViewById(R.id.btn_item_play)
        private val favoriteButton: ImageButton = itemView.findViewById(R.id.btn_favorite)

        fun bind(station: RadioStation) {
            nameTextView.text = station.name
            genreTextView.text = station.genre ?: "Unknown Genre"

            playButton.setOnClickListener {
                onPlayClicked(station)
            }

            if (showFavoriteButton) {
                favoriteButton.visibility = View.VISIBLE
                updateFavoriteButtonState(station.id)

                favoriteButton.setOnClickListener {
                    val isCurrentlyFavorite = sharedPrefsManager.isFavorite(station.id)
                    if (isCurrentlyFavorite) {
                        sharedPrefsManager.removeFavorite(station.id)
                    } else {
                        sharedPrefsManager.addFavorite(station)
                    }
                    updateFavoriteButtonState(station.id)
                    onFavoriteToggle(station, !isCurrentlyFavorite) // Notify fragment
                }
            } else {
                favoriteButton.visibility = View.GONE
            }
        }

        private fun updateFavoriteButtonState(stationId: String) {
            if (sharedPrefsManager.isFavorite(stationId)) {
                favoriteButton.setImageResource(R.drawable.ic_star_filled)
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.yellow_700_star)); // Example color
            } else {
                favoriteButton.setImageResource(R.drawable.ic_star_border)
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.grey_500_star)); // Example color
            }
        }
    }
}
