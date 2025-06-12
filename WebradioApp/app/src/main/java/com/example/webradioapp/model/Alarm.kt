package com.example.webradioapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val stationId: String, // Assuming RadioStation has a String ID
    val stationName: String, // For display purposes
    val stationIconUrl: String?, // For display purposes
    val isEnabled: Boolean = true
)
