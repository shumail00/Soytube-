package com.example.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class NotificationRepository(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "soytube_alerts_channel"
        const val CHANNEL_NAME = "SoyTube Alerts & Uploads"
        const val CHANNEL_DESC = "Notifications for new video uploads, subscriptions, and playback updates"
    }

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        createNotificationChannel()
        loadInitialNotifications()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadInitialNotifications() {
        val initial = listOf(
            AppNotification(
                id = "notif_1",
                title = "New Video: Blender 4.2 LTS Deep Dive",
                message = "Blender Open Projects uploaded a full walkthrough of Geometry Nodes and EEVEE Next.",
                channelTitle = "Blender",
                channelAvatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=100&auto=format&fit=crop&q=60",
                videoThumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400&auto=format&fit=crop&q=80",
                videoId = "kJQP7kiw5Fk",
                timeAgo = "15m ago",
                isRead = false,
                type = NotificationType.VIDEO_UPLOAD
            ),
            AppNotification(
                id = "notif_2",
                title = "Lofi Girl is Live now!",
                message = "synthwave radio - chill beats to relax / study to is currently streaming.",
                channelTitle = "Lofi Girl",
                channelAvatarUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=100&auto=format&fit=crop&q=60",
                videoThumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&auto=format&fit=crop&q=80",
                videoId = "jfKfPfyJRdk",
                timeAgo = "1h ago",
                isRead = false,
                type = NotificationType.LIVE_STREAM
            ),
            AppNotification(
                id = "notif_3",
                title = "Big Buck Bunny 4K Remaster Released",
                message = "Watch the remastered 60fps classic open movie in stunning dynamic clarity.",
                channelTitle = "Blender Studio",
                channelAvatarUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=100&auto=format&fit=crop&q=60",
                videoThumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=400&auto=format&fit=crop&q=80",
                videoId = "aqz-KE-bpKQ",
                timeAgo = "3h ago",
                isRead = false,
                type = NotificationType.VIDEO_UPLOAD
            ),
            AppNotification(
                id = "notif_4",
                title = "What's new in Jetpack Compose 2026",
                message = "Android Developers posted highlights from Modern Android Architecture.",
                channelTitle = "Android Developers",
                channelAvatarUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=100&auto=format&fit=crop&q=60",
                videoThumbnailUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&auto=format&fit=crop&q=80",
                videoId = "dQw4w9WgXcQ",
                timeAgo = "1d ago",
                isRead = true,
                type = NotificationType.COMMUNITY_POST
            ),
            AppNotification(
                id = "notif_5",
                title = "SoyTube Background Engine Ready",
                message = "Your background audio playback service is optimized for seamless multitasking.",
                channelTitle = "SoyTube System",
                channelAvatarUrl = "",
                videoThumbnailUrl = null,
                videoId = null,
                timeAgo = "2d ago",
                isRead = true,
                type = NotificationType.SYSTEM_UPDATE
            )
        )
        _notifications.value = initial
    }

    fun markAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearAll() {
        _notifications.value = emptyList()
    }

    fun addNotification(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value
        sendSystemNotification(notification)
    }

    fun sendTestNotification(): AppNotification {
        val testNotif = AppNotification(
            id = UUID.randomUUID().toString(),
            title = "Test Notification: Video Upload Alert",
            message = "A channel you subscribe to just published a new video!",
            channelTitle = "SoyTube Notification Backend",
            timeAgo = "Just now",
            isRead = false,
            type = NotificationType.VIDEO_UPLOAD,
            videoId = "aqz-KE-bpKQ"
        )
        addNotification(testNotif)
        return testNotif
    }

    /**
     * Dispatches real Android System Notification into Android's NotificationManager
     */
    fun sendSystemNotification(notification: AppNotification) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                notification.videoId?.let { putExtra("VIDEO_ID", it) }
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notification.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not granted yet
        } catch (e: Exception) {
            // Ignore if notification cannot be posted in test/headless environment
        }
    }
}
