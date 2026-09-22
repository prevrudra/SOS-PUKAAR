package com.pukaar.highalert

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Persists the active SOS so alarm-clock repeats can re-ring until dismissed.
 * Uses SharedPreferences for fast synchronous access (no ANR risk).
 */
object AlertRingState {
    private const val PREFS_NAME = "alert_ring_prefs"
    private const val KEY_JSON = "active_alert_json"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(PendingAlertResponse::class.java)

    @Volatile
    private var cachedAlert: PendingAlertResponse? = null

    fun setActive(context: Context, alert: PendingAlertResponse) {
        cachedAlert = alert
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, adapter.toJson(alert))
            .apply()
    }

    fun getActive(context: Context): PendingAlertResponse? {
        cachedAlert?.let { return it }
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
        return json?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
            .also { cachedAlert = it }
    }

    fun clear(context: Context) {
        cachedAlert = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_JSON)
            .apply()
    }

    fun isRinging(context: Context): Boolean = getActive(context)?.eventId != null
}
