package com.example.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoCard
import com.example.ui.MainViewModel
import com.example.ui.NavSection
import com.example.ui.components.DesktopBrowserTabBar
import com.example.ui.components.ExoPlayerView
import com.example.ui.components.TabItem
import com.example.ui.components.ThinnerSearchBar
import com.example.ui.components.VideoCardItem
import com.example.ui.components.handleExternalKeyboard

@Composable
fun TabletLayout(
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
    val comments by viewModel.comments.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val savedVideos by viewModel.savedVideos.collectAsState()
    val backgroundAudioEnabled by viewModel.backgroundAudioEnabled.collectAsState()

    val isResolvingStream by viewModel.isResolvingStream.collectAsState()
    val streamError by viewModel.streamError.collectAsState()

    val playbackService = viewModel.getService()
    var showKeyboardHelper by remember { mutableStateOf(false) }

    // Root container with external Bluetooth keyboard handler
    Row(
        modifier = modifier
            .fillMaxSize()
            .handleExternalKeyboard(viewModel)
            .testTag("tablet_two_pane_layout")
    ) {
        // ==================== LEFT PANE ====================
        // Persistent Navigation Rail + Tab Stack Drawer
        Row(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            // 1. Navigation Rail (Home, Subscriptions, Account)
            NavigationRail(
                modifier = Modifier
                    .fillMaxHeight()
                    .testTag("tablet_navigation_rail"),
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                NavigationRailItem(
                    selected = currentNav == NavSection.HOME,
                    onClick = { viewModel.setNavSection(NavSection.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (currentNav == NavSection.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("tablet_nav_home")
                )

                NavigationRailItem(
                    selected = currentNav == NavSection.SUBSCRIPTIONS,
                    onClick = { viewModel.setNavSection(NavSection.SUBSCRIPTIONS) },
                    icon = {
                        Icon(
                            imageVector = if (currentNav == NavSection.SUBSCRIPTIONS) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions,
                            contentDescription = "Subscriptions"
                        )
                    },
                    label = { Text("Subscriptions") },
                    modifier = Modifier.testTag("tablet_nav_subscriptions")
                )

                NavigationRailItem(
                    selected = currentNav == NavSection.NOTIFICATIONS,
                    onClick = { viewModel.setNavSection(NavSection.NOTIFICATIONS) },
                    icon = {
                        val unreadCount by viewModel.unreadNotificationCount.collectAsState()
                        BadgedBox(badge = {
                            if (unreadCount > 0) {
                                Badge {
                                    Text(
                                        text = if (unreadCount > 9) "9+" else "$unreadCount",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentNav == NavSection.NOTIFICATIONS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("tablet_nav_notifications")
                )

                NavigationRailItem(
                    selected = currentNav == NavSection.ACCOUNT,
                    onClick = { viewModel.setNavSection(NavSection.ACCOUNT) },
                    icon = {
                        BadgedBox(badge = {
                            if (isLoggedIn) {
                                Badge(modifier = Modifier.size(6.dp))
                            }
                        }) {
                            Icon(
                                imageVector = if (currentNav == NavSection.ACCOUNT) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                                contentDescription = "Account"
                            )
                        }
                    },
                    label = { Text("Account") },
                    modifier = Modifier.testTag("tablet_nav_account")
                )

                Spacer(modifier = Modifier.weight(1f))

                // External Keyboard Guide Trigger
                IconButton(
                    onClick = { showKeyboardHelper = !showKeyboardHelper },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard Shortcuts",
                        tint = if (showKeyboardHelper) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // 2. Tab Stack Drawer
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .testTag("tablet_tab_stack_drawer")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tabs (${tabs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { viewModel.createNewTabShortcut() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("tablet_new_tab_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Tab (Ctrl+T)"
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                if (tabs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No open tabs\nPress Ctrl+T or click a video",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabs, key = { it.tabId }) { tab ->
                            TabItem(
                                tab = tab,
                                isActive = tab.tabId == activeTabId,
                                onSelect = { viewModel.switchTab(tab.tabId) },
                                onClose = { viewModel.closeTab(tab.tabId) }
                            )
                        }
                    }
                }

                // Keyboard Helper Card when triggered
                if (showKeyboardHelper) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Keyboard Shortcuts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("• Space / K: Play/Pause", style = MaterialTheme.typography.labelSmall)
                            Text("• J / L: -10s / +10s", style = MaterialTheme.typography.labelSmall)
                            Text("• Ctrl+W: Close active tab", style = MaterialTheme.typography.labelSmall)
                            Text("• Ctrl+T: New tab", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ==================== RIGHT / MAIN PANE ====================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Desktop Browser Tab Bar at the top of the right pane with rounded header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DesktopBrowserTabBar(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onSelectTab = { viewModel.switchTab(it) },
                        onCloseTab = { viewModel.closeTab(it) },
                        onNewTab = { viewModel.createNewHomeTab() }
                    )

                    // 2. Thinner Search / Address Bar with "Search or link"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        ThinnerSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChanged(it) },
                            onSearch = { viewModel.submitSearchOrLink(searchQuery) }
                        )
                    }
                }
            }

            // 3. Main Content based on Navigation
            when (currentNav) {
                NavSection.SUBSCRIPTIONS -> {
                    SubscriptionsView(
                        viewModel = viewModel,
                        onPlayVideo = { video -> viewModel.openVideoInTab(video, activateImmediately = true) },
                        onOpenInNewTab = { video -> viewModel.openVideoInTab(video, activateImmediately = false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                NavSection.NOTIFICATIONS -> {
                    NotificationsView(
                        viewModel = viewModel,
                        onPlayVideo = { video -> viewModel.openVideoInTab(video, activateImmediately = true) },
                        modifier = Modifier.weight(1f)
                    )
                }

                NavSection.ACCOUNT -> {
                    AccountView(
                        viewModel = viewModel,
                        onPlayVideo = { video -> viewModel.openVideoInTab(video, activateImmediately = true) },
                        onOpenInNewTab = { video -> viewModel.openVideoInTab(video, activateImmediately = false) },
                        modifier = Modifier.weight(1f)
                    )
                }

                NavSection.HOME -> {
                    val isPlayingActiveVideo = activeTab != null && activeTab?.videoId?.isNotEmpty() == true

                    if (isPlayingActiveVideo) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Large 16:9 ExoPlayer SurfaceView
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                ExoPlayerView(
                                    player = playbackService?.player,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
        
                                if (isResolvingStream) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.7f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "Resolving stream...",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
        
                            // Video Meta & Details
                            activeTab?.let { tab ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${tab.channelTitle} • SoyTube Background Media Engine",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
        
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.IconButton(
                                            onClick = { viewModel.toggleBackgroundAudio() },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = "Background Audio",
                                                tint = if (backgroundAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        androidx.compose.material3.IconButton(
                                            onClick = {
                                                viewModel.toggleSaveVideo(
                                                    VideoCard(
                                                        videoId = tab.videoId,
                                                        title = tab.title,
                                                        channelTitle = tab.channelTitle,
                                                        thumbnailUrl = tab.thumbnailUrl
                                                    )
                                                )
                                            },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                        ) {
                                            val isSaved = savedVideos.any { it.videoId == tab.videoId }
                                            Icon(
                                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save Video",
                                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.openVideoInTab(
                                                    VideoCard(
                                                        videoId = tab.videoId,
                                                        title = tab.title,
                                                        channelTitle = tab.channelTitle,
                                                        thumbnailUrl = tab.thumbnailUrl
                                                    ),
                                                    activateImmediately = false
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Duplicate Tab")
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
        
                            // Scrollable Content Underneath: Comments and Recommended Feed Side-by-Side
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                // Comments Column
                                Column(
                                    modifier = Modifier
                                        .weight(0.45f)
                                        .fillMaxHeight()
                                        .padding(end = 8.dp)
                                        .testTag("tablet_comments_column")
                                ) {
                                    Text(
                                        text = "Comments (${comments.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
        
                                    if (comments.isEmpty()) {
                                        Text(
                                            text = "No comments available for this stream",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(bottom = 16.dp)
                                        ) {
                                            items(comments, key = { it.commentId }) { comment ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = "@${comment.author}",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = comment.publishedTime,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = comment.text,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
        
                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        
                                // Recommendations / Search Results Column
                                Column(
                                    modifier = Modifier
                                        .weight(0.55f)
                                        .fillMaxHeight()
                                        .padding(start = 8.dp)
                                        .testTag("tablet_feed_column")
                                ) {
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Recommended Videos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
        
                                    if (isFeedLoading) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(bottom = 16.dp)
                                        ) {
                                            items(homeFeed, key = { it.videoId }) { video ->
                                                VideoCardItem(
                                                    video = video,
                                                    onPlayNow = {
                                                        viewModel.openVideoInTab(video, activateImmediately = true)
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
                        }
                    } else {
                        // Standard Home Feed List for empty Home Tab
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
                                    .testTag("tablet_feed_list"),
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Search Results" else "Recommended",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                items(homeFeed, key = { it.videoId }) { video ->
                                    VideoCardItem(
                                        video = video,
                                        onPlayNow = {
                                            viewModel.openVideoInTab(video, activateImmediately = true)
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
            }
        }
    }
}
