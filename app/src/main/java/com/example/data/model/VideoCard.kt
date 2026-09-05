package com.example.data.model

data class VideoCard(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val duration: String = "",
    val viewCountText: String = "",
    val publishedTimeText: String = ""
)
