package com.example.player

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R
import com.example.data.model.VideoTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class PlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set

    private val binder = LocalBinder()

    private val _currentTabState = MutableStateFlow<VideoTab?>(null)
    val currentTabState: StateFlow<VideoTab?> = _currentTabState.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private var cachedThumbnailBitmap: Bitmap? = null
    private var lastThumbnailUrl: String = ""

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return super.onBind for MediaSession clients, or local binder if custom action
        if (intent?.action == ACTION_LOCAL_BIND) {
            return binder
        }
        return super.onBind(intent) ?: binder
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        // Strictly single ExoPlayer instance to prevent hardware codec starvation & audio focus collisions
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingState.value = isPlaying
                updateNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_READY) {
                    _playbackProgress.value = player.currentPosition
                }
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotification()
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("PlaybackService", "ExoPlayer playback error: ${error.errorCodeName}", error)
                val activeTab = _currentTabState.value
                val safeFallbackUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
                if (activeTab != null) {
                    val fallbackMediaItem = MediaItem.Builder()
                        .setMediaId(activeTab.tabId)
                        .setUri(safeFallbackUrl)
                        .build()
                    player.setMediaItem(fallbackMediaItem)
                    player.prepare()
                    try {
                        player.play()
                    } catch (_: Exception) {
                        player.playWhenReady = true
                    }
                }
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause() else player.play()
            }
            ACTION_REWIND_10 -> {
                val newPos = (player.currentPosition - 10000L).coerceAtLeast(0L)
                player.seekTo(newPos)
            }
            ACTION_FORWARD_10 -> {
                val newPos = (player.currentPosition + 10000L).coerceAtMost(player.duration.coerceAtLeast(0L))
                player.seekTo(newPos)
            }
            ACTION_CLOSE_ACTIVE_TAB -> {
                player.stop()
                player.clearMediaItems()
                _currentTabState.value = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Prepares and starts playback for the specified tab.
     * When switching tabs, the caller saves the previous position, then calls this.
     */
    fun playTab(tab: VideoTab, streamUrl: String) {
        _currentTabState.value = tab

        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(tab.videoId)
            .build()

        player.setMediaItem(mediaItem)
        if (tab.lastPlaybackPositionMs > 0) {
            player.seekTo(tab.lastPlaybackPositionMs)
        }
        player.prepare()
        try {
            player.play()
        } catch (e: SecurityException) {
            // In case WAKE_LOCK is restricted by custom container or user profile
            player.playWhenReady = true
        }

        loadThumbnailAsync(tab.thumbnailUrl)
        startForegroundWithCustomNotification()
    }

    fun pause() {
        player.pause()
    }

    fun play() {
        try {
            player.play()
        } catch (e: SecurityException) {
            player.playWhenReady = true
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    /**
     * Detach video rendering surface when the app is minimized or the screen turns off,
     * while audio playback continues uninterrupted without PIP.
     */
    fun detachSurface() {
        player.clearVideoSurface()
    }

    private fun loadThumbnailAsync(url: String) {
        if (url == lastThumbnailUrl && cachedThumbnailBitmap != null) return
        lastThumbnailUrl = url
        serviceScope.launch(Dispatchers.IO) {
            try {
                val stream = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                withContext(Dispatchers.Main) {
                    cachedThumbnailBitmap = bitmap
                    updateNotification()
                }
            } catch (e: Exception) {
                // Keep default icon fallback
            }
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundWithCustomNotification() {
        val notification = buildCustomNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildCustomNotification())
    }

    private fun buildCustomNotification(): Notification {
        val currentTab = _currentTabState.value
        val title = currentTab?.title ?: "SoyTube Playback"
        val channel = currentTab?.channelTitle ?: "Playing Audio/Video"

        val remoteViews = RemoteViews(packageName, R.layout.notification_media_playback).apply {
            setTextViewText(R.id.notification_title, title)
            setTextViewText(R.id.notification_channel, channel)

            // Dynamic Thumbnail Preview
            if (cachedThumbnailBitmap != null) {
                setImageViewBitmap(R.id.notification_thumbnail, cachedThumbnailBitmap)
            } else {
                setImageViewResource(R.id.notification_thumbnail, R.drawable.ic_notification_icon)
            }

            // Play/Pause icon toggle
            if (player.isPlaying) {
                setImageViewResource(R.id.btn_notification_play_pause, R.drawable.ic_pause)
            } else {
                setImageViewResource(R.id.btn_notification_play_pause, R.drawable.ic_play_arrow)
            }

            // Large click target PendingIntents
            setOnClickPendingIntent(R.id.btn_notification_rewind, createActionPendingIntent(ACTION_REWIND_10, 1))
            setOnClickPendingIntent(R.id.btn_notification_play_pause, createActionPendingIntent(ACTION_PLAY_PAUSE, 2))
            setOnClickPendingIntent(R.id.btn_notification_forward, createActionPendingIntent(ACTION_FORWARD_10, 3))
            setOnClickPendingIntent(R.id.btn_notification_close, createActionPendingIntent(ACTION_CLOSE_ACTIVE_TAB, 4))
        }

        // Tap notification to bring MainActivity forward
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // Prevent aggressive OEM OS task-killers (HyperOS/MIUI) from killing the process
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build().apply {
                flags = flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
            }
    }

    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SoyTube Playback Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground playback notification with custom HyperOS controls"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "soytube_playback_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_LOCAL_BIND = "com.example.soytube.LOCAL_BIND"
        const val ACTION_PLAY_PAUSE = "com.example.soytube.PLAY_PAUSE"
        const val ACTION_REWIND_10 = "com.example.soytube.REWIND_10"
        const val ACTION_FORWARD_10 = "com.example.soytube.FORWARD_10"
        const val ACTION_CLOSE_ACTIVE_TAB = "com.example.soytube.CLOSE_ACTIVE_TAB"
    }
}
