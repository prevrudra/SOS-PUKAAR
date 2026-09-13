package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AlertMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
            return START_NOT_STICKY
        }
        return try {
            createChannels()
            acquireWakeLock()
            startForeground(NOTIF_ID, buildWatchingNotification())
            MonitorWatchdogReceiver.schedule(this)
            MonitorKeepAliveWorker.enqueue(this)
            if (pollingJob?.isActive != true) {
                pollingJob = scope.launch { pollLoop() }
            }
            START_STICKY
        } catch (t: Throwable) {
            Log.e("HighAlert", "Monitor start failed: ${t.message}", t)
            runCatching { stopSelf() }
            START_NOT_STICKY
        }
    }

    private suspend fun pollLoop() {
        while (scope.isActive) {
            try {
                renewWakeLockIfNeeded()
                PendingAlertChecker.checkAndFire(this)
            } catch (e: Exception) {
                Log.w("HighAlert", "Poll failed: ${e.message}")
            }
            delay(3_000L)
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
            AlertFireHelper.ensureAlertChannel(this)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pukaar:highalert").apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L)
        }
    }

    private fun renewWakeLockIfNeeded() {
        if (wakeLock?.isHeld == true) return
        acquireWakeLock()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        scope.cancel()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        MonitorWatchdogReceiver.schedule(this)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        MonitorWatchdogReceiver.schedule(this)
        start(this)
    }

    companion object {
        private const val CHANNEL_MONITOR = "highalert_monitor"
        private const val NOTIF_ID = 9001
        const val ALERT_NOTIF_ID = AlertFireHelper.ALERT_NOTIF_ID
        const val ACTION_STOP = "com.pukaar.highalert.STOP"

        fun start(ctx: Context) {
            runCatching {
                val appCtx = ctx.applicationContext
                val i = Intent(appCtx, AlertMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(i)
                } else {
                    appCtx.startService(i)
                }
            }.onFailure {
                Log.e("HighAlert", "Could not start monitor: ${it.message}", it)
            }
        }

        fun stop(ctx: Context) {
            runCatching {
                MonitorWatchdogReceiver.cancel(ctx.applicationContext)
                MonitorKeepAliveWorker.cancel(ctx.applicationContext)
                val i = Intent(ctx.applicationContext, AlertMonitorService::class.java).apply {
                    action = ACTION_STOP
                }
                ctx.applicationContext.startService(i)
            }
        }
    }
}
