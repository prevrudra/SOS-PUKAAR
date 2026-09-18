package com.pukaar.app.emergency

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.pukaar.app.PukaarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Silent inactivity tracking — no notification to the elder.
 * Records unlock, screen-on, and any app usage; server escalates to contacts only.
 */
object PhoneUsageTracker {
    private const val TAG = "PhoneUsage"
    private const val PREFS = "phone_usage_tracker"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SOFT_HOURS = "soft_hours"
    private const val KEY_LAST_LOCAL_MS = "last_local_ms"
    private const val KEY_LAST_SERVER_SYNC_MS = "last_server_sync_ms"
    private const val KEY_PENDING_HEARTBEAT = "pending_heartbeat"
    private const val SERVER_SYNC_DEBOUNCE_MS = 2 * 60 * 1000L
    private const val SCREEN_ON_DEBOUNCE_MS = 90 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var lastScreenOnSyncMs = 0L

    fun configure(context: Context, softHours: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SOFT_HOURS, softHours)
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun arm(context: Context) {
        val appCtx = context.applicationContext
        if (!isEnabled(appCtx)) return
        PhoneUsagePollWorker.schedule(appCtx)
        val now = System.currentTimeMillis()
        touchLocal(appCtx, now)
        syncToServerIfNeeded(appCtx, now, force = true)
        Log.i(TAG, "Armed — silent phone-usage tracking")
    }

    fun disarm(context: Context) {
        PhoneUsagePollWorker.cancel(context.applicationContext)
    }

    /** Called on unlock, screen-on, app open, or usage-stats poll. */
    fun onPhoneUsed(context: Context, source: String) {
        if (!isEnabled(context)) return
        val now = System.currentTimeMillis()
        if (source == "screen_on" && now - lastScreenOnSyncMs < SCREEN_ON_DEBOUNCE_MS) return
        if (source == "screen_on") lastScreenOnSyncMs = now
        touchLocal(context, now)
        syncToServerIfNeeded(context, now, force = false)
        Log.d(TAG, "Activity ($source) at $now")
    }

    /** Periodic poll — detects any app opened without unlocking (e.g. incoming call UI). */
    fun pollUsageStats(context: Context): Boolean {
        if (!isEnabled(context)) return false
        val lastUsed = queryLastPhoneUsageMs(context) ?: return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastLocal = prefs.getLong(KEY_LAST_LOCAL_MS, 0L)
        if (lastUsed <= lastLocal) return false
        touchLocal(context, lastUsed)
        syncToServerIfNeeded(context, lastUsed, force = lastUsed > prefs.getLong(KEY_LAST_SERVER_SYNC_MS, 0L))
        Log.d(TAG, "Usage-stats activity at $lastUsed")
        return true
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun touchLocal(context: Context, atMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_LOCAL_MS, atMs)
            .apply()
    }

    private fun syncToServerIfNeeded(context: Context, atMs: Long, force: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_SERVER_SYNC_MS, 0L)
        if (!force && atMs - lastSync < SERVER_SYNC_DEBOUNCE_MS) return
        prefs.edit().putBoolean(KEY_PENDING_HEARTBEAT, true).apply()
        scope.launch {
            runCatching {
                if (PukaarApp.instance.sessionStore.token() == null) return@launch
                PukaarApp.instance.repository.heartbeat()
                prefs.edit()
                    .putLong(KEY_LAST_SERVER_SYNC_MS, System.currentTimeMillis())
                    .putBoolean(KEY_PENDING_HEARTBEAT, false)
                    .apply()
                Log.i(TAG, "Server lastActivityAt updated")
            }.onFailure {
                Log.w(TAG, "Heartbeat failed (will retry): ${it.message}")
            }
        }
    }

    fun flushPendingHeartbeat(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_PENDING_HEARTBEAT, false) && !isEnabled(context)) return
        syncToServerIfNeeded(context, System.currentTimeMillis(), force = true)
    }

    private fun queryLastPhoneUsageMs(context: Context): Long? {
        if (!hasUsageAccess(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.HOURS.toMillis(24)
        var latest = 0L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.USER_INTERACTION,
                    UsageEvents.Event.SCREEN_INTERACTIVE -> {
                        if (event.timeStamp > latest) latest = event.timeStamp
                    }
                }
            }
        }

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        stats?.forEach { s ->
            val t = maxOf(s.lastTimeUsed, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) s.lastTimeVisible else 0L)
            if (t > latest) latest = t
        }
        return latest.takeIf { it > 0 }
    }
}
