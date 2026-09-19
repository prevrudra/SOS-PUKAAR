package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

object AlertFireHelper {
    private const val TAG = "HighAlertFire"
    const val CHANNEL_ALERT = "highalert_sos_v4"
    const val ALERT_NOTIF_ID = 9002
    const val ACTION_ALERT_DATA_UPDATED = "com.pukaar.highalert.ALERT_DATA_UPDATED"
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastRingEventId: String? = null
    private var lastRingAtMs = 0L
    private var dataListener: ((PendingAlertResponse) -> Unit)? = null

    fun setDataListener(listener: ((PendingAlertResponse) -> Unit)?) {
        dataListener = listener
    }

    fun ensureAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { nm.deleteNotificationChannel("highalert_sos") }
        runCatching { nm.deleteNotificationChannel("highalert_sos_v2") }
        runCatching { nm.deleteNotificationChannel("highalert_sos_v3") }
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.sos_alert}")
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Full-screen SOS alerts — rings like an alarm clock"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 250, 700, 250, 1100)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, alarmAttrs)
            }
        )
    }

    /** Always on main thread — required for Activity + notification on Android 12+. */
    fun fire(context: Context, alert: PendingAlertResponse) {
        val appCtx = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            fireInternal(appCtx, alert)
        } else {
            mainHandler.post { fireInternal(appCtx, alert) }
        }
    }

    /** Updates stored alert + open UI without re-ringing (used after snapshot enrich). */
    fun updateAlertData(context: Context, alert: PendingAlertResponse) {
        val appCtx = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateAlertDataInternal(appCtx, alert)
        } else {
            mainHandler.post { updateAlertDataInternal(appCtx, alert) }
        }
    }

    fun reRing(context: Context, alert: PendingAlertResponse) {
        val appCtx = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            reRingInternal(appCtx, alert)
        } else {
            mainHandler.post { reRingInternal(appCtx, alert) }
        }
    }

    fun dismissRinging(context: Context) {
        val appCtx = context.applicationContext
        val work = Runnable {
            AlertRingState.clear(appCtx)
            AlarmClockRinger.cancel(appCtx)
            AlertSoundService.stop(appCtx)
            appCtx.getSystemService(NotificationManager::class.java)?.cancel(ALERT_NOTIF_ID)
        }
        if (Looper.myLooper() == Looper.getMainLooper()) work.run() else mainHandler.post(work)
    }

    private fun fireInternal(context: Context, alert: PendingAlertResponse) {
        val eventId = alert.eventId
        if (AlertSilence.isSilenced(context, eventId)) {
            Log.i(TAG, "Skip fire — silenced by user event=$eventId")
            return
        }
        val stored = AlertRingState.getActive(context)
        if (!eventId.isNullOrBlank() && stored?.eventId == eventId) {
            val merged = AlertMerge.merge(stored, alert)
            Log.i(TAG, "Already alerting event=$eventId — refresh only (no re-ring)")
            AlertRingState.setActive(context, merged)
            notifyDataUpdated(context, merged)
            return
        }
        val merged = alert
        val mergedEventId = merged.eventId
        val now = SystemClock.elapsedRealtime()
        val duplicateRing = !mergedEventId.isNullOrBlank() &&
            mergedEventId == lastRingEventId &&
            now - lastRingAtMs < 2_500L

        Log.i(TAG, "FIRE SOS event=$mergedEventId victim=${merged.victimName} ring=${!duplicateRing}")
        AlertRingState.setActive(context, merged)
        notifyDataUpdated(context, merged)

        if (duplicateRing) {
            AlarmClockRinger.arm(context, merged)
            return
        }

        lastRingEventId = mergedEventId
        lastRingAtMs = now
        AlarmClockRinger.fireNow(context, merged)
        ringNow(context, merged)
    }

    private fun updateAlertDataInternal(context: Context, alert: PendingAlertResponse) {
        if (AlertSilence.isSilenced(context, alert.eventId)) return
        val stored = AlertRingState.getActive(context)
        val merged = when {
            stored == null -> alert
            stored.eventId == alert.eventId -> AlertMerge.merge(stored, alert)
            else -> alert
        }
        Log.i(TAG, "UPDATE alert data event=${merged.eventId}")
        AlertRingState.setActive(context, merged)
        // Do not re-arm alarm clock here — that re-opens the screen in a loop.
        notifyDataUpdated(context, merged)
    }

    private fun notifyDataUpdated(context: Context, alert: PendingAlertResponse) {
        dataListener?.invoke(alert)
        context.sendBroadcast(
            Intent(ACTION_ALERT_DATA_UPDATED).apply {
                setPackage(context.packageName)
                putExtra("event_id", alert.eventId)
            }
        )
    }

    private fun reRingInternal(context: Context, alert: PendingAlertResponse) {
        if (AlertSilence.isSilenced(context, alert.eventId)) {
            AlarmClockRinger.cancel(context)
            return
        }
        AlarmClockRinger.arm(context, alert)
        ringNow(context, alert)
    }

    private fun ringNow(context: Context, alert: PendingAlertResponse) {
        ensureAlertChannel(context)
        wakeDevice(context)

        val who = alert.victimName?.takeIf { it.isNotBlank() }
            ?: alert.victimPhone?.takeIf { it.isNotBlank() }
            ?: "PUKAAR user"
        val isHelp = alert.triggerType.equals("HELP", ignoreCase = true)
            || alert.triggerType.equals("HELP_MODE", ignoreCase = true)
            || alert.triggerType.equals("INACTIVE_CHECK", ignoreCase = true)
        val mock = alert.mockDrill == true

        runCatching { AlertSoundService.start(context, who, isHelp, mock, alert) }

        val fullScreen = PendingIntent.getActivity(
            context, alert.eventId.hashCode(),
            AlertActivity.intent(context, alert),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.sos_alert}")
        val title = when {
            mock -> "PUKAAR TEST ALERT"
            isHelp -> "PUKAAR HELP — EMERGENCY"
            else -> "PUKAAR SOS — EMERGENCY"
        }
        val body = when {
            mock -> "$who has activated a practice alert. Tap to open."
            isHelp -> "$who has activated HELP and may need assistance. Tap to open."
            else -> "$who has activated SOS and may need immediate help."
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setSound(soundUri)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setVibrate(longArrayOf(0, 700, 250, 700, 250, 1100))
            .setTimeoutAfter(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        context.getSystemService(NotificationManager::class.java)?.notify(ALERT_NOTIF_ID, builder.build())

        runCatching {
            context.startActivity(
                AlertActivity.intent(context, alert).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            )
        }.onFailure { Log.w(TAG, "Could not start AlertActivity: ${it.message}") }
    }

    private fun wakeDevice(context: Context) {
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "pukaar:alarm_wake"
            ).acquire(10 * 60 * 1000L)
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pukaar:alarm_hold")
                .acquire(10 * 60 * 1000L)
        }
    }
}
