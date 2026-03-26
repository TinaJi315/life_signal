package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 群组数据模型
 * 对应前端 App.tsx 中 NetworkPage 的 Groups 部分和 GroupDetailPage
 *
 * Firestore 集合: groups/{groupId}
 */
data class Group(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val memberIds: List<String> = emptyList(),
    val memberCount: Int = 0,
    val avatarUrls: List<String> = emptyList(),   // 成员头像 URL 列表
    @ServerTimestamp
    val createdAt: Date? = null
) {
    companion object {
        const val COLLECTION = "groups"
    }
}

/**
 * 群组成员状态，用于 GroupDetailPage 展示
 */
data class GroupMemberStatus(
    val name: String = "",
    val status: String = "safe"   // "safe" | "overdue"
)
