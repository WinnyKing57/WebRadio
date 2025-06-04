package com.example.webradioapp.fragments // Placing in fragments package as it's closely tied to SearchFragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter // Changed to ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.webradioapp.R
import com.example.webradioapp.model.RadioStation
// Removed: import com.example.webradioapp.utils.SharedPreferencesManager

class StationAdapter(
    private val context: Context,
    private val onPlayClicked: (RadioStation) -> Unit,
    private val onFavoriteToggle: (RadioStation) -> Unit, // Simplified callback
    private val showFavoriteButton: Boolean = true
) : ListAdapter<RadioStation, StationAdapter.StationViewHolder>(StationDiffCallback()) { // Using ListAdapter

    // Removed: private val sharedPrefsManager

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        val station = getItem(position) // ListAdapter provides getItem()
        holder.bind(station)
    }

    // Removed: getItemCount(), updateStations() - Handled by ListAdapter

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
                updateFavoriteButtonState(station) // Pass the whole station

                favoriteButton.setOnClickListener {
                    // The station object itself might be stale regarding isFavorite if not from a Flow.
                    // However, the click should toggle the *current* DB state.
                    // The ViewModel will handle the logic of current state and toggle.
                    onFavoriteToggle(station)
                    // The UI update for the star should ideally come from observing the Flow
                    // for this specific station, or by re-submitting the list.
                    // For now, optimistic update:
                    // updateFavoriteButtonState(station.copy(isFavorite = !station.isFavorite))
                    // Better: Let the fragment observe changes and submit new list.
                }
            } else {
                favoriteButton.visibility = View.GONE
            }
        }

        private fun updateFavoriteButtonState(station: RadioStation) {
            // isFavorite is now part of the RadioStation object from DB/ViewModel
            if (station.isFavorite) {
                favoriteButton.setImageResource(R.drawable.ic_star_filled)
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.yellow_700_star))
            } else {
                favoriteButton.setImageResource(R.drawable.ic_star_border)
                favoriteButton.setColorFilter(ContextCompat.getColor(context, R.color.grey_500_star))
            }
        }
    }
}

class StationDiffCallback : DiffUtil.ItemCallback<RadioStation>() {
    override fun areItemsTheSame(oldItem: RadioStation, newItem: RadioStation): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: RadioStation, newItem: RadioStation): Boolean {
        return oldItem == newItem // Relies on data class equals() for content comparison
    }
}
