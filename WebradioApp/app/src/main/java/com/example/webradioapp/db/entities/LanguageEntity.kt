package com.example.webradioapp.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey val name: String, // e.g., "english", "fr" (Language Name or Code from API)
    val stationCount: Int
)
