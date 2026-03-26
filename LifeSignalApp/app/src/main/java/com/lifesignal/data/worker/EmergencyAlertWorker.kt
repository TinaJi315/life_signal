package com.lifesignal.data.worker

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifesignal.data.repository.AuthRepository
import com.lifesignal.data.repository.UserRepository
import com.lifesignal.data.service.CheckInService
import kotlinx.coroutines.flow.firstOrNull

/**
 * 后台紧急呼救 Worker
 * 当用户设定的安全逾期时间归零后自动触发，通过底层直接下发求助短信。
 */
class EmergencyAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val authRepo = AuthRepository()
            val userRepo = UserRepository()
            val checkInService = CheckInService()

            val uid = authRepo.currentUid ?: return Result.success() // 没登录则取消执行

            // 获取最新地理位置
            val lastCheckInResult = checkInService.getLastCheckIn(uid)
            val location = lastCheckInResult.getOrNull()?.location ?: "Unknown Location"
            
            // 组装求救短信（夹带一个假定中心或获取到的经纬度参数来拉起谷歌地图）
            // 在 MVP 版本中我们使用固定的经纬度示例结构或查询来的地址作为地标
            val lat = 39.11
            val lng = -94.62
            val message = "URGENT from LifeSignal: User has missed their safety check-in! Last known location: $location. Map: https://maps.google.com/?q=$lat,$lng"
            
            // 获取数据库内写入好的 Emergency Contacts (用强拉取而非监听流，防止首帧空缓存)
            val contacts = userRepo.getContactsOnce(uid).getOrNull() ?: emptyList()

            if (contacts.isNotEmpty()) {
                val smsManager = SmsManager.getDefault() // Android 原生发短信引擎
                for (contact in contacts) {
                    val phone = contact.phone
                    if (phone.isNotBlank()) {
                        // TODO: 在实机或含通讯模块的模拟器中即刻从后背发送真实验证码短信
                        smsManager.sendTextMessage(contact.phone, null, message, null, null)
                        Log.d("EmergencyAlertWorker", "Sent automated emergency SMS to ${contact.phone}")
                    }
                }
            } else {
                Log.d("EmergencyAlertWorker", "No emergency contacts found for SMS alerting.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("EmergencyAlertWorker", "Failed to execute SMS background trigger", e)
            Result.failure()
        }
    }
}
