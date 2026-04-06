package com.lifesignal.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lifesignal.R
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import java.util.Date

/**
 * 24 小时警告 Worker
 *
 * 当用户超过 24 小时未签到时触发：
 * 1. 将 Firestore 中的 user.status 更新为 "warning"
 * 2. 发送本地推送通知，提醒用户尽快签到
 */
class WarningAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "lifesignal_warnings"
        const val CHANNEL_NAME = "签到逾期提醒"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            val authRepo = AuthRepository()
            val uid = authRepo.currentUid ?: return Result.success()

            // 1. 更新 Firestore 状态为 "warning"
            val firestore = FirebaseFirestore.getInstance()
            val updates = mapOf(
                "status" to "warning",
                "updatedAt" to Date()
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("WarningAlertWorker", "Failed to update status", e)
                }

            // 2. 发送本地推送通知
            sendWarningNotification()

            Log.d("WarningAlertWorker", "Warning state activated for uid=$uid")
            Result.success()
        } catch (e: Exception) {
            Log.e("WarningAlertWorker", "Failed to execute warning alert", e)
            Result.failure()
        }
    }

    private fun sendWarningNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 创建通知渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "签到超时警告提醒"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 点击通知打开 App
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LifeSignal — 签到已逾期")
            .setContentText("您已超过 24 小时未签到。请立即打开应用签到，否则紧急联系人将收到警报。")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("您已超过 24 小时未签到。请立即打开应用签到，否则紧急联系人将在倒计时结束后收到自动警报短信。")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
