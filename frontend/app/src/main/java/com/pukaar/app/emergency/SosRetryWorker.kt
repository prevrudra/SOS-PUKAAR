package com.pukaar.app.emergency

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.util.DeviceTelemetry
import java.util.concurrent.TimeUnit

/** Retries offline SOS until the server accepts it. */
class SosRetryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appCtx = applicationContext
        if (EmergencySessionStore.serverSynced(appCtx)) return Result.success()
        val localId = EmergencySessionStore.activeEventId(appCtx) ?: return Result.success()
        if (!localId.startsWith("local-") && EmergencySessionStore.serverSynced(appCtx)) {
            return Result.success()
        }
        return try {
            if (PukaarApp.instance.sessionStore.token() == null) return Result.retry()
            val event = PukaarApp.instance.repository.trigger(
                TriggerRequest(
                    triggerType = when {
                        EmergencySessionStore.isMock(appCtx) -> "MOCK_DRILL"
                        EmergencySessionStore.isSos(appCtx) -> "APP"
                        else -> "HELP"
                    },
                    latitude = EmergencySessionStore.latitude(appCtx),
                    longitude = EmergencySessionStore.longitude(appCtx),
                    mockDrill = EmergencySessionStore.isMock(appCtx),
                    batteryPct = DeviceTelemetry.batteryPercent(appCtx),
                    networkType = DeviceTelemetry.networkType(appCtx)
                )
            )
            val id = event.id
            if (id.isNullOrBlank()) return Result.retry()
            EmergencySessionStore.markServerSynced(appCtx, id)
            Log.i(TAG, "Offline SOS synced to server event=$id (FGS resumes when app opens)")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "SOS retry failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PUKAAR_SOS_RETRY"
        private const val UNIQUE = "pukaar_sos_retry"

        fun enqueue(context: Context) {
            runCatching {
                val req = OneTimeWorkRequestBuilder<SosRetryWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                    UNIQUE,
                    ExistingWorkPolicy.REPLACE,
                    req
                )
            }
        }
    }
}
