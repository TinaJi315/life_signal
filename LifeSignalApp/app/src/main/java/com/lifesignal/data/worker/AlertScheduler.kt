package com.lifesignal.data.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Two-stage alert scheduler
 *
 * After check-in, schedules two countdowns:
 *   1. WARNING_WORK  — triggers after 24 hours (test default: 30 seconds)
 *   2. EMERGENCY_WORK — triggers after 48 hours (test default: 60 seconds)
 *
 * Each new check-in REPLACEs old tasks, restarting the countdown.
 */
object AlertScheduler {
    private const val WARNING_WORK = "warning_alert_work"
    private const val EMERGENCY_WORK = "emergency_alert_work"

    /**
     * Schedule two countdown triggers
     * @param warningDelaySeconds   Warning delay (production: 86400 = 24h)
     * @param emergencyDelaySeconds Emergency delay (production: 172800 = 48h)
     */
    fun scheduleAlerts(
        context: Context,
        warningDelaySeconds: Long = 30,
        emergencyDelaySeconds: Long = 60
    ) {
        val wm = WorkManager.getInstance(context)

        // Phase 1: WARNING
        val warningRequest = OneTimeWorkRequestBuilder<WarningAlertWorker>()
            .setInitialDelay(warningDelaySeconds, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(WARNING_WORK, ExistingWorkPolicy.REPLACE, warningRequest)

        // Phase 2: EMERGENCY
        val emergencyRequest = OneTimeWorkRequestBuilder<EmergencyAlertWorker>()
            .setInitialDelay(emergencyDelaySeconds, TimeUnit.SECONDS)
            .build()
        wm.enqueueUniqueWork(EMERGENCY_WORK, ExistingWorkPolicy.REPLACE, emergencyRequest)
    }

    /**
     * Cancel all alerts (call on sign out)
     */
    fun cancelAllAlerts(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WARNING_WORK)
        wm.cancelUniqueWork(EMERGENCY_WORK)
    }
}
