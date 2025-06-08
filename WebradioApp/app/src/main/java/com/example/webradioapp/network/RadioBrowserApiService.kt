package com.example.webradioapp.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// --- API SERVICE ---

interface RadioBrowserApiService {

    // 🔍 Recherche de stations par critères multiples
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("language") language: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "votes",
        @Query("reverse") reverse: Boolean = true
    ): Response<List<RadioStation>>

    // 📻 Obtenir une station par UUID
    @GET("json/stations/byuuid")
    suspend fun getStationById(@Query("uuids") uuid: String): Response<List<RadioStation>>

    // 🔝 Stations les plus votées
    @GET("json/stations/topvote")
    suspend fun getTopStations(@Query("limit") limit: Int = 50): Response<List<RadioStation>>

    // 🔥 Stations les plus écoutées
    @GET("json/stations/topclick")
    suspend fun getPopularStations(@Query("limit") limit: Int = 50): Response<List<RadioStation>>

    // 🌍 Liste des pays
    @GET("json/countries")
    suspend fun getCountries(): Response<List<Country>>

    // 🗣️ Liste des langues
    @GET("json/languages")
    suspend fun getLanguages(): Response<List<Language>>

    // 🏷️ Liste des genres/tags
    @GET("json/tags")
    suspend fun getTags(@Query("limit") limit: Int = 100): Response<List<Tag>>
}

// --- DATA CLASSES ---

data class RadioStation(
    val name: String,
    val url: String,
    val favicon: String?,
    val country: String,
    val language: String,
    val tags: String,
    val codec: String,
    val bitrate: Int,
    val uuid: String
)

data class Country(
    val name: String,
    val stationcount: Int
)

data class Language(
    val name: String,
    val stationcount: Int
)

data class Tag(
    val name: String,
    val stationcount: Int
)
