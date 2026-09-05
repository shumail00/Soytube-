package com.example.data.model

import java.io.Serializable

/**
 * Encapsulates extracted stream resolution and metadata from NewPipeExtractor.
 */
data class StreamData(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelAvatarUrl: String?,
    val durationSeconds: Long,
    val bestVideoStreamUrl: String,
    val bestAudioStreamUrl: String,
    val isLiveStream: Boolean = false,
    val viewCount: Long = 0L,
    val description: String = ""
) : Serializable
