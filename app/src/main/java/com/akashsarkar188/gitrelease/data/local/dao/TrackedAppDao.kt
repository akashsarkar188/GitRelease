package com.akashsarkar188.gitrelease.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.akashsarkar188.gitrelease.data.local.entity.TrackedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackedAppDao {
    @Query("SELECT * FROM tracked_apps")
    fun getAllApps(): Flow<List<TrackedApp>>

    @Query("SELECT * FROM tracked_apps WHERE id = :id")
    suspend fun getAppById(id: Long): TrackedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: TrackedApp)

    @Update
    suspend fun updateApp(app: TrackedApp)

    @Delete
    suspend fun deleteApp(app: TrackedApp)
}
