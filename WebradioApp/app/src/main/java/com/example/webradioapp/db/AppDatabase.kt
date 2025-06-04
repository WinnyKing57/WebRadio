package com.example.webradioapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.webradioapp.model.RadioStation

@Database(entities = [RadioStation::class], version = 1, exportSchema = false) // exportSchema = false for simplicity
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteStationDao(): FavoriteStationDao
    abstract fun historyStationDao(): HistoryStationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webradio_app_database"
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // .fallbackToDestructiveMigration() // Use this only during development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
