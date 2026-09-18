package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Polls /pending and fires SOS until the contact taps Stop or the victim marks safe. */
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
            if (alert.active != true || eventId.isNullOrBlank()) {
                // Victim marked safe — stop any leftover ringing.
                if (AlertRingState.isRinging(appCtx)) {
                    AlertFireHelper.dismissRinging(appCtx)
                }
                return false
            }

            if (AlertSilence.isSilenced(appCtx, eventId)) {
                Log.i(TAG, "Pending SOS $eventId — skipped (user stopped alert)")
                return false
            }

            Log.i(TAG, "Pending SOS $eventId — firing alert")
            AlertFireHelper.fire(appCtx, alert)
            if (alert.trustedContacts.isNullOrEmpty() || alert.helpNumbers.isNullOrEmpty()) {
                AlertDataFetcher.fetchEventSnapshot(appCtx, eventId)?.let { enriched ->
                    if (enriched.active == true) {
                        AlertFireHelper.updateAlertData(appCtx, enriched)
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Pending check failed: ${e.message}")
            false
        }
    }
}
