package com.example.miles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.GoalEntity
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.model.SavedRouteEntity

@Database(
    entities = [
        ActivityEntity::class,
        SavedRouteEntity::class,
        PrivacyZoneEntity::class,
        GoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MilesDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun savedRouteDao(): SavedRouteDao
    abstract fun privacyZoneDao(): PrivacyZoneDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: MilesDatabase? = null

        fun getInstance(context: Context): MilesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilesDatabase::class.java,
                    "miles_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
