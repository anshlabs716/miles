package com.example.miles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.miles.data.model.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE isDeleted = 0 ORDER BY startTime DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isDeleted = 0 ORDER BY startTime DESC")
    suspend fun getAllActivitiesOnce(): List<ActivityEntity>

    @Query("SELECT * FROM activities WHERE isDeleted = 0 AND isFavorite = 1 ORDER BY startTime DESC")
    fun getFavoriteActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    fun getActivityById(id: String): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id LIMIT 1")
    suspend fun getActivityByIdOnce(id: String): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("UPDATE activities SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun moveToTrash(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE activities SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: String)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun permanentlyDelete(id: String)

    @Query("DELETE FROM activities WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM activities")
    suspend fun clearAll()
}
