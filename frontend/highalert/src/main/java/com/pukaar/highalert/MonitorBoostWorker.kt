package com.pukaar.highalert

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/** Immediate one-shot wake after boot, login, or network restore. */
class MonitorBoostWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val token = AlertSession(applicationContext).token()
            if (!token.isNullOrBlank()) {
                PendingAlertChecker.checkAndFire(applicationContext)
                AlertMonitorService.startGuard(applicationContext)
                MonitorWatchdogReceiver.schedule(applicationContext)
                FcmRegistrar.refreshAndRegister(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Boost failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HighAlertBoost"
        private const val UNIQUE = "highalert_boost"

        fun kick(context: Context) {
            runCatching {
                val req = OneTimeWorkRequestBuilder<MonitorBoostWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
            }.onFailure {
                Log.w(TAG, "Kick failed: ${it.message}")
            }
        }
    }
}
