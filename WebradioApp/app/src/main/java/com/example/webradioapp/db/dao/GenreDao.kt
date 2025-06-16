package com.example.webradioapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.webradioapp.db.entities.GenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(genres: List<GenreEntity>)

    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAll(): Flow<List<GenreEntity>>

    @Query("DELETE FROM genres")
    suspend fun deleteAll()
}
