package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Group data model
 * Corresponds to Groups section in frontend NetworkPage and GroupDetailPage
 *
 * Firestore collection: groups/{groupId}
 */
data class Group(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val avatarUrls: List<String> = emptyList(),   // Member avatar URL list
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION = "groups"
    }
}

/**
 * Group member status, for GroupDetailPage display
 */
data class GroupMemberStatus(
    val name: String = "",
    val status: String = "safe"   // "safe" | "overdue"
)
