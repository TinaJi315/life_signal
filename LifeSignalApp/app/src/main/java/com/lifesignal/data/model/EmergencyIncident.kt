package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * 紧急事件日志数据模型
 * 当用户超过 48 小时未签到触发 EMERGENCY 状态时，系统自动创建此记录
 * 用于 Firebase 云端追踪和事后审计
 *
 * Firestore 集合: users/{uid}/incidents/{incidentId}
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
