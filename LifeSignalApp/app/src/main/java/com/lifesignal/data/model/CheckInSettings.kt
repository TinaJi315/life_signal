package com.lifesignal.data.model

import com.google.firebase.firestore.DocumentId

/**
 * 签到设置数据模型
 * Firestore 集合: users/{uid}/settings/check_in
 */
data class CheckInSettings(
    @DocumentId
    val id: String = "",
    val userUid: String = "",
    val frequencyHours: Int = 24,
    val gracePeriodMinutes: Int = 60
) {
    companion object {
        const val COLLECTION = "check_in_settings"
    }
}
