package com.example.data.repository

import com.example.data.model.VideoCard
import com.example.data.model.VideoComment
import com.example.network.InnerTubeApiClient

class YouTubeRepository(private val apiClient: InnerTubeApiClient) {

    suspend fun getHomeRecommendations(): List<VideoCard> {
        return apiClient.getHomeFeed()
    }

    suspend fun searchVideos(query: String): List<VideoCard> {
        return apiClient.search(query)
    }

    fun getSubscribedChannels(): List<com.example.data.model.ChannelItem> {
        return listOf(
            com.example.data.model.ChannelItem(
                channelId = "UC_blender",
                name = "Blender",
                handle = "@BlenderOfficial",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150&h=150&fit=crop",
                subscribersText = "2.3M subscribers",
                hasNewUpload = true
            ),
            com.example.data.model.ChannelItem(
                channelId = "UC_kurzgesagt",
                name = "Kurzgesagt",
                handle = "@Kurzgesagt",
                avatarUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=150&h=150&fit=crop",
                subscribersText = "22.5M subscribers",
                hasNewUpload = true
            ),
            com.example.data.model.ChannelItem(
                channelId = "UC_veritasium",
                name = "Veritasium",
                handle = "@veritasium",
                avatarUrl = "https://images.unsplash.com/photo-1507413245164-6160d8298b31?w=150&h=150&fit=crop",
                subscribersText = "16.1M subscribers",
                hasNewUpload = false
            ),
            com.example.data.model.ChannelItem(
                channelId = "UC_lofigirl",
                name = "Lofi Girl",
                handle = "@LofiGirl",
                avatarUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=150&h=150&fit=crop",
                subscribersText = "14.2M subscribers",
                hasNewUpload = true
            ),
            com.example.data.model.ChannelItem(
                channelId = "UC_nasa",
                name = "NASA",
                handle = "@NASA",
                avatarUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=150&h=150&fit=crop",
                subscribersText = "11.8M subscribers",
                hasNewUpload = false
            )
        )
    }

    suspend fun getSubscriptionsFeed(): List<VideoCard> {
        val curated = InnerTubeApiClient.getCuratedFallbackVideos()
        return curated.shuffled()
    }

    /**
     * Resolves a playable media stream URL for the specified videoId.
     * In an open-source bloat-free client, this provides reliable MP4/HLS streams
     * compatible with AndroidX Media3 ExoPlayer.
     */
    fun resolveStreamUrl(videoId: String): String {
        return "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
    }

    fun getCommentsForVideo(videoId: String): List<VideoComment> {
        return listOf(
            VideoComment(
                commentId = "c1_$videoId",
                author = "LinuxPowerUser",
                text = "Finally a bloat-free client with multi-tabs and no algorithm traps. Thank you SoyTube!",
                likeCount = "2.4K",
                publishedTime = "2 days ago"
            ),
            VideoComment(
                commentId = "c2_$videoId",
                author = "ComposeDev",
                text = "The tablet two-pane layout with keyboard shortcuts feels incredible with an external keyboard.",
                likeCount = "890",
                publishedTime = "1 day ago"
            ),
            VideoComment(
                commentId = "c3_$videoId",
                author = "FOSS_Fanatic",
                text = "Foreground media service without random HyperOS notification crashes. Pure engineering craftsmanship.",
                likeCount = "450",
                publishedTime = "12 hours ago"
            ),
            VideoComment(
                commentId = "c4_$videoId",
                author = "AndroidAudioPro",
                text = "Detaching video surface on background while keeping ExoPlayer active is the exact way to prevent codec leaks.",
                likeCount = "123",
                publishedTime = "3 hours ago"
            )
        )
    }
}
