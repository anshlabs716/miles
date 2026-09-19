package com.example.miles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.miles.data.model.GoalEntity
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.model.SavedRouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    fun getAllRoutes(): Flow<List<SavedRouteEntity>>

    @Query("SELECT * FROM saved_routes WHERE id = :id LIMIT 1")
    suspend fun getRouteById(id: String): SavedRouteEntity?

    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    suspend fun getAllRoutesOnce(): List<SavedRouteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity)

    @Update
    suspend fun updateRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRoute(id: String)

    @Query("DELETE FROM saved_routes")
    suspend fun clearAll()
}

@Dao
interface PrivacyZoneDao {
    @Query("SELECT * FROM privacy_zones")
    fun getAllZones(): Flow<List<PrivacyZoneEntity>>

    @Query("SELECT * FROM privacy_zones")
    suspend fun getAllZonesOnce(): List<PrivacyZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: PrivacyZoneEntity)

    @Query("DELETE FROM privacy_zones WHERE id = :id")
    suspend fun deleteZone(id: String)

    @Query("DELETE FROM privacy_zones")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsOnce(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: String)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}
