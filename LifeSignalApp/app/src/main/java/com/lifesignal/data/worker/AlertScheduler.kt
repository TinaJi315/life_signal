package com.lifesignal.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AlertScheduler {
    private const val WORK_NAME = "emergency_alert_work"

    /**
     * 埋下倒计时触发器：
     * 为了实机肉眼验收测试，我们将原本可能长达 24 小时的倒计时缩短至秒级（如 30 秒）。
     * 只要 30 秒内你没有再次打卡替换掉它，它就会准时发送求救短信。
     */
    fun scheduleAlert(context: Context, delaySeconds: Long = 30) {
        val request = OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        
        // 使用 REPLACE 策略：如果用户及时签到再次调用这个方法，旧的倒计时就会被抹除并重置！
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAlert(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
