package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Emergency incident log data model
 * Automatically created when user misses check-in for 48+ hours, triggering EMERGENCY status
 * Used for Firebase cloud tracking and post-incident auditing
 *
 * Firestore collection: users/{uid}/incidents/{incidentId}
 */
data class EmergencyIncident(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    @ServerTimestamp
    val triggeredAt: Date? = null,
    val lastKnownLatitude: Double = 0.0,
    val lastKnownLongitude: Double = 0.0,
    val lastKnownAddress: String = "",
    val contactsNotified: List<String> = emptyList(),
    val resolved: Boolean = false,
    val resolvedAt: Date? = null
) {
    companion object {
        const val COLLECTION = "incidents"
    }
}
