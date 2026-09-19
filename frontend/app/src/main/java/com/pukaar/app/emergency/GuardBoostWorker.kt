package com.pukaar.app.emergency

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pukaar.app.data.local.SessionStore

/** Re-arm hardware listeners from background — never start FGS here (Android 12+ crash). */
class GuardBoostWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (SessionStore(applicationContext).token() != null) {
                HardwareReceiverRegistry.register(applicationContext)
                if (PhoneUsageTracker.isEnabled(applicationContext)) {
                    PhoneUsageTracker.arm(applicationContext)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Guard boost failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PukaarGuardBoost"
        private const val UNIQUE = "pukaar_guard_boost"

        fun kick(context: Context) {
            runCatching {
                val req = OneTimeWorkRequestBuilder<GuardBoostWorker>()
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
