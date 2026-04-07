package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Friend (network member) data model
 * Corresponds to Member interface in frontend App.tsx, NetworkPage Friends list, and FriendDetailPage
 *
 * Firestore collection: users/{uid}/friends/{friendId}
 */
data class Friend(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val status: String = "safe",         // "safe" | "overdue"
    val location: String = "",
    val lastUpdated: String = "",        // Display use, e.g. "12m ago"
    val imageUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val checkinSuccessRate: String = "", // e.g. "100% Success Rate"
    val memberSince: String = "",        // e.g. "Jan 2024"
    @ServerTimestamp
    val addedAt: Date? = null
) {
    companion object {
        const val COLLECTION = "friends"
    }
}
