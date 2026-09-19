package com.pukaar.app.emergency

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pukaar.app.PukaarApp

/** Retry guard FGS + hardware listeners when boot/background start is blocked (Motorola, Android 12+). */
class GuardBoostWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (PukaarApp.instance.sessionStore.token() != null) {
                PukaarGuardService.start(applicationContext, hasSession = true)
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
