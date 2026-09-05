package com.example.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    player: ExoPlayer?,
    modifier: Modifier = Modifier,
    useController: Boolean = false // Forced to false for custom UI
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = context as? Activity

    var showControls by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(player?.isPlaying == true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var isFullscreen by remember { mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) }
    var showSettings by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // In background, ExoPlayer continues audio seamlessly in PlaybackService
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                isPlaying = isPlayingNow
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player != null) {
                    duration = player.duration.coerceAtLeast(0)
                }
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying, player) {
        while (isPlaying && player != null) {
            currentPosition = player.currentPosition
            bufferedPosition = player.bufferedPosition
            delay(100)
        }
    }

    LaunchedEffect(showControls, isPlaying, showSettings) {
        if (showControls && isPlaying && !showSettings) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isFullscreen) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
            .background(Color.Black)
            .testTag("player_surface_container"),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        setKeepContentOnPlayerReset(true)
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player != player) {
                        view.player = player
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("exoplayer_surface")
            )

            // Gesture Overlay for Double Taps (Skip) and Single Tap (Controls)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val halfWidth = size.width / 2
                                if (offset.x < halfWidth) {
                                    // Rewind 10s
                                    val newTime = (player.currentPosition - 10000).coerceAtLeast(0)
                                    player.seekTo(newTime)
                                    currentPosition = newTime
                                } else {
                                    // Forward 10s
                                    val newTime = (player.currentPosition + 10000).coerceAtMost(if (duration > 0) duration else Long.MAX_VALUE)
                                    player.seekTo(newTime)
                                    currentPosition = newTime
                                }
                                showControls = true
                            },
                            onTap = {
                                showControls = !showControls
                                showSettings = false
                            }
                        )
                    }
            )

            // Custom UI Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Top Right Action Bar
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { /* Toggle CC placeholder */ }) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = "Closed Captions", tint = Color.White)
                        }
                        
                        Box {
                            IconButton(onClick = { showSettings = !showSettings }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSettings,
                                onDismissRequest = { showSettings = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Quality: Auto") },
                                    onClick = { showSettings = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback Speed: Normal") },
                                    onClick = { 
                                        player.setPlaybackSpeed(1.0f)
                                        showSettings = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Playback Speed: 1.5x") },
                                    onClick = { 
                                        player.setPlaybackSpeed(1.5f)
                                        showSettings = false
                                    }
                                )
                            }
                        }
                    }

                    // Center Play/Pause Button
                    IconButton(
                        onClick = {
                            if (isPlaying) player.pause() else player.play()
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Bottom Bar: Squiggly Progress + Timers
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${formatTime(currentPosition)} / ${formatTime(duration)}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                            IconButton(
                                onClick = {
                                    isFullscreen = !isFullscreen
                                    if (isFullscreen) {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        SquigglySlider(
                            progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                            bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f,
                            isPlaying = isPlaying,
                            onSeek = { p ->
                                val newPos = (p * duration).toLong()
                                player.seekTo(newPos)
                                currentPosition = newPos
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Select a video to play",
                    tint = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = "Select a video or open a tab to start playing",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SquigglySlider(
    progress: Float,
    bufferedProgress: Float,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                withFrameNanos {
                    phase.floatValue += 1.5f // Adjust speed of the squiggly animation
                }
            }
        }
    }

    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val currentProgress = dragProgress ?: progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragProgress?.let { onSeek(it) }
                        dragProgress = null
                    },
                    onHorizontalDrag = { change, _ ->
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Draw background track
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        
        // Draw buffered track
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width * bufferedProgress.coerceIn(0f, 1f), centerY),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
        
        // Draw active squiggly track
        val activeWidth = width * currentProgress
        val path = Path()
        path.moveTo(0f, centerY)
        
        val amplitude = 6f
        val frequency = 0.15f
        
        var x = 0f
        while (x < activeWidth) {
            val y = centerY + kotlin.math.sin((x - phase.floatValue) * frequency) * amplitude
            if (x == 0f) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
            x += 2f
        }
        
        if (activeWidth > 0f) {
            val finalY = centerY + kotlin.math.sin((activeWidth - phase.floatValue) * frequency) * amplitude
            path.lineTo(activeWidth, finalY)
            drawPath(
                path = path,
                color = Color.Red,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            // Draw Thumb
            drawCircle(
                color = Color.Red,
                radius = 16f,
                center = Offset(activeWidth, finalY)
            )
        } else {
            // Draw just the thumb at 0 if no progress
            drawCircle(
                color = Color.Red,
                radius = 16f,
                center = Offset(0f, centerY)
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
