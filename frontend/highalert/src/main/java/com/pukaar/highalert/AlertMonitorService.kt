package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlertMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastEventId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannels()
        acquireWakeLock()
        startForeground(NOTIF_ID, buildWatchingNotification())
        if (pollingJob?.isActive != true) {
            pollingJob = scope.launch { pollLoop() }
        }
        return START_STICKY
    }

    private suspend fun pollLoop() {
        while (scope.isActive) {
            try {
                val token = HighAlertApp.instance.session.token()
                if (!token.isNullOrBlank()) {
                    val api = AlertNetwork.api { token }
                    val alert = api.pendingAlert()
                    if (alert.active == true && alert.eventId != null && alert.eventId != lastEventId) {
                        lastEventId = alert.eventId
                        fireAlert(alert)
                    }
                }
            } catch (e: Exception) {
                Log.w("HighAlert", "Poll failed: ${e.message}")
            }
            delay(3_000L)
        }
    }

    private fun fireAlert(alert: PendingAlertResponse) {
        wakeScreen()
        val fullScreen = PendingIntent.getActivity(
            this, alert.eventId.hashCode(),
            AlertActivity.intent(this, alert),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val notif = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setContentTitle(if (alert.mockDrill == true) "TEST ALERT" else "EMERGENCY SOS")
            .setContentText("${alert.victimName ?: "Someone"} needs help — tap to open")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setSound(alarmUri)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setVibrate(longArrayOf(0, 800, 400, 800, 400, 1200))
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIF_ID, notif)
        runCatching {
            startActivity(AlertActivity.intent(this, alert))
        }.onFailure { Log.w("HighAlert", "Could not start AlertActivity: ${it.message}") }
    }

    private fun wakeScreen() {
        runCatching {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "pukaar:grab"
            ).acquire(15_000L)
        }
    }

    private fun buildWatchingNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("PUKAAR High Alert")
            .setContentText("Watching for PUKAAR alerts…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_MONITOR, "Alert Monitor", NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERT, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Full-screen SOS alerts from trusted contacts"
                    enableVibration(true)
                    enableLights(true)
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val alarmAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    if (alarmUri != null) setSound(alarmUri, alarmAttrs)
                }
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pukaar:highalert").apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L) // 1 hour; renewed each start
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        start(this)
    }

    companion object {
        private const val CHANNEL_MONITOR = "highalert_monitor"
        private const val CHANNEL_ALERT = "highalert_sos"
        private const val NOTIF_ID = 9001
        const val ALERT_NOTIF_ID = 9002
        const val ACTION_STOP = "com.pukaar.highalert.STOP"

        fun start(ctx: Context) {
            val i = Intent(ctx, AlertMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            val i = Intent(ctx, AlertMonitorService::class.java).apply { action = ACTION_STOP }
            ctx.startService(i)
        }
    }
}
