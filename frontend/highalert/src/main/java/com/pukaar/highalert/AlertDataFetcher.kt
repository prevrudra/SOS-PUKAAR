package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay

/** Fetches full SOS payload from the server with retries — FCM push is intentionally thin. */
object AlertDataFetcher {
    private const val TAG = "HighAlertFetch"

    suspend fun fetchEventSnapshot(context: Context, eventId: String): PendingAlertResponse? {
        val token = AlertSession(context).token()
        if (token.isNullOrBlank()) return null
        val api = AlertNetwork.api { token }
        var last: PendingAlertResponse? = null
        repeat(5) { attempt ->
            try {
                val snap = api.eventSnapshot(eventId)
                last = snap
                if (snap.active == true) {
                    if (!snap.trustedContacts.isNullOrEmpty() || !snap.helpNumbers.isNullOrEmpty()) {
                        return snap
                    }
                    if (attempt >= 4) return snap
                } else if (snap.active == false) {
                    return snap
                }
            } catch (e: Exception) {
                Log.w(TAG, "eventSnapshot $eventId attempt ${attempt + 1}: ${e.message}")
            }
            if (attempt < 4) delay(600L * (attempt + 1))
        }
        return last
    }

    suspend fun fetchPending(context: Context): PendingAlertResponse? {
        val token = AlertSession(context).token()
        if (token.isNullOrBlank()) return null
        val api = AlertNetwork.api { token }
        repeat(3) { attempt ->
            try {
                val pending = api.pendingAlert()
                if (pending.active == true) return pending
                return pending
            } catch (e: Exception) {
                Log.w(TAG, "pending attempt ${attempt + 1}: ${e.message}")
            }
            if (attempt < 2) delay(800L)
        }
        return null
    }
}
