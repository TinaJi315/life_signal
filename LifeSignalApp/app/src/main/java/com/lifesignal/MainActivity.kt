package com.lifesignal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import com.lifesignal.ui.navigation.LifeSignalAppNavHost
import com.lifesignal.ui.theme.LifeSignalTheme
import com.lifesignal.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channels (Android 8.0+)
        createNotificationChannels()

        // Initialize theme manager
        ThemeManager.init(this)

        setContent {
            val themeMode by ThemeManager.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            LifeSignalTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LifeSignalAppNavHost()
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Warning channel
            val warningChannel = NotificationChannel(
                "lifesignal_warnings",
                "Check-in Overdue Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sent when user misses their check-in time"
            }

            // Emergency channel
            val emergencyChannel = NotificationChannel(
                "lifesignal_emergency",
                "Emergency Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Triggered when user misses check-in for 48 hours"
            }

            manager.createNotificationChannels(listOf(warningChannel, emergencyChannel))
        }
    }
}
