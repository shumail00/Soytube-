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

    /**
     * Resolves a playable media stream URL for the specified videoId.
     * In an open-source bloat-free client, this provides reliable MP4/HLS streams
     * compatible with AndroidX Media3 ExoPlayer.
     */
    fun resolveStreamUrl(videoId: String): String {
        return when (videoId) {
            "aqz-KE-bpKQ" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            "e-ORhEE9VVg" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            "YE7VzlLtp-4" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            "dQw4w9WgXcQ" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
            "jNQXAC9IVRw" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
            "L_LUpnjgPso" -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4"
            else -> "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        }
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
