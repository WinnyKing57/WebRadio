package com.example.webradioapp.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.webradioapp.model.Alarm

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: Alarm): Long

    @Update
    suspend fun update(alarm: Alarm)

    @Delete
    suspend fun delete(alarm: Alarm)

    @Query("SELECT * FROM alarms ORDER BY hour, minute ASC")
    fun getAllAlarms(): LiveData<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?

    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY hour, minute ASC")
    suspend fun getEnabledAlarms(): List<Alarm>
}
