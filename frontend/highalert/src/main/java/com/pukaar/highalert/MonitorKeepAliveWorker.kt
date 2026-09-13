package com.pukaar.highalert

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Backup path when OEM kills the FGS + delays exact alarms.
 * WorkManager can still run periodically and restart monitoring / fire pending SOS.
 */
class MonitorKeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val token = AlertSession(applicationContext).token()
            if (!token.isNullOrBlank()) {
                PendingAlertChecker.checkAndFire(applicationContext)
                AlertMonitorService.start(applicationContext)
                MonitorWatchdogReceiver.schedule(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "KeepAlive failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HighAlertKeepAlive"
        private const val UNIQUE = "highalert_keepalive"

        fun enqueue(context: Context) {
            runCatching {
                val req = PeriodicWorkRequestBuilder<MonitorKeepAliveWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    UNIQUE,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    req
                )
            }.onFailure {
                Log.w(TAG, "Enqueue failed: ${it.message}")
            }
        }

        fun cancel(context: Context) {
            runCatching {
                WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE)
            }
        }
    }
}
