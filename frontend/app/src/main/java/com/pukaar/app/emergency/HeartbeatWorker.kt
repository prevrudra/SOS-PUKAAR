package com.pukaar.app.emergency

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pukaar.app.data.local.SessionStore
import java.util.concurrent.TimeUnit

/**
 * Backup usage poll + flush pending heartbeats — never fakes activity.
 */
class HeartbeatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (SessionStore(applicationContext).token() != null) {
                HardwareReceiverRegistry.register(applicationContext)
                PhoneUsageTracker.pollUsageStats(applicationContext)
                PhoneUsageTracker.flushPendingHeartbeat(applicationContext)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "pukaar_heartbeat"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
