package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

/** Tracks alerts the contact dismissed — must not re-ring until a new SOS event. */
object AlertSilence {
    private const val TAG = "HighAlertSilence"

    fun isSilenced(context: Context, eventId: String?): Boolean {
        if (eventId.isNullOrBlank()) return false
        return runBlocking { AlertSession(context).handledEventIds().contains(eventId) }
    }

    fun silence(context: Context, eventId: String) {
        if (eventId.isBlank()) return
        Log.i(TAG, "Silencing event $eventId")
        runBlocking { AlertSession(context).markEventHandled(eventId) }
        AlertFireHelper.dismissRinging(context)
    }
}
