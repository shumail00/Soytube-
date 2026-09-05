package com.example.data.repository

import com.example.data.local.TabDao
import com.example.data.model.VideoTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TabRepository(private val tabDao: TabDao) {

    val tabsFlow: Flow<List<VideoTab>> = tabDao.getAllTabs()

    suspend fun saveTab(tab: VideoTab) = withContext(Dispatchers.IO) {
        tabDao.insertTab(tab)
    }

    suspend fun updatePlaybackPosition(tabId: String, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        tabDao.updatePlaybackPosition(tabId, positionMs, durationMs)
    }

    suspend fun closeTab(tabId: String) = withContext(Dispatchers.IO) {
        tabDao.deleteTabById(tabId)
    }

    suspend fun getTab(tabId: String): VideoTab? = withContext(Dispatchers.IO) {
        tabDao.getTabById(tabId)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        tabDao.clearAllTabs()
    }
}
