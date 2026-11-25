package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dskDataStore: DataStore<Preferences> by preferencesDataStore(name = "dsk_youtube_prefs")

data class UserSettings(
    val currentUserId: String? = null,
    val darkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val autoplayNext: Boolean = true,
    val defaultQuality: String = "Auto",
    val restrictedMode: Boolean = false,
    val language: String = "English",
    val privateWatchHistory: Boolean = false
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val RESTRICTED_MODE = booleanPreferencesKey("restricted_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val PRIVATE_WATCH_HISTORY = booleanPreferencesKey("private_watch_history")
    }

    val settingsFlow: Flow<UserSettings> = context.dskDataStore.data.map { prefs ->
        UserSettings(
            currentUserId = prefs[Keys.CURRENT_USER_ID]?.takeIf { it.isNotBlank() },
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            autoplayNext = prefs[Keys.AUTOPLAY_NEXT] ?: true,
            defaultQuality = prefs[Keys.DEFAULT_QUALITY] ?: "Auto",
            restrictedMode = prefs[Keys.RESTRICTED_MODE] ?: false,
            language = prefs[Keys.LANGUAGE] ?: "English",
            privateWatchHistory = prefs[Keys.PRIVATE_WATCH_HISTORY] ?: false
        )
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dskDataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(Keys.CURRENT_USER_ID)
            } else {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dskDataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dskDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setAutoplayNext(enabled: Boolean) {
        context.dskDataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dskDataStore.edit { it[Keys.DEFAULT_QUALITY] = quality }
    }

    suspend fun setRestrictedMode(enabled: Boolean) {
        context.dskDataStore.edit { it[Keys.RESTRICTED_MODE] = enabled }
    }

    suspend fun setLanguage(language: String) {
        context.dskDataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setPrivateWatchHistory(enabled: Boolean) {
        context.dskDataStore.edit { it[Keys.PRIVATE_WATCH_HISTORY] = enabled }
    }
}
