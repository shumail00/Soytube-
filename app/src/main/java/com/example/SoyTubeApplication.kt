package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SecurePreferences
import com.example.data.repository.StreamRepository
import com.example.data.repository.TabRepository
import com.example.data.repository.YouTubeRepository
import com.example.extractor.OkHttpDownloader
import com.example.network.InnerTubeApiClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

class SoyTubeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var tabRepository: TabRepository
        private set

    lateinit var securePreferences: SecurePreferences
        private set

    lateinit var youTubeRepository: YouTubeRepository
        private set

    lateinit var streamRepository: StreamRepository
        private set

    lateinit var notificationRepository: com.example.data.repository.NotificationRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        tabRepository = TabRepository(database.tabDao())
        securePreferences = SecurePreferences(this)
        val apiClient = InnerTubeApiClient(securePreferences)
        youTubeRepository = YouTubeRepository(apiClient)
        streamRepository = StreamRepository()
        notificationRepository = com.example.data.repository.NotificationRepository(this)

        initNewPipeExtractor()
    }

    /**
     * Initializes the NewPipe extractor singleton with our OkHttpDownloader,
     * which injects user-agent and authenticated cookies from SecurePreferences.
     */
    private fun initNewPipeExtractor() {
        try {
            val downloader = OkHttpDownloader(securePreferences)
            val defaultLocale = Locale.getDefault()
            val localization = Localization(defaultLocale.language, defaultLocale.country)
            val country = ContentCountry(defaultLocale.country.ifEmpty { "US" })
            NewPipe.init(downloader, localization, country)
            Log.i("SoyTubeApplication", "NewPipeExtractor initialized successfully with OkHttpDownloader")
        } catch (e: Exception) {
            Log.e("SoyTubeApplication", "Failed to initialize NewPipeExtractor", e)
        }
    }
}
