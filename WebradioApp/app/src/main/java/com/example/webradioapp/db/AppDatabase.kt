package com.example.webradioapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.webradioapp.model.Alarm
import com.example.webradioapp.model.RadioStation
import com.example.webradioapp.db.entities.CountryEntity
import com.example.webradioapp.db.entities.GenreEntity
import com.example.webradioapp.db.entities.LanguageEntity
import com.example.webradioapp.db.dao.CountryDao
import com.example.webradioapp.db.dao.GenreDao
import com.example.webradioapp.db.dao.LanguageDao

// Incremented version to 3
@Database(entities = [RadioStation::class, Alarm::class, CountryEntity::class, GenreEntity::class, LanguageEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteStationDao(): FavoriteStationDao
    abstract fun historyStationDao(): HistoryStationDao
    abstract fun alarmDao(): AlarmDao // Added AlarmDao
    abstract fun countryDao(): CountryDao
    abstract fun genreDao(): GenreDao
    abstract fun languageDao(): LanguageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // val MIGRATION_1_2 = object : Migration(1, 2) { ... }
        // val MIGRATION_2_3 = object : Migration(2, 3) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("""
        //             CREATE TABLE IF NOT EXISTS `alarms` (
        //                 `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        //                 `hour` INTEGER NOT NULL,
        //                 `minute` INTEGER NOT NULL,
        //                 `stationId` TEXT NOT NULL,
        //                 `stationName` TEXT NOT NULL,
        //                 `stationIconUrl` TEXT,
        //                 `isEnabled` INTEGER NOT NULL DEFAULT 1
        //             )
        //         """.trimIndent())
        //     }
        // }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webradio_app_database"
                )
                // .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add new migration if not using fallback
                .fallbackToDestructiveMigration() // Keeps things simple for now
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
