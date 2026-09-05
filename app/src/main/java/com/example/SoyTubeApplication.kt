package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferences
import com.example.data.repository.TabRepository
import com.example.data.repository.YouTubeRepository
import com.example.network.InnerTubeApiClient

class SoyTubeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var tabRepository: TabRepository
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var youTubeRepository: YouTubeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        tabRepository = TabRepository(database.tabDao())
        securePreferences = SecurePreferences(this)
        val apiClient = InnerTubeApiClient(securePreferences)
        youTubeRepository = YouTubeRepository(apiClient)
    }
}
