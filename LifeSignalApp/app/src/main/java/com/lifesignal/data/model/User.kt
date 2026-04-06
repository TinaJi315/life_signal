package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 用户数据模型
 * 对应前端 App.tsx 中的用户个人资料和 ProfilePage / HomePage 组件
 *
 * Firestore 集合: users/{uid}
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
    val shareUrl: String = "",            // 用于 QR 二维码分享
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
) {
    companion object {
        const val COLLECTION = "users"
    }
}
