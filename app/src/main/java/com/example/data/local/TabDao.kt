package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.VideoTab
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM video_tabs ORDER BY createdAt ASC")
    fun getAllTabs(): Flow<List<VideoTab>>

    @Query("SELECT * FROM video_tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): VideoTab?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: VideoTab)

    @Update
    suspend fun updateTab(tab: VideoTab)

    @Query("UPDATE video_tabs SET lastPlaybackPositionMs = :positionMs, durationMs = :durationMs WHERE tabId = :tabId")
    suspend fun updatePlaybackPosition(tabId: String, positionMs: Long, durationMs: Long)

    @Query("DELETE FROM video_tabs WHERE tabId = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM video_tabs")
    suspend fun clearAllTabs()
}
