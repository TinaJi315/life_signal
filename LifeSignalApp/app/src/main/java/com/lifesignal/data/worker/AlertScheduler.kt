package com.lifesignal.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 双阶段警报调度器
 *
 * 签到后同时埋下两个倒计时：
 *   1. WARNING_WORK  — 24 小时后触发（测试默认 30 秒）
 *   2. EMERGENCY_WORK — 48 小时后触发（测试默认 60 秒）
 *
 * 每次新签到都会 REPLACE 旧任务，重新开始倒计时。
 */
object AlertScheduler {
    private const val WARNING_WORK = "warning_alert_work"
    private const val EMERGENCY_WORK = "emergency_alert_work"

    /**
     * 埋下双倒计时触发器
     * @param warningDelaySeconds   警告延迟（生产环境 86400 = 24h）
     * @param emergencyDelaySeconds 紧急延迟（生产环境 172800 = 48h）
     */
    fun scheduleAlerts(
        context: Context,
        warningDelaySeconds: Long = 30,
        emergencyDelaySeconds: Long = 60
    ) {
        val wm = WorkManager.getInstance(context)

        // 阶段一：WARNING
        val warningRequest = OneTimeWorkRequestBuilder<WarningAlertWorker>()
            .setInitialDelay(warningDelaySeconds, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(WARNING_WORK, ExistingWorkPolicy.REPLACE, warningRequest)

        // 阶段二：EMERGENCY
        val emergencyRequest = OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
            .setInitialDelay(emergencyDelaySeconds, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(EMERGENCY_WORK, ExistingWorkPolicy.REPLACE, emergencyRequest)
    }

    /**
     * 取消所有警报（退出登录时调用）
     */
    fun cancelAllAlerts(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WARNING_WORK)
        wm.cancelUniqueWork(EMERGENCY_WORK)
    }
}
