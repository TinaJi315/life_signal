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
import androidx.compose.ui.Modifier
import com.lifesignal.ui.navigation.LifeSignalAppNavHost
import com.lifesignal.ui.theme.LifeSignalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建通知渠道（Android 8.0+）
        createNotificationChannels()

        setContent {
            LifeSignalTheme {
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

            // 警告渠道
            val warningChannel = NotificationChannel(
                "lifesignal_warnings",
                "签到逾期提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "超过 24 小时未签到时发送提醒"
            }

            // 紧急渠道
            val emergencyChannel = NotificationChannel(
                "lifesignal_emergency",
                "紧急警报",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "超过 48 小时未签到时触发紧急警报"
            }

            manager.createNotificationChannels(listOf(warningChannel, emergencyChannel))
        }
    }
}
