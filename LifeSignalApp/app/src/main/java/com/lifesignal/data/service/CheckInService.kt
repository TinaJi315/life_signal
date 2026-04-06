package com.lifesignal.data.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.lifesignal.data.model.CheckIn
import com.lifesignal.data.model.EmergencyIncident
import com.lifesignal.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * 签到服务
 * 处理签到逻辑、签到历史和签到提醒
 * 对应前端 HomePage 的签到按钮和签到状态展示
 */
class CheckInService {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * 执行签到操作
     * 对应前端 HomePage 中的 "Safe" 按钮点击
     * 1. 在用户的 checkins 子集合中新增一条签到记录
     * 2. 更新用户的 isCheckedIn、status 和 lastCheckInTime 字段
     * 3. 计算并设置下次签到时间
     * 4. 如果之前处于 emergency 状态，解决活跃的 incident
     */
    suspend fun checkIn(uid: String, location: String = ""): Result<Unit> {
        return try {
            val now = Date()
            val batch = firestore.batch()

            // 1. 添加签到记录
            val checkIn = CheckIn(
                userUid = uid,
                status = "safe",
                location = location
            )
            val checkInRef = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .document()
            batch.set(checkInRef, checkIn)

            // 2. 更新用户文档
            val calendar = Calendar.getInstance().apply {
                time = now
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 20) // 下次签到默认晚上8点
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            val userUpdates = mapOf(
                "isCheckedIn" to true,
                "status" to "safe",
                "lastCheckInTime" to now,
                "nextCheckInTime" to calendar.time,
                "updatedAt" to now
            )
            val userRef = firestore.collection(User.COLLECTION).document(uid)
            batch.set(userRef, userUpdates, SetOptions.merge())

            batch.commit().await()

            // 3. 解决活跃的紧急事件（如有）
            resolveActiveIncidents(uid)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解决所有未关闭的紧急事件
     * 当用户从 warning/emergency 状态恢复签到时调用
     */
    private suspend fun resolveActiveIncidents(uid: String) {
        try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(EmergencyIncident.COLLECTION)
                .whereEqualTo("resolved", false)
                .get()
                .await()

            val now = Date()
            for (doc in snapshot.documents) {
                doc.reference.update(
                    mapOf(
                        "resolved" to true,
                        "resolvedAt" to now
                    )
                )
            }
        } catch (e: Exception) {
            // 解决 incident 失败不影响签到主流程
            android.util.Log.w("CheckInService", "解决活跃 incident 失败", e)
        }
    }

    /**
     * 获取签到历史记录
     * 对应前端 HomePage 中的 "Last Record" 和 "Previous" 展示
     */
    suspend fun getCheckInHistory(
        uid: String,
        limit: Long = 10
    ): Result<List<CheckIn>> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            val history = snapshot.toObjects(CheckIn::class.java)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取最近一次签到记录
     * 对应前端 HomePage 中的 "Last Check-in" 展示
     */
    suspend fun getLastCheckIn(uid: String): Result<CheckIn?> {
        return try {
            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val lastCheckIn = snapshot.toObjects(CheckIn::class.java).firstOrNull()
            Result.success(lastCheckIn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查用户今天是否已签到
     * 对应前端 HomePage 中判断是否显示 "Checked-in" 还是 "Safe" 按钮
     */
    suspend fun isCheckedInToday(uid: String): Result<Boolean> {
        return try {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .limit(1)
                .get()
                .await()

            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 向群组所有成员发送签到提醒
     * 对应前端 GroupDetailPage 中的 "Remind All Members" 按钮
     * 注意：实际推送通知需要配合 Firebase Cloud Messaging (FCM)
     *       这里仅记录提醒事件到 Firestore
     */
    suspend fun sendGroupReminder(
        groupId: String,
        senderUid: String,
        memberIds: List<String>
    ): Result<Unit> {
        return try {
            val reminder = mapOf(
                "groupId" to groupId,
                "senderUid" to senderUid,
                "memberIds" to memberIds,
                "type" to "check_in_reminder",
                "timestamp" to Date(),
                "read" to false
            )
            firestore.collection("reminders")
                .add(reminder)
                .await()

            // TODO: 集成 FCM 向各成员发送推送通知
            // 可通过 Cloud Functions 触发器，监听 reminders 集合的新文档
            // 然后向各成员的 FCM token 发送推送

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 计算签到成功率
     * 对应前端 FriendDetailPage 中的 "100% Success Rate" 展示
     */
    suspend fun getCheckInSuccessRate(uid: String, days: Int = 30): Result<String> {
        return try {
            val startDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -days)
            }.time

            val snapshot = firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(CheckIn.COLLECTION)
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .get()
                .await()

            val totalCheckIns = snapshot.size()
            val rate = if (days > 0) {
                ((totalCheckIns.toFloat() / days) * 100).coerceAtMost(100f).toInt()
            } else {
                0
            }
            Result.success("${rate}% Success Rate")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
