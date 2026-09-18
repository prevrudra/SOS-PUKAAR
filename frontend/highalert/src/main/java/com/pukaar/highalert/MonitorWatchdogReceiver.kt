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
                    Log.i(TAG, "Watchdog tick — check pending")
                    PendingAlertChecker.checkAndFire(appCtx)
                    // Do NOT start FGS from alarm receiver — Android 12+ crashes the app.
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
        private const val REQ_PRIMARY = 9101
        private const val REQ_BACKUP = 9102
        /** 5s primary + 15s backup — was 15/45; cut delay when FCM is late. */
        private const val INTERVAL_MS = 5_000L
        private const val BACKUP_INTERVAL_MS = 15_000L

        fun schedule(context: Context) {
            scheduleAlarm(context, REQ_PRIMARY, INTERVAL_MS)
            scheduleAlarm(context, REQ_BACKUP, BACKUP_INTERVAL_MS)
        }

        fun cancel(context: Context) {
            runCatching {
                val appCtx = context.applicationContext
                val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
                am.cancel(pending(appCtx, REQ_PRIMARY))
                am.cancel(pending(appCtx, REQ_BACKUP))
            }
        }

        private fun scheduleAlarm(context: Context, reqCode: Int, intervalMs: Long) {
            runCatching {
                val appCtx = context.applicationContext
                val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
                val pi = pending(appCtx, reqCode)
                val trigger = SystemClock.elapsedRealtime() + intervalMs
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
                } else {
                    am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
                }
            }.onFailure {
                Log.w(TAG, "Schedule failed (req=$reqCode): ${it.message}")
            }
        }

        private fun pending(context: Context, reqCode: Int): PendingIntent {
            val i = Intent(context, MonitorWatchdogReceiver::class.java)
            return PendingIntent.getBroadcast(
                context, reqCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
