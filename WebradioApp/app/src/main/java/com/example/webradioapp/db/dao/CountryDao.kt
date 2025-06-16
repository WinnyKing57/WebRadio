package com.example.webradioapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.webradioapp.db.entities.CountryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(countries: List<CountryEntity>)

    @Query("SELECT * FROM countries ORDER BY name ASC")
    fun getAll(): Flow<List<CountryEntity>>

    @Query("DELETE FROM countries")
    suspend fun deleteAll()
}
