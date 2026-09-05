package com.example.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoCard
import com.example.ui.MainViewModel
import com.example.ui.NavSection
import com.example.ui.components.DesktopBrowserTabBar
import com.example.ui.components.ExoPlayerView
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.MiniPlayer
import com.example.ui.components.ThinnerSearchBar
import com.example.ui.components.VideoCardItem
import com.example.ui.components.YouTubeStylePlayerDetails
import com.example.ui.theme.SoyTubeMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLayout(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val tabs by viewModel.tabs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val homeFeed by viewModel.homeFeed.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentNav by viewModel.currentNav.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val isResolvingStream by viewModel.isResolvingStream.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val savedVideos by viewModel.savedVideos.collectAsState()
    val backgroundAudioEnabled by viewModel.backgroundAudioEnabled.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()

    val playbackService = viewModel.getService()
    val isPlayingActiveVideo = isPlayerExpanded && activeTab != null && activeTab?.videoId?.isNotEmpty() == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("phone_layout_scaffold")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Desktop Browser Tab Bar at the VERY TOP with Rounded Header Container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    DesktopBrowserTabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { tabId ->
                            viewModel.switchTab(tabId)
                            viewModel.setPlayerExpanded(true)
                        },
                        onCloseTab = { tabId ->
                            viewModel.closeTab(tabId)
                        },
                        onNewTab = {
                            viewModel.createNewHomeTab()
                        }
                    )

                    // 2. Thinner Search / Address Bar with "Search or link"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        ThinnerSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onSearch = { viewModel.submitSearchOrLink(searchQuery) }
                        )
                    }
                }
            }

            // 3. Main Body Content based on Navigation & Active Video Playback
            when (currentNav) {
                NavSection.HOME -> {
                    if (isPlayingActiveVideo) {
                        val currentTab = activeTab!!
                        val isCurrentVideoSaved = savedVideos.any { it.videoId == currentTab.videoId }

                        // YouTube-style Player view & details
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Video Player Screen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                ExoPlayerView(
                                    player = playbackService?.player,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Top collapse chevron overlay
                                IconButton(
                                    onClick = { viewModel.setPlayerExpanded(false) },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .testTag("collapse_player_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Collapse Player",
                                        tint = Color.White
                                    )
                                }

                                if (isResolvingStream) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Resolving stream...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // YouTube-style details: title, subscribe, actions, description, comments, recommendations
                            YouTubeStylePlayerDetails(
                                tab = currentTab,
                                comments = comments,
                                recommendedVideos = homeFeed.filter { it.videoId != currentTab.videoId },
                                isSaved = isCurrentVideoSaved,
                                backgroundAudioEnabled = backgroundAudioEnabled,
                                onToggleSave = {
                                    viewModel.toggleSaveVideo(
                                        VideoCard(
                                            videoId = currentTab.videoId,
                                            title = currentTab.title,
                                            channelTitle = currentTab.channelTitle,
                                            thumbnailUrl = currentTab.thumbnailUrl
                                        )
                                    )
                                },
                                onToggleBackgroundAudio = {
                                    viewModel.toggleBackgroundAudio()
                                },
                                onOpenInNewTab = {
                                    viewModel.openVideoInTab(
                                        VideoCard(
                                            videoId = currentTab.videoId,
                                            title = currentTab.title,
                                            channelTitle = currentTab.channelTitle,
                                            thumbnailUrl = currentTab.thumbnailUrl
                                        ),
                                        activateImmediately = false
                                    )
                                },
                                onPlayVideo = { video ->
                                    viewModel.openVideoInTab(video, activateImmediately = true)
                                },
                                onAddComment = { commentText ->
                                    viewModel.addComment(commentText)
                                }
                            )
                        }
                    } else {
                        // Standard Home Feed List
                        if (isFeedLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .testTag("phone_feed_list"),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Recommended",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }

                                items(homeFeed, key = { it.videoId }) { video ->
                                    VideoCardItem(
                                        video = video,
                                        onPlayNow = {
                                            viewModel.openVideoInTab(video, activateImmediately = true)
                                            viewModel.setPlayerExpanded(true)
                                        },
                                        onOpenInNewTab = {
                                            viewModel.openVideoInTab(video, activateImmediately = false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                NavSection.SUBSCRIPTIONS -> {
                    SubscriptionsView(
                        viewModel = viewModel,
                        onPlayVideo = { video ->
                            viewModel.openVideoInTab(video, activateImmediately = true)
                            viewModel.setPlayerExpanded(true)
                        },
                        onOpenInNewTab = { video ->
                            viewModel.openVideoInTab(video, activateImmediately = false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                NavSection.NOTIFICATIONS -> {
                    NotificationsView(
                        viewModel = viewModel,
                        onPlayVideo = { video ->
                            viewModel.openVideoInTab(video, activateImmediately = true)
                            viewModel.setPlayerExpanded(true)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                NavSection.ACCOUNT -> {
                    AccountView(
                        viewModel = viewModel,
                        onPlayVideo = { video ->
                            viewModel.openVideoInTab(video, activateImmediately = true)
                            viewModel.setPlayerExpanded(true)
                        },
                        onOpenInNewTab = { video ->
                            viewModel.openVideoInTab(video, activateImmediately = false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Floating Bottom Bar Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bottom-docked mini-player when collapsed
            if (!isPlayerExpanded && activeTab != null && activeTab?.videoId?.isNotEmpty() == true) {
                MiniPlayer(
                    activeTab = activeTab,
                    playbackService = playbackService,
                    onExpand = { viewModel.setPlayerExpanded(true) },
                    onClose = { activeTab?.let { viewModel.closeTab(it.tabId) } }
                )
            }

            // Floating Pill-Shaped Material You Bottom Navigation Bar with completely transparent background
            FloatingPillNavBar(
                currentNav = currentNav,
                onNavSelect = { viewModel.setNavSection(it) },
                isLoggedIn = isLoggedIn,
                unreadNotificationCount = unreadNotificationCount
            )
        }
    }
}
