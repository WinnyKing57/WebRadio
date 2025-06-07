package com.example.webradioapp.network.model

import com.google.gson.annotations.SerializedName
import com.example.webradioapp.model.RadioStation // Import the domain model

// Based on fields from https://de1.api.radio-browser.info/
// Not all fields are included, only those relevant for mapping to RadioStation or potential future use.
data class ApiRadioStation(
    @SerializedName("stationuuid") val stationuuid: String,
    @SerializedName("name") val name: String?, // Name can be null sometimes
    @SerializedName("url_resolved") val urlResolved: String?, // Stream URL
    @SerializedName("url") val urlAlternate: String?, // Fallback if url_resolved is null
    @SerializedName("tags") val tags: String?, // Comma-separated list of tags/genres
    @SerializedName("countrycode") val countryCode: String?,
    @SerializedName("language") val language: String?, // Comma-separated list of languages
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("codec") val codec: String?,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("hls") val hls: Int?, // 1 if HLS, 0 otherwise
    @SerializedName("lastcheckok") val lastCheckOk: Int?, // 1 if last check was okay
    @SerializedName("lastcheckoktime_iso8601") val lastCheckOkTime: String?

)

// Mapper function to convert ApiRadioStation to our domain RadioStation model
fun ApiRadioStation.toDomain(): RadioStation? {
    // urlResolved is the primary stream URL. Fallback to urlAlternate if urlResolved is null or empty.
    val streamUrl = if (this.urlResolved?.isNotBlank() == true) this.urlResolved else this.urlAlternate
    if (streamUrl.isNullOrBlank() || this.name.isNullOrBlank()) {
        // Essential data is missing, cannot create a valid RadioStation
        return null
    }

    // Tags are comma-separated, take the first one as primary genre
    val genre = this.tags?.split(",")?.firstOrNull()?.trim()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    // Languages are comma-separated, take the first one
    val language = this.language?.split(",")?.firstOrNull()?.trim()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }


    return RadioStation(
        id = this.stationuuid,
        name = this.name,
        streamUrl = streamUrl,
        genre = genre,
        country = this.countryCode, // countryCode from API is usually short (e.g., "US", "GB")
        language = language,
        faviconUrl = if (this.favicon?.isNotBlank() == true) this.favicon else null // Ensure favicon is not empty string
    )
}
