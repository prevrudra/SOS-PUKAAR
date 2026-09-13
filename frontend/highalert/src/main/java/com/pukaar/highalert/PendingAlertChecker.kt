package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared pending-SOS check used by the foreground poller and OEM watchdog alarms.
 * Fires the alert UI even when [AlertMonitorService] was killed.
 */
object PendingAlertChecker {
    private const val TAG = "HighAlertPending"
    private val mutex = Mutex()

    suspend fun checkAndFire(context: Context): Boolean = mutex.withLock {
        val appCtx = context.applicationContext
        val session = AlertSession(appCtx)
        val token = session.token()
        if (token.isNullOrBlank()) return false

        return try {
            val api = AlertNetwork.api { token }
            val alert = api.pendingAlert()
            val eventId = alert.eventId
            if (alert.active != true || eventId.isNullOrBlank()) return false

            if (session.handledEventIds().contains(eventId)) {
                runCatching { api.acknowledge(AcknowledgeRequest(eventId, "DELIVERED")) }
                return false
            }

            Log.i(TAG, "Pending SOS $eventId — firing alert")
            AlertFireHelper.fire(appCtx, alert)
            session.markEventHandled(eventId)
            runCatching { api.acknowledge(AcknowledgeRequest(eventId, "DELIVERED")) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Pending check failed: ${e.message}")
            false
        }
    }
}
