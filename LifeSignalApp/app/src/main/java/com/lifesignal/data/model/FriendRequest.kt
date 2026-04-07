package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Friend request data model
 * Corresponds to "Add" and "Suggested" features on frontend AddFriendPage
 *
 * Firestore collection: friend_requests/{requestId}
 */
data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val toName: String = "",
    val status: String = "pending",    // "pending" | "accepted" | "rejected"
    val relation: String = "",          // e.g. "Cousin", "Neighbor"
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION = "friend_requests"
    }
}
