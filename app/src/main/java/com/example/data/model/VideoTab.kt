package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "video_tabs")
data class VideoTab(
    @PrimaryKey
    val tabId: String = UUID.randomUUID().toString(),
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val lastPlaybackPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
