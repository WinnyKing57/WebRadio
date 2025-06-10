package com.example.webradioapp.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "stations")
data class RadioStation(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "stream_url") val streamUrl: String,
    @ColumnInfo(name = "genre") val genre: String? = null,
    @ColumnInfo(name = "country") val country: String? = null,
    @ColumnInfo(name = "language") val language: String? = null,
    @ColumnInfo(name = "favicon") val favicon: String = "",

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    var isFavorite: Boolean = false,

    @ColumnInfo(name = "last_played_timestamp", defaultValue = "0")
    var lastPlayedTimestamp: Long = 0,

    @ColumnInfo(name = "play_count", defaultValue = "0")
    var playCount: Int = 0
) : Parcelable {
    @Ignore
    constructor() : this(
        id = "IGNORE_ID", // Valeur factice pour champ non-nullable
        name = "IGNORE_NAME", // Valeur factice
        streamUrl = "IGNORE_URL", // Valeur factice
        genre = null,
        country = null,
        language = null,
        favicon = "",
        isFavorite = false,
        lastPlayedTimestamp = 0L,
        playCount = 0
    )
}
