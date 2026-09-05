package com.example.network

import android.util.Log
import com.example.data.local.SecurePreferences
import com.example.data.model.VideoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class InnerTubeApiClient(private val securePreferences: SecurePreferences) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getHomeFeed(): List<VideoCard> = withContext(Dispatchers.IO) {
        val requestBodyJson = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240215.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("browseId", "FEwhat_to_watch")
        }

        val requestBuilder = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .addHeader("X-YouTube-Client-Name", "1")
            .addHeader("X-YouTube-Client-Version", "2.20240215.01.00")

        val cookieHeader = securePreferences.getCookieHeader()
        if (cookieHeader.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookieHeader)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful || bodyString.isEmpty()) {
                Log.w("InnerTubeApiClient", "InnerTube response code: ${response.code}")
                return@withContext getCuratedFallbackVideos()
            }

            val parsedVideos = parseVideoRenderers(JSONObject(bodyString))
            if (parsedVideos.isNotEmpty()) {
                parsedVideos
            } else {
                getCuratedFallbackVideos()
            }
        } catch (e: Exception) {
            Log.e("InnerTubeApiClient", "InnerTube feed request failed, returning fallback catalogue", e)
            getCuratedFallbackVideos()
        }
    }

    suspend fun search(query: String): List<VideoCard> = withContext(Dispatchers.IO) {
        val requestBodyJson = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "WEB")
                    put("clientVersion", "2.20240215.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
        }

        val requestBuilder = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/search")
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .addHeader("X-YouTube-Client-Name", "1")
            .addHeader("X-YouTube-Client-Version", "2.20240215.01.00")

        val cookieHeader = securePreferences.getCookieHeader()
        if (cookieHeader.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookieHeader)
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""
            if (response.isSuccessful && bodyString.isNotEmpty()) {
                val results = parseVideoRenderers(JSONObject(bodyString))
                if (results.isNotEmpty()) return@withContext results
            }
        } catch (e: Exception) {
            Log.e("InnerTubeApiClient", "Search failed", e)
        }

        // Filter fallback catalogue if live query was not reachable
        getCuratedFallbackVideos().filter {
            it.title.contains(query, ignoreCase = true) || it.channelTitle.contains(query, ignoreCase = true)
        }
    }

    private fun parseVideoRenderers(root: JSONObject): List<VideoCard> {
        val results = mutableListOf<VideoCard>()
        findVideoObjects(root, results)
        return results.distinctBy { it.videoId }
    }

    private fun findVideoObjects(node: Any?, results: MutableList<VideoCard>) {
        when (node) {
            is JSONObject -> {
                if (node.has("videoRenderer")) {
                    parseSingleVideoRenderer(node.optJSONObject("videoRenderer"))?.let { results.add(it) }
                }
                if (node.has("compactVideoRenderer")) {
                    parseSingleVideoRenderer(node.optJSONObject("compactVideoRenderer"))?.let { results.add(it) }
                }

                val keys = node.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    findVideoObjects(node.opt(key), results)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    findVideoObjects(node.opt(i), results)
                }
            }
        }
    }

    private fun parseSingleVideoRenderer(obj: JSONObject?): VideoCard? {
        if (obj == null) return null
        val videoId = obj.optString("videoId").takeIf { it.isNotEmpty() } ?: return null

        // Extract Title
        var title = ""
        val titleObj = obj.optJSONObject("title")
        if (titleObj != null) {
            val runs = titleObj.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                title = runs.optJSONObject(0)?.optString("text") ?: ""
            } else {
                title = titleObj.optString("simpleText", "")
            }
        }
        if (title.isEmpty()) title = "Video $videoId"

        // Extract Channel
        var channel = "YouTube Creator"
        val ownerObj = obj.optJSONObject("ownerText") ?: obj.optJSONObject("longBylineText") ?: obj.optJSONObject("shortBylineText")
        if (ownerObj != null) {
            val runs = ownerObj.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                channel = runs.optJSONObject(0)?.optString("text") ?: channel
            }
        }

        // Extract Thumbnail
        var thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val thumbObj = obj.optJSONObject("thumbnail")
        if (thumbObj != null) {
            val thumbs = thumbObj.optJSONArray("thumbnails")
            if (thumbs != null && thumbs.length() > 0) {
                val last = thumbs.optJSONObject(thumbs.length() - 1)
                thumbnailUrl = last?.optString("url") ?: thumbnailUrl
            }
        }

        val duration = obj.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
        val viewCount = obj.optJSONObject("viewCountText")?.optString("simpleText", "") ?: ""
        val publishedTime = obj.optJSONObject("publishedTimeText")?.optString("simpleText", "") ?: ""

        return VideoCard(
            videoId = videoId,
            title = title,
            channelTitle = channel,
            thumbnailUrl = thumbnailUrl,
            duration = duration,
            viewCountText = viewCount,
            publishedTimeText = publishedTime
        )
    }

    companion object {
        fun getCuratedFallbackVideos(): List<VideoCard> {
            return listOf(
                VideoCard(
                    videoId = "aqz-KE-bpKQ",
                    title = "Big Buck Bunny - 4K Open Movie",
                    channelTitle = "Blender Animation Studio",
                    thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&q=80",
                    duration = "10:34",
                    viewCountText = "14M views",
                    publishedTimeText = "3 years ago"
                ),
                VideoCard(
                    videoId = "dQw4w9WgXcQ",
                    title = "Rick Astley - Never Gonna Give You Up (Official Music Video)",
                    channelTitle = "Rick Astley",
                    thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                    duration = "3:33",
                    viewCountText = "1.5B views",
                    publishedTimeText = "14 years ago"
                ),
                VideoCard(
                    videoId = "jNQXAC9IVRw",
                    title = "Me at the zoo - The First Video on YouTube",
                    channelTitle = "jawed",
                    thumbnailUrl = "https://images.unsplash.com/photo-1534567153574-2b12153a87f0?w=800&q=80",
                    duration = "0:19",
                    viewCountText = "310M views",
                    publishedTimeText = "19 years ago"
                ),
                VideoCard(
                    videoId = "e-ORhEE9VVg",
                    title = "Sintel - Open Source CGI Short Film",
                    channelTitle = "Blender Foundation",
                    thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&q=80",
                    duration = "15:22",
                    viewCountText = "18M views",
                    publishedTimeText = "5 years ago"
                ),
                VideoCard(
                    videoId = "YE7VzlLtp-4",
                    title = "Tears of Steel - High FPS Sci-Fi VFX",
                    channelTitle = "Blender VFX Team",
                    thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&q=80",
                    duration = "12:14",
                    viewCountText = "7.2M views",
                    publishedTimeText = "4 years ago"
                ),
                VideoCard(
                    videoId = "L_LUpnjgPso",
                    title = "Lofi Hip Hop Radio - Beats to Relax/Study to",
                    channelTitle = "Lofi Girl",
                    thumbnailUrl = "https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=800&q=80",
                    duration = "LIVE",
                    viewCountText = "35K watching",
                    publishedTimeText = "Streamed live"
                )
            )
        }
    }
}
