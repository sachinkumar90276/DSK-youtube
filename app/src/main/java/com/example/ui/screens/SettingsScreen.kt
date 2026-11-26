package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSettings
import com.example.ui.components.*

@Composable
fun SettingsScreen(
    settings: UserSettings,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSetDarkMode: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetAutoplay: (Boolean) -> Unit,
    onSetQuality: (String) -> Unit,
    onSetRestrictedMode: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetPrivateHistory: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        LiquidGlassTopBar(
            title = "Settings",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingToggleRow("Dark Mode", "Charcoal Liquid Glass dark theme", settings.darkMode, onSetDarkMode)
            SettingToggleRow("Notifications", "Alerts for uploads, replies, and likes", settings.notificationsEnabled, onSetNotifications)
            SettingToggleRow("Autoplay Next Video", "Automatically play the next recommended video", settings.autoplayNext, onSetAutoplay)
            SettingToggleRow("Restricted Mode", "Filter potentially mature tagged videos", settings.restrictedMode, onSetRestrictedMode)
            SettingToggleRow("Private Watch History", "Pause recording watch history while viewing", settings.privateWatchHistory, onSetPrivateHistory)

            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                intensity = GlassIntensity.REGULAR,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Default Video Quality", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Auto", "1080p", "720p", "480p").forEach { q ->
                            LiquidGlassChip(
                                selected = settings.defaultQuality == q,
                                onClick = { onSetQuality(q) },
                                label = q
                            )
                        }
                    }
                }
            }

            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                intensity = GlassIntensity.REGULAR,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Language", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("English", "Hindi", "Spanish", "French").forEach { lang ->
                            LiquidGlassChip(
                                selected = settings.language == lang,
                                onClick = { onSetLanguage(lang) },
                                label = lang
                            )
                        }
                    }
                }
            }

            LiquidGlassCard(
                shape = RoundedCornerShape(20.dp),
                intensity = GlassIntensity.SUBTLE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("100% Free Platform & Local Storage Policy", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "DSK YouTube never charges subscription fees or membership dues. Local device uploads are capped at 100 MB per file to protect storage capacity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSignedIn) {
                LiquidGlassButton(
                    onClick = onClearHistory,
                    isPrimary = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Watch History")
                }
                LiquidGlassButton(
                    onClick = {
                        onLogout()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    LiquidGlassCard(
        shape = RoundedCornerShape(20.dp),
        intensity = GlassIntensity.REGULAR,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
