package com.example.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.NavSection
import com.example.ui.components.ExoPlayerView
import com.example.ui.components.MiniPlayer
import com.example.ui.components.TabItem
import com.example.ui.components.VideoCardItem
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
    val comments by viewModel.comments.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val playbackService = viewModel.getService()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("phone_layout_scaffold"),
        bottomBar = {
            Column {
                // Bottom-docked mini-player when collapsed
                if (!isPlayerExpanded && activeTab != null) {
                    MiniPlayer(
                        activeTab = activeTab,
                        playbackService = playbackService,
                        onExpand = { viewModel.setPlayerExpanded(true) },
                        onClose = { activeTab?.let { viewModel.closeTab(it.tabId) } }
                    )
                }

                // Standard Material 3 Bottom Navigation Bar
                NavigationBar(
                    modifier = Modifier.testTag("phone_bottom_navigation"),
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentNav == NavSection.HOME,
                        onClick = { viewModel.setNavSection(NavSection.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentNav == NavSection.HISTORY,
                        onClick = { viewModel.setNavSection(NavSection.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        modifier = Modifier.testTag("nav_history")
                    )
                    NavigationBarItem(
                        selected = currentNav == NavSection.BOOKMARKS,
                        onClick = { viewModel.setNavSection(NavSection.BOOKMARKS) },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks") },
                        label = { Text("Bookmarks") },
                        modifier = Modifier.testTag("nav_bookmarks")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Collapsible top player surface
            AnimatedVisibility(
                visible = isPlayerExpanded && activeTab != null,
                enter = expandVertically(animationSpec = SoyTubeMotion.bouncySpring<IntSize>()),
                exit = shrinkVertically(animationSpec = SoyTubeMotion.bouncySpring<IntSize>())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    // ExoPlayer View
                    ExoPlayerView(
                        player = playbackService?.player,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Video Meta Header with collapse chevron
                    activeTab?.let { tab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tab.channelTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setPlayerExpanded(false) },
                                modifier = Modifier.testTag("collapse_player_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Collapse Player"
                                )
                            }
                        }
                    }
                }
            }

            // Top Search & Auth Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("phone_search_input"),
                    placeholder = { Text("Search videos or paste YouTube link...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.showLoginDialog(true) },
                    modifier = Modifier.testTag("phone_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account & Cookies",
                        tint = if (isLoggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Horizontal scrollable Tab Row above video meta container
            if (tabs.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("phone_tab_row"),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs, key = { it.tabId }) { tab ->
                        TabItem(
                            tab = tab,
                            isActive = tab.tabId == activeTabId,
                            onSelect = {
                                viewModel.switchTab(tab.tabId)
                                viewModel.setPlayerExpanded(true)
                            },
                            onClose = { viewModel.closeTab(tab.tabId) }
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            // Content Area based on currentNav
            when (currentNav) {
                NavSection.HOME -> {
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
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // If video is active, show comments section first
                            if (activeTab != null && isPlayerExpanded && comments.isNotEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Comments (${comments.size})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            comments.take(2).forEach { comment ->
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(
                                                        text = "@${comment.author} • ${comment.publishedTime}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
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

                            item {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "Search Results" else "Recommended Videos",
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
                NavSection.HISTORY, NavSection.BOOKMARKS -> {
                    HistoryAndBookmarksView(
                        section = currentNav,
                        onPlayVideo = {
                            viewModel.openVideoInTab(it, activateImmediately = true)
                            viewModel.setPlayerExpanded(true)
                        },
                        onOpenInNewTab = {
                            viewModel.openVideoInTab(it, activateImmediately = false)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
