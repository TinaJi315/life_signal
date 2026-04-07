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
 * 24-hour Warning Worker
 *
 * Triggered when user misses check-in (e.g. over 24 hours):
 * 1. Updates user.status to "warning" in Firestore
 * 2. Sends a local push notification reminding user to check in
 */
class WarningAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "lifesignal_warnings"
        const val CHANNEL_NAME = "Check-in Overdue Reminder"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            val authRepo = AuthRepository()
            val uid = authRepo.currentUid ?: return Result.success()

            // 1. Update Firestore status to "warning"
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

            // 2. Send local push notification
            sendWarningNotification()

            Log.d("WarningAlertWorker", "Warning status activated for uid=$uid")
            Result.success()
        } catch (e: Exception) {
            Log.e("WarningAlertWorker", "Warning logic execution failed", e)
            Result.failure()
        }
    }

    private fun sendWarningNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Check-in overdue warning reminder"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Open app on notification tap
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LifeSignal — Check-in Overdue")
            .setContentText("You missed your check-in. Please open the app and check in immediately, or your emergency contacts will be notified.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You missed your check-in. Please open the app and check in immediately. If the countdown ends without a check-in, the system will automatically send an emergency SMS to your emergency contacts.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
