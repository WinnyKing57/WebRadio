package com.example.webradioapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadioStation(
    val id: String, // Using String for ID for more flexibility e.g. from APIs
    val name: String,
    val streamUrl: String,
    val genre: String? = null,
    val country: String? = null,
    val language: String? = null,
    val faviconUrl: String? = null
) : Parcelable
