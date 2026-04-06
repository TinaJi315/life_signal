package com.lifesignal.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lifesignal.R
import com.lifesignal.data.model.EmergencyIncident
import com.lifesignal.data.model.User
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * 48 小时紧急呼救 Worker
 *
 * 当用户超过 48 小时未签到时自动触发：
 * 1. 将 Firestore 中的 user.status 更新为 "emergency"
 * 2. 获取真实 GPS 坐标（回退至 Firestore 中的 geoPoint）
 * 3. 向所有紧急联系人发送包含位置链接的短信
 * 4. 在 Firestore 中创建 EmergencyIncident 事件日志
 * 5. 发送本地高优先级通知
 */
class EmergencyAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "lifesignal_emergency"
        const val CHANNEL_NAME = "紧急警报"
        const val NOTIFICATION_ID = 1002
    }

    override suspend fun doWork(): Result {
        return try {
            val authRepo = AuthRepository()
            val userRepo = UserRepository()
            val firestore = FirebaseFirestore.getInstance()

            val uid = authRepo.currentUid ?: return Result.success()

            // 1. 更新状态为 "emergency"
            val statusUpdates = mapOf(
                "status" to "emergency",
                "updatedAt" to Date()
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .set(statusUpdates, SetOptions.merge())
                .await()

            // 2. 获取用户文档（取名字和已有位置）
            val userDoc = firestore.collection(User.COLLECTION)
                .document(uid)
                .get()
                .await()
            val userName = userDoc.getString("name") ?: "LifeSignal 用户"
            val storedGeoPoint = userDoc.getGeoPoint("geoPoint")
            val storedAddress = userDoc.getString("location") ?: ""

            // 3. 尝试获取真实设备 GPS 坐标
            var lat = storedGeoPoint?.latitude ?: 0.0
            var lng = storedGeoPoint?.longitude ?: 0.0
            var address = storedAddress

            if (hasLocationPermission()) {
                try {
                    val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
                    val deviceLocation = fusedClient.lastLocation.await()
                    if (deviceLocation != null) {
                        lat = deviceLocation.latitude
                        lng = deviceLocation.longitude
                        address = if (storedAddress.isNotBlank()) storedAddress else "GPS: $lat, $lng"
                    }
                } catch (e: Exception) {
                    Log.w("EmergencyAlertWorker", "无法获取设备位置，使用 Firestore 缓存", e)
                }
            }

            // 4. 组装求救短信
            val mapLink = "https://maps.google.com/?q=$lat,$lng"
            val message = "\uD83D\uDEA8 LifeSignal 紧急警报：$userName 已超过 48 小时未签到！\n最后已知位置：$address\n地图：$mapLink"

            // 5. 获取紧急联系人并发送短信
            val contacts = userRepo.getContactsOnce(uid).getOrNull() ?: emptyList()
            val notifiedPhones = mutableListOf<String>()

            if (contacts.isNotEmpty()) {
                val smsManager = SmsManager.getDefault()
                for (contact in contacts) {
                    val phone = contact.phone
                    if (phone.isNotBlank()) {
                        try {
                            // 长短信拆分发送
                            val parts = smsManager.divideMessage(message)
                            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                            notifiedPhones.add(phone)
                            Log.d("EmergencyAlertWorker", "已发送紧急短信至 $phone")
                        } catch (e: Exception) {
                            Log.e("EmergencyAlertWorker", "发送短信至 $phone 失败", e)
                        }
                    }
                }
            } else {
                Log.w("EmergencyAlertWorker", "未找到紧急联系人，无法发送短信")
            }

            // 6. Firebase 事件日志
            val incident = mapOf(
                "userUid" to uid,
                "triggeredAt" to Date(),
                "lastKnownLatitude" to lat,
                "lastKnownLongitude" to lng,
                "lastKnownAddress" to address,
                "contactsNotified" to notifiedPhones,
                "resolved" to false,
                "resolvedAt" to null
            )
            firestore.collection(User.COLLECTION)
                .document(uid)
                .collection(EmergencyIncident.COLLECTION)
                .add(incident)
                .await()
            Log.d("EmergencyAlertWorker", "紧急事件已记录至 Firebase")

            // 7. 发送本地紧急通知
            sendEmergencyNotification()

            Result.success()
        } catch (e: Exception) {
            Log.e("EmergencyAlertWorker", "紧急呼救执行失败", e)
            Result.failure()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendEmergencyNotification() {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "紧急警报 — 已通知紧急联系人"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("\uD83D\uDEA8 LifeSignal — 紧急警报已触发")
            .setContentText("您的紧急联系人已收到自动警报短信。请立即签到以解除紧急状态。")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("由于您超过 48 小时未签到，系统已自动向所有紧急联系人发送包含您位置信息的警报短信。请立即打开应用签到以解除紧急状态。")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
