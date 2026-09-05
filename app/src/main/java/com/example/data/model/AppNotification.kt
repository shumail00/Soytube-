package com.example.data.model

enum class NotificationType {
    VIDEO_UPLOAD,
    COMMUNITY_POST,
    LIVE_STREAM,
    SYSTEM_UPDATE
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val channelTitle: String,
    val channelAvatarUrl: String = "",
    val videoThumbnailUrl: String? = null,
    val videoId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.VIDEO_UPLOAD
)
