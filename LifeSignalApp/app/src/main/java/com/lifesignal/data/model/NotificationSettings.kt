package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Notification settings data model
 * Corresponds to settings items on frontend NotificationSettingsPage
 *
 * Firestore collection: users/{uid}/settings/notifications
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
