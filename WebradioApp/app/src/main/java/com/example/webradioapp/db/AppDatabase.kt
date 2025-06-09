package com.example.webradioapp.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.webradioapp.model.RadioStation

@Database(entities = [RadioStation::class], version = 2, exportSchema = false) // exportSchema = false for simplicity
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteStationDao(): FavoriteStationDao
    abstract fun historyStationDao(): HistoryStationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stations ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "webradio_app_database"
                )
                .addMigrations(MIGRATION_1_2)
                // Wipes and rebuilds instead of migrating if no Migration object.
                // .fallbackToDestructiveMigration() // Use this only during development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
