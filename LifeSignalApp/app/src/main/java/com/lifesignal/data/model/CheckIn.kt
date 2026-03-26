package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 签到记录数据模型
 * 对应前端 App.tsx 中 HomePage 的签到按钮逻辑和签到历史展示
 *
 * Firestore 集合: users/{uid}/checkins/{checkinId}
 */
data class CheckIn(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    val status: String = "safe",     // "safe" 表示已签到
    val location: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
) {
    companion object {
        const val COLLECTION = "checkins"
    }
}
