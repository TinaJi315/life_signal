package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * User data model
 * Corresponds to user profile in frontend App.tsx and ProfilePage / HomePage components
 *
 * Firestore collection: users/{uid}
 */
data class User(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val isCheckedIn: Boolean = false,
    val status: String = "safe",         // "safe" | "warning" | "emergency"
    val location: String = "",
    val lastCheckInTime: Date? = null,
    val nextCheckInTime: Date? = null,
    val memberSince: Date? = null,
    val shareUrl: String = "",            // For QR code sharing
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    companion object {
        const val COLLECTION = "users"
    }
}
