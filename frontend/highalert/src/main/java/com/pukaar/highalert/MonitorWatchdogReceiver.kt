package com.pukaar.highalert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * OEM survival: Motorola/Xiaomi often kill the monitor FGS.
 * Periodic exact alarms poll /pending and fire the SOS even if the service is dead.
 */
class MonitorWatchdogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val appCtx = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val token = AlertSession(appCtx).token()
                if (!token.isNullOrBlank()) {
                    Log.i(TAG, "Watchdog tick — check pending + restart monitor")
                    PendingAlertChecker.checkAndFire(appCtx)
                    AlertMonitorService.start(appCtx)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Watchdog failed: ${t.message}")
            } finally {
                schedule(appCtx)
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "HighAlertWatchdog"
        private const val REQ = 9101
        /** Short chain of exact alarms — critical for phones that kill the FGS. */
        private const val INTERVAL_MS = 60_000L

        fun schedule(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
                val pi = pending(appCtx)
                val trigger = SystemClock.elapsedRealtime() + INTERVAL_MS
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
                } else {
                    am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
                }
            }.onFailure {
                Log.w(TAG, "Schedule failed: ${it.message}")
            }
        }

        fun cancel(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
                am.cancel(pending(appCtx))
            }
        }

        private fun pending(context: Context): PendingIntent {
            val i = Intent(context, MonitorWatchdogReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, REQ, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
