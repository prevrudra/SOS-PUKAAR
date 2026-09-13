package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

object AlertFireHelper {
    private const val TAG = "HighAlertFire"
    const val CHANNEL_ALERT = "highalert_sos_v3"
    const val ALERT_NOTIF_ID = 9002

    fun ensureAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { nm.deleteNotificationChannel("highalert_sos") }
        runCatching { nm.deleteNotificationChannel("highalert_sos_v2") }
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.sos_alert}")
        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERT, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Full-screen SOS alerts from trusted contacts"
                enableVibration(true)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(soundUri, alarmAttrs)
            }
        )
    }

    fun fire(context: Context, alert: PendingAlertResponse) {
        val appCtx = context.applicationContext
        ensureAlertChannel(appCtx)
        wakeScreen(appCtx)

        val fullScreen = PendingIntent.getActivity(
            appCtx, alert.eventId.hashCode(),
            AlertActivity.intent(appCtx, alert),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = Uri.parse("android.resource://${appCtx.packageName}/${R.raw.sos_alert}")
        val who = alert.victimName ?: "Someone"
        val notif = NotificationCompat.Builder(appCtx, CHANNEL_ALERT)
            .setContentTitle(
                if (alert.mockDrill == true) "PUKAAR TEST ALERT"
                else "PUKAAR SOS — EMERGENCY"
            )
            .setContentText(
                if (alert.mockDrill == true) "$who has activated a practice SOS. Tap to open."
                else "$who has activated SOS and may need immediate help."
            )
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
            .build()
        appCtx.getSystemService(NotificationManager::class.java)?.notify(ALERT_NOTIF_ID, notif)
        runCatching {
            appCtx.startActivity(
                AlertActivity.intent(appCtx, alert).addFlags(
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            )
        }.onFailure { Log.w(TAG, "Could not start AlertActivity: ${it.message}") }
    }

    private fun wakeScreen(context: Context) {
        runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "pukaar:grab"
            ).acquire(15_000L)
        }
    }
}
