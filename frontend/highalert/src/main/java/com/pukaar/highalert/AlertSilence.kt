package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tracks alerts the contact dismissed — must not re-ring until a new SOS event.
 * Uses SharedPreferences + in-memory cache for fast synchronous lookups (no ANR).
 */
object AlertSilence {
    private const val TAG = "HighAlertSilence"
    private const val PREFS_NAME = "alert_silence_prefs"
    private const val KEY_HANDLED = "handled_event_ids"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedIds: Set<String>? = null

    fun isSilenced(context: Context, eventId: String?): Boolean {
        if (eventId.isNullOrBlank()) return false
        val ids = cachedIds ?: loadIds(context).also { cachedIds = it }
        return ids.contains(eventId)
    }

    fun silence(context: Context, eventId: String) {
        if (eventId.isBlank()) return
        Log.i(TAG, "Silencing event $eventId")
        val current = cachedIds ?: loadIds(context)
        val updated = current + eventId
        cachedIds = updated
        // Persist async — silencing is already applied in memory
        scope.launch {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_HANDLED, updated)
                .apply()
        }
        // Also persist to session for cross-check
        scope.launch {
            runCatching { AlertSession(context).markEventHandled(eventId) }
        }
        AlertFireHelper.dismissRinging(context)
    }

    private fun loadIds(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_HANDLED, emptySet()) ?: emptySet()
    }

    /** Clear old silenced events (call on logout or periodically). */
    fun clearAll(context: Context) {
        cachedIds = emptySet()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HANDLED)
            .apply()
    }
}
