package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 通知设置数据模型
 * 对应前端 App.tsx 中 NotificationSettingsPage 的设置项
 *
 * Firestore 集合: users/{uid}/settings/notifications
 */
data class NotificationSettings(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    val missedCheckIn: Boolean = true,
    val groupReminder: Boolean = true,
    val newFriend: Boolean = true,
    val systemAlerts: Boolean = true
) {
    companion object {
        const val COLLECTION = "notification_settings"
    }
}
