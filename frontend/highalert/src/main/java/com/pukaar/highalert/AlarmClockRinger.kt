package com.pukaar.highalert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Uses [AlarmManager.setAlarmClock] — same privilege tier as the Clock app.
 * Shows over lock screen without requiring the user to unlock first.
 */
object AlarmClockRinger {
    private const val TAG = "HighAlertAlarmClock"
    private const val REQ = 9201
    private const val REQ_NOW = 9202
    private const val REPEAT_MS = 20_000L

    /** Fire immediately like an alarm clock (over lock screen). */
    fun fireNow(context: Context, alert: PendingAlertResponse) {
        scheduleAt(context, alert, System.currentTimeMillis() + 250L, REQ_NOW)
        arm(context, alert)
    }

    fun arm(context: Context, alert: PendingAlertResponse) {
        scheduleAt(context, alert, System.currentTimeMillis() + REPEAT_MS, REQ)
    }

    private fun scheduleAt(context: Context, alert: PendingAlertResponse, triggerAt: Long, reqCode: Int) {
        runCatching {
            val appCtx = context.applicationContext
            val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val showIntent = PendingIntent.getActivity(
                appCtx,
                alert.eventId.hashCode() + reqCode,
                AlertActivity.intent(appCtx, alert).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val ringIntent = PendingIntent.getBroadcast(
                appCtx,
                reqCode,
                Intent(appCtx, AlertRingReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val info = AlarmManager.AlarmClockInfo(triggerAt, showIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, ringIntent)
            } else {
                am.setAlarmClock(info, ringIntent)
            }
            Log.i(TAG, "Alarm-clock armed at delta=${triggerAt - System.currentTimeMillis()}ms")
        }.onFailure {
            Log.w(TAG, "arm failed: ${it.message}")
        }
    }

    fun cancel(context: Context) {
        runCatching {
            val appCtx = context.applicationContext
            val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (req in listOf(REQ, REQ_NOW)) {
                am.cancel(
                    PendingIntent.getBroadcast(
                        appCtx,
                        req,
                        Intent(appCtx, AlertRingReceiver::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }
}
