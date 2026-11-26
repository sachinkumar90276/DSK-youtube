package com.example

import android.app.Application
import com.example.data.local.DskDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.YouTubeApiService
import com.example.data.remote.YouTubeSyncManager
import com.example.data.repository.DskRepository

class DskYouTubeApp : Application() {
    val database: DskDatabase by lazy { DskDatabase.getInstance(this) }
    val settingsStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val youtubeApi: YouTubeApiService by lazy { YouTubeApiService.create() }
    val youtubeSyncManager: YouTubeSyncManager by lazy { YouTubeSyncManager(database, youtubeApi) }
    val repository: DskRepository by lazy { DskRepository(this, database, settingsStore, youtubeSyncManager) }
}
