package com.example.webradioapp.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey val name: String, // e.g., "US", "DE" (Country Code or Name from API)
    val stationCount: Int
)
