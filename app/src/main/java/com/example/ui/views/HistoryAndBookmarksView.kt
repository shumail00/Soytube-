package com.example.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoCard
import com.example.network.InnerTubeApiClient
import com.example.ui.NavSection
import com.example.ui.components.VideoCardItem

@Composable
fun HistoryAndBookmarksView(
    section: NavSection,
    onPlayVideo: (VideoCard) -> Unit,
    onOpenInNewTab: (VideoCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val sampleVideos = InnerTubeApiClient.getCuratedFallbackVideos()
    val videos = if (section == NavSection.HISTORY) {
        sampleVideos.take(3)
    } else {
        sampleVideos.takeLast(3)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag(if (section == NavSection.HISTORY) "history_view" else "bookmarks_view")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (section == NavSection.HISTORY) "Watch History" else "Saved Bookmarks",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (section == NavSection.HISTORY)
                "Recently watched videos across all active and past sessions"
            else
                "Saved videos stored locally in your offline repository",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (section == NavSection.HISTORY) Icons.Default.History else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text("No items found yet", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(videos, key = { "${section.name}_${it.videoId}" }) { video ->
                    VideoCardItem(
                        video = video,
                        onPlayNow = { onPlayVideo(video) },
                        onOpenInNewTab = { onOpenInNewTab(video) }
                    )
                }
            }
        }
    }
}
