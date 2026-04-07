package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Check-in record data model
 * Corresponds to check-in button logic and check-in history display in frontend HomePage
 *
 * Firestore collection: users/{uid}/checkins/{checkinId}
 */
data class CheckIn(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    val status: String = "safe",     // "safe" means checked in
    val location: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    companion object {
        const val COLLECTION = "checkins"
    }
}
