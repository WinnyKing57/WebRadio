package com.example.webradioapp.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val name: String, // e.g., "rock", "pop" (Tag Name from API)
    val stationCount: Int
)
