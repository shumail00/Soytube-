package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SoyTubeApplication
import com.example.data.model.ChannelItem
import com.example.data.model.StreamData
import com.example.data.model.VideoCard
import com.example.data.model.VideoComment
import com.example.data.model.VideoTab
import com.example.data.repository.StreamRepository
import com.example.network.InnerTubeApiClient
import com.example.player.PlaybackService
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.regex.Pattern

enum class NavSection {
    HOME,
    SUBSCRIPTIONS,
    NOTIFICATIONS,
    ACCOUNT
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SoyTubeApplication
    private val tabRepo = app.tabRepository
    private val ytRepo = app.youTubeRepository
    private val streamRepo = app.streamRepository
    private val notifRepo = app.notificationRepository
    private val securePrefs = app.securePreferences

    val notifications = notifRepo.notifications
    val unreadNotificationCount: StateFlow<Int> = notifications
        .map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun markNotificationRead(id: String) { notifRepo.markAsRead(id) }
    fun markAllNotificationsRead() { notifRepo.markAllAsRead() }
    fun deleteNotification(id: String) { notifRepo.deleteNotification(id) }
    fun clearAllNotifications() { notifRepo.clearAll() }
    fun sendTestNotification() {
        val n = notifRepo.sendTestNotification()
        viewModelScope.launch {
            _snackbarMessage.emit("Dispatched system notification: ${n.title}")
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        val newComment = VideoComment(
            commentId = java.util.UUID.randomUUID().toString(),
            author = "You",
            text = text.trim(),
            likeCount = "1",
            publishedTime = "Just now"
        )
        _comments.value = listOf(newComment) + _comments.value
        viewModelScope.launch {
            _snackbarMessage.emit("Comment posted")
        }
    }

    private val tabMutex = Mutex()

    // Service connection to PlaybackService
    private var playbackService: PlaybackService? = null
    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val tabs: StateFlow<List<VideoTab>> = tabRepo.tabsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeTab: StateFlow<VideoTab?> = combine(tabs, _activeTabId) { tabList, activeId ->
        tabList.firstOrNull { it.tabId == activeId } ?: tabList.lastOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _homeFeed = MutableStateFlow<List<VideoCard>>(emptyList())
    val homeFeed: StateFlow<List<VideoCard>> = _homeFeed.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _comments = MutableStateFlow<List<VideoComment>>(emptyList())
    val comments: StateFlow<List<VideoComment>> = _comments.asStateFlow()

    private val _currentNav = MutableStateFlow(NavSection.HOME)
    val currentNav: StateFlow<NavSection> = _currentNav.asStateFlow()

    // Subscriptions State
    private val _subscribedChannels = MutableStateFlow<List<ChannelItem>>(ytRepo.getSubscribedChannels())
    val subscribedChannels: StateFlow<List<ChannelItem>> = _subscribedChannels.asStateFlow()

    private val _subscriptionsFeed = MutableStateFlow<List<VideoCard>>(emptyList())
    val subscriptionsFeed: StateFlow<List<VideoCard>> = _subscriptionsFeed.asStateFlow()

    // Account & History State
    private val _historyVideos = MutableStateFlow<List<VideoCard>>(InnerTubeApiClient.getCuratedFallbackVideos().take(4))
    val historyVideos: StateFlow<List<VideoCard>> = _historyVideos.asStateFlow()

    private val _savedVideos = MutableStateFlow<List<VideoCard>>(InnerTubeApiClient.getCuratedFallbackVideos().takeLast(3))
    val savedVideos: StateFlow<List<VideoCard>> = _savedVideos.asStateFlow()

    private val _qualityPreference = MutableStateFlow("1080p FHD")
    val qualityPreference: StateFlow<String> = _qualityPreference.asStateFlow()

    private val _backgroundAudioEnabled = MutableStateFlow(true)
    val backgroundAudioEnabled: StateFlow<Boolean> = _backgroundAudioEnabled.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _isLoginDialogVisible = MutableStateFlow(false)
    val isLoginDialogVisible: StateFlow<Boolean> = _isLoginDialogVisible.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(securePrefs.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(true)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _isResolvingStream = MutableStateFlow(false)
    val isResolvingStream: StateFlow<Boolean> = _isResolvingStream.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlaybackService.LocalBinder) {
                playbackService = service.getService()
                _isServiceBound.value = true

                // Synchronize if active tab was set
                activeTab.value?.let { current ->
                    if (playbackService?.player?.currentMediaItem == null) {
                        switchTabInternal(current.tabId, autoPlay = false)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindPlaybackService()
        loadHomeFeed()
        loadSubscriptionsFeed()
    }

    private fun bindPlaybackService() {
        val intent = Intent(app, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_LOCAL_BIND
        }
        app.startService(intent)
        app.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun loadHomeFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            _isFeedLoading.value = true
            val results = ytRepo.getHomeRecommendations()
            _homeFeed.value = results
            _isFeedLoading.value = false
        }
    }

    fun loadSubscriptionsFeed() {
        viewModelScope.launch(Dispatchers.IO) {
            val subs = ytRepo.getSubscriptionsFeed()
            _subscriptionsFeed.value = subs
        }
    }

    fun toggleSubscribe(channelId: String) {
        val current = _subscribedChannels.value
        _subscribedChannels.value = current.map { channel ->
            if (channel.channelId == channelId) {
                val newState = !channel.isSubscribed
                viewModelScope.launch {
                    _snackbarMessage.emit(if (newState) "Subscribed to ${channel.name}" else "Unsubscribed from ${channel.name}")
                }
                channel.copy(isSubscribed = newState)
            } else {
                channel
            }
        }
    }

    fun clearHistory() {
        _historyVideos.value = emptyList()
        viewModelScope.launch {
            _snackbarMessage.emit("Watch history cleared")
        }
    }

    fun removeFromHistory(videoId: String) {
        _historyVideos.value = _historyVideos.value.filter { it.videoId != videoId }
    }

    fun toggleSaveVideo(video: VideoCard) {
        val current = _savedVideos.value
        val exists = current.any { it.videoId == video.videoId }
        if (exists) {
            _savedVideos.value = current.filter { it.videoId != video.videoId }
            viewModelScope.launch { _snackbarMessage.emit("Removed from Saved") }
        } else {
            _savedVideos.value = listOf(video) + current
            viewModelScope.launch { _snackbarMessage.emit("Saved to Account library") }
        }
    }

    fun setQualityPreference(quality: String) {
        _qualityPreference.value = quality
        viewModelScope.launch {
            _snackbarMessage.emit("Stream quality set to $quality")
        }
    }

    fun toggleBackgroundAudio() {
        val newState = !_backgroundAudioEnabled.value
        _backgroundAudioEnabled.value = newState
        viewModelScope.launch {
            _snackbarMessage.emit(if (newState) "Background audio playback enabled" else "Background audio playback disabled")
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank()) {
            _isSearching.value = false
            loadHomeFeed()
        } else {
            _isSearching.value = true
            viewModelScope.launch(Dispatchers.IO) {
                _isFeedLoading.value = true
                val searchResults = ytRepo.searchVideos(newQuery)
                _homeFeed.value = searchResults
                _isFeedLoading.value = false
            }
        }
    }

    fun submitSearchOrLink(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return

        val detectedVideoId = extractYouTubeVideoId(trimmed)
        if (detectedVideoId != null) {
            // Direct URL paste -> Open immediately in new/active tab
            val card = VideoCard(
                videoId = detectedVideoId,
                title = "Video ($detectedVideoId)",
                channelTitle = "YouTube Direct Link",
                thumbnailUrl = "https://i.ytimg.com/vi/$detectedVideoId/hqdefault.jpg"
            )
            openVideoInTab(card, activateImmediately = true)
            setPlayerExpanded(true)
            _searchQuery.value = ""
            _isSearching.value = false
            viewModelScope.launch {
                _snackbarMessage.emit("Opened link: $detectedVideoId")
            }
        } else {
            onSearchQueryChanged(trimmed)
        }
    }

    fun setNavSection(section: NavSection) {
        _currentNav.value = section
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    /**
     * Create a new Home tab when user presses '+' or triggers new home tab.
     */
    fun createNewHomeTab() {
        viewModelScope.launch {
            tabMutex.withLock {
                val homeTab = VideoTab(
                    videoId = "",
                    title = "Home",
                    channelTitle = "Explore",
                    thumbnailUrl = ""
                )
                tabRepo.saveTab(homeTab)
                _activeTabId.value = homeTab.tabId
                _currentNav.value = NavSection.HOME
                _isPlayerExpanded.value = false
                _searchQuery.value = ""
                _isSearching.value = false
                _snackbarMessage.emit("Opened new Home tab")
            }
        }
    }

    /**
     * Open a video card in a tab. If activateImmediately is true, switches playback to it.
     * If the current active tab is an empty Home tab, reuses it to load the video.
     */
    fun openVideoInTab(video: VideoCard, activateImmediately: Boolean = true) {
        viewModelScope.launch {
            // Update history
            val currentHistory = _historyVideos.value.filter { it.videoId != video.videoId }
            _historyVideos.value = listOf(video) + currentHistory

            tabMutex.withLock {
                val currentActive = activeTab.value
                if (currentActive != null && currentActive.videoId.isEmpty() && activateImmediately) {
                    val updatedTab = currentActive.copy(
                        videoId = video.videoId,
                        title = video.title,
                        channelTitle = video.channelTitle,
                        thumbnailUrl = video.thumbnailUrl
                    )
                    tabRepo.saveTab(updatedTab)
                    switchTabInternal(updatedTab.tabId, autoPlay = true)
                    _isPlayerExpanded.value = true
                } else {
                    val newTab = VideoTab(
                        videoId = video.videoId,
                        title = video.title,
                        channelTitle = video.channelTitle,
                        thumbnailUrl = video.thumbnailUrl
                    )
                    tabRepo.saveTab(newTab)
                    if (activateImmediately || _activeTabId.value == null) {
                        switchTabInternal(newTab.tabId, autoPlay = true)
                        _isPlayerExpanded.value = true
                    } else {
                        _snackbarMessage.emit("Opened in background tab: ${video.title}")
                    }
                }
            }
        }
    }

    /**
     * Switch to target tab safely.
     * Captures player.currentPosition for the outgoing tab, saves it to Room DB,
     * clears the surface, sets incoming tab media item, seeks to saved position, and prepares.
     */
    fun switchTab(targetTabId: String) {
        viewModelScope.launch {
            tabMutex.withLock {
                switchTabInternal(targetTabId, autoPlay = true)
            }
        }
    }

    private fun switchTabInternal(targetTabId: String, autoPlay: Boolean = true) {
        val currentActive = activeTab.value
        val service = playbackService

        // Capture outgoing tab position
        if (currentActive != null && service != null && service.player.currentMediaItem != null && currentActive.videoId.isNotEmpty()) {
            val currentPos = service.player.currentPosition
            val duration = service.player.duration.coerceAtLeast(0L)
            viewModelScope.launch(Dispatchers.IO) {
                tabRepo.updatePlaybackPosition(currentActive.tabId, currentPos, duration)
            }
        }

        _activeTabId.value = targetTabId

        // Look up target tab and resolve stream
        viewModelScope.launch {
            val targetTab = tabRepo.getTab(targetTabId) ?: tabs.value.firstOrNull { it.tabId == targetTabId }
            if (targetTab != null) {
                if (targetTab.videoId.isEmpty()) {
                    // Empty Home Tab
                    service?.pause()
                    _isResolvingStream.value = false
                    _isPlayerExpanded.value = false
                    _currentNav.value = NavSection.HOME
                    return@launch
                }

                if (service != null) {
                    _streamError.value = null
                    _isResolvingStream.value = true

                    // Use StreamRepository to resolve media stream with resilient fallback
                    val streamResult = streamRepo.getStreamUrl(targetTab.videoId)
                    _isResolvingStream.value = false

                    var resolvedUrl = ""
                    var finalTab: VideoTab = targetTab

                    streamResult.onSuccess { streamData ->
                        resolvedUrl = streamData.bestVideoStreamUrl
                        // Update tab metadata if enriched from extraction
                        if (streamData.title.isNotEmpty() && streamData.title != targetTab.title) {
                            val updated = targetTab.copy(
                                title = streamData.title,
                                channelTitle = streamData.channelName.ifEmpty { targetTab.channelTitle }
                            )
                            tabRepo.saveTab(updated)
                            finalTab = updated
                        }
                    }.onFailure {
                        // Resilient fallback: use InnerTube/verified fallback stream
                        resolvedUrl = ytRepo.resolveStreamUrl(targetTab.videoId)
                    }

                    if (resolvedUrl.isEmpty()) {
                        resolvedUrl = ytRepo.resolveStreamUrl(targetTab.videoId)
                    }

                    if (autoPlay) {
                        service.playTab(finalTab, resolvedUrl)
                    }
                    _comments.value = ytRepo.getCommentsForVideo(targetTab.videoId)
                }
            }
        }
    }

    /**
     * Close a tab. If closing the currently active tab, switches to the next tab or stops.
     */
    fun closeTab(tabId: String) {
        viewModelScope.launch {
            tabMutex.withLock {
                val currentTabs = tabs.value
                val isClosingActive = _activeTabId.value == tabId

                tabRepo.closeTab(tabId)

                if (isClosingActive) {
                    val remaining = currentTabs.filter { it.tabId != tabId }
                    if (remaining.isNotEmpty()) {
                        val nextTab = remaining.last()
                        switchTabInternal(nextTab.tabId, autoPlay = true)
                    } else {
                        playbackService?.let {
                            it.player.stop()
                            it.player.clearMediaItems()
                            it.detachSurface()
                        }
                        _activeTabId.value = null
                    }
                }
                _snackbarMessage.emit("Tab closed")
            }
        }
    }

    /**
     * Parse shared YouTube link from external app SEND intent or VIEW intent.
     * Appends a new tab asynchronously without disrupting currently playing audio.
     */
    fun handleSharedContent(rawContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val videoId = extractYouTubeVideoId(rawContent)
            if (videoId != null) {
                val tab = VideoTab(
                    videoId = videoId,
                    title = "Shared Video ($videoId)",
                    channelTitle = "YouTube Shared Link",
                    thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                )
                tabRepo.saveTab(tab)
                _snackbarMessage.emit("Added shared video to tabs: $videoId")
            } else {
                _snackbarMessage.emit("Could not parse YouTube link")
            }
        }
    }

    private fun extractYouTubeVideoId(text: String): String? {
        val patterns = listOf(
            Pattern.compile("youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("v=([a-zA-Z0-9_-]{11})"),
            Pattern.compile("embed/([a-zA-Z0-9_-]{11})"),
            Pattern.compile("shorts/([a-zA-Z0-9_-]{11})")
        )
        for (p in patterns) {
            val matcher = p.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        // If text is strictly 11 alphanumeric characters
        if (text.trim().matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return text.trim()
        }
        return null
    }

    // Keyboard Shortcuts Support
    fun togglePlayPause() {
        playbackService?.let {
            if (it.player.isPlaying) it.pause() else it.play()
        }
    }

    fun seekRelative(offsetMs: Long) {
        playbackService?.let {
            val newPos = (it.player.currentPosition + offsetMs).coerceIn(0L, it.player.duration.coerceAtLeast(0L))
            it.seekTo(newPos)
        }
    }

    fun closeActiveTabShortcut() {
        _activeTabId.value?.let { closeTab(it) }
    }

    fun createNewTabShortcut() {
        createNewHomeTab()
    }

    // Auth & Cookie interception
    fun showLoginDialog(show: Boolean) {
        _isLoginDialogVisible.value = show
    }

    fun onCookiesExtracted(cookies: Map<String, String>) {
        securePrefs.saveCookies(cookies)
        _isLoggedIn.value = securePrefs.isLoggedIn()
        _isLoginDialogVisible.value = false
        viewModelScope.launch {
            _snackbarMessage.emit("Signed in to YouTube! Refreshing personalized feed...")
            loadHomeFeed()
        }
    }

    fun logout() {
        securePrefs.clearAuth()
        _isLoggedIn.value = false
        viewModelScope.launch {
            _snackbarMessage.emit("Signed out")
            loadHomeFeed()
        }
    }

    fun getService(): PlaybackService? = playbackService

    override fun onCleared() {
        try {
            app.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Ignore
        }
        super.onCleared()
    }
}
