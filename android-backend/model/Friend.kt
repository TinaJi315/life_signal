package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 好友（网络成员）数据模型
 * 对应前端 App.tsx 中的 Member 接口和 NetworkPage 的 Friends 列表 / FriendDetailPage
 *
 * Firestore 集合: users/{uid}/friends/{friendId}
 */
data class Friend(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val status: String = "safe",         // "safe" | "overdue"
    val location: String = "",
    val lastUpdated: String = "",        // 显示用, 例如 "12m ago"
    val imageUrl: String = "",
    val phone: String = "",
    val email: String = "",
    val checkinSuccessRate: String = "", // 例如 "100% Success Rate"
    val memberSince: String = "",        // 例如 "Jan 2024"
    @ServerTimestamp
    val addedAt: Date? = null
) {
    companion object {
        const val COLLECTION = "friends"
    }
}
