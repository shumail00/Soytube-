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
import com.example.data.model.VideoCard
import com.example.data.model.VideoComment
import com.example.data.model.VideoTab
import com.example.network.InnerTubeApiClient
import com.example.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.regex.Pattern

enum class NavSection {
    HOME,
    HISTORY,
    BOOKMARKS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SoyTubeApplication
    private val tabRepo = app.tabRepository
    private val ytRepo = app.youTubeRepository
    private val securePrefs = app.securePreferences

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

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _isLoginDialogVisible = MutableStateFlow(false)
    val isLoginDialogVisible: StateFlow<Boolean> = _isLoginDialogVisible.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(securePrefs.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Mini-player expansion state on phone
    private val _isPlayerExpanded = MutableStateFlow(true)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

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

    fun setNavSection(section: NavSection) {
        _currentNav.value = section
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    /**
     * Open a video card in a tab. If activateImmediately is true, switches playback to it.
     */
    fun openVideoInTab(video: VideoCard, activateImmediately: Boolean = true) {
        viewModelScope.launch {
            tabMutex.withLock {
                val newTab = VideoTab(
                    videoId = video.videoId,
                    title = video.title,
                    channelTitle = video.channelTitle,
                    thumbnailUrl = video.thumbnailUrl
                )
                tabRepo.saveTab(newTab)
                if (activateImmediately || _activeTabId.value == null) {
                    switchTabInternal(newTab.tabId, autoPlay = true)
                } else {
                    _snackbarMessage.emit("Opened in background tab: ${video.title}")
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
        if (currentActive != null && service != null && service.player.currentMediaItem != null) {
            val currentPos = service.player.currentPosition
            val duration = service.player.duration.coerceAtLeast(0L)
            viewModelScope.launch(Dispatchers.IO) {
                tabRepo.updatePlaybackPosition(currentActive.tabId, currentPos, duration)
            }
        }

        // Clear surface
        service?.detachSurface()

        _activeTabId.value = targetTabId

        // Look up target tab
        viewModelScope.launch {
            val targetTab = tabRepo.getTab(targetTabId) ?: tabs.value.firstOrNull { it.tabId == targetTabId }
            if (targetTab != null && service != null) {
                val streamUrl = ytRepo.resolveStreamUrl(targetTab.videoId)
                if (autoPlay) {
                    service.playTab(targetTab, streamUrl)
                }
                _comments.value = ytRepo.getCommentsForVideo(targetTab.videoId)
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
        val sample = InnerTubeApiClient.getCuratedFallbackVideos().random()
        openVideoInTab(sample, activateImmediately = true)
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
