package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 好友请求数据模型
 * 对应前端 App.tsx 中 AddFriendPage 的 "Add" 和 "Suggested" 功能
 *
 * Firestore 集合: friend_requests/{requestId}
 */
data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromName: String = "",
    val toName: String = "",
    val status: String = "pending",    // "pending" | "accepted" | "rejected"
    val relation: String = "",          // 例如 "Cousin", "Neighbor"
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION = "friend_requests"
    }
}
