package com.example.webradioapp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.webradioapp.db.entities.LanguageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(languages: List<LanguageEntity>)

    @Query("SELECT * FROM languages ORDER BY name ASC")
    fun getAll(): Flow<List<LanguageEntity>>

    @Query("DELETE FROM languages")
    suspend fun deleteAll()
}
