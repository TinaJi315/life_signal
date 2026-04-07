package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Check-in settings data model
 * Firestore collection: users/{uid}/settings/check_in
 */
data class CheckInSettings(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    val frequencyHours: Int = 24,
    val gracePeriodMinutes: Int = 60,
    val checkInHour: Int = 10,
    val checkInMinute: Int = 0
) {
    companion object {
        const val COLLECTION = "check_in_settings"
    }
}
