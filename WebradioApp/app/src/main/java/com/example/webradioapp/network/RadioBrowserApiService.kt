package com.example.webradioapp.network

import com.example.webradioapp.network.model.ApiRadioStation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url // For dynamic base URL if needed later

/**
 * Retrofit service interface for interacting with the Radio-Browser API.
 * Defines suspend functions for fetching radio station data.
 */
interface RadioBrowserApiService {

    /**
     * Searches for radio stations based on various criteria.
     * @param name Search by station name.
     * @param genre Search by station genre (tag).
     * @param countryCode Search by country code (e.g., "US").
     * @param language Search by language (e.g., "english").
     * @param limit Maximum number of results to return.
     * @param offset Offset for pagination.
     * @param hideBroken If true, tries to hide stations that are known to be broken.
     * @param order Field to order results by (e.g., "clickcount", "name", "votes").
     * @param reverse If true, reverses the order (descending).
     * @return A [Response] containing a list of [ApiRadioStation] objects.
     */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("tag") genre: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("language") language: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = true, // Good default to filter out broken streams
        @Query("order") order: String = "clickcount", // Order by popularity (clickcount) or votes
        @Query("reverse") reverse: Boolean = true // True for descending order (most popular first)
    ): Response<List<ApiRadioStation>>

    @GET("json/stations/bytag/{tag}")
    suspend fun getStationsByGenre(
        @Path("tag") genre: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): Response<List<ApiRadioStation>>

    // Example of how to get stations by language
    @GET("json/stations/bylanguage/{languageName}")
    suspend fun getStationsByLanguage(
        @Path("languageName") languageName: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): Response<List<ApiRadioStation>>

    // It might be useful to have a method to directly get a station by its UUID
    @GET("json/stations/byuuid")
    suspend fun getStationByUuid(@Query("uuids") uuid: String): Response<List<ApiRadioStation>> // API returns a list even for single UUID
}
package com.example.webradioapp.network

import com.example.webradioapp.model.RadioStation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApiService {

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

    @GET("json/stations/byuuid")
    suspend fun getStationById(@Query("uuids") uuid: String): Response<List<RadioStation>>

    @GET("json/stations/topvote")
    suspend fun getTopStations(@Query("limit") limit: Int = 50): Response<List<RadioStation>>

    @GET("json/stations/topclick")
    suspend fun getPopularStations(@Query("limit") limit: Int = 50): Response<List<RadioStation>>

    @GET("json/countries")
    suspend fun getCountries(): Response<List<Country>>

    @GET("json/languages")
    suspend fun getLanguages(): Response<List<Language>>

    @GET("json/tags")
    suspend fun getTags(@Query("limit") limit: Int = 100): Response<List<Tag>>
}

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
