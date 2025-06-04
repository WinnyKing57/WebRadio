package com.example.webradioapp.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Choose a base URL. For production, consider fetching server list and choosing dynamically.
    // Using a server that supports HTTPS is crucial.
    // Found via: https://api.radio-browser.info/ Langing page -> https://de1.api.radio-browser.info/ (seems to be a good default)
    private const val BASE_URL = "https://de1.api.radio-browser.info/" // Retrofit paths will start with "json/"

    val instance: RadioBrowserApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Or Level.BASIC for less verbose logs
        }

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add logging interceptor for debugging
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(RadioBrowserApiService::class.java)
    }
}
