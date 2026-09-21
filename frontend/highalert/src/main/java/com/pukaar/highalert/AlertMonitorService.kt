package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Hard-coded SOS receiver.
 *
 * CRITICAL: After [Context.startForegroundService], [startForeground] MUST be called
 * within ~5s or Android kills the process ("keeps stopping"). Never skip it.
 * FGS notifications are exempt from POST_NOTIFICATIONS on Android 13+.
 */
class AlertMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var checkJob: Job? = null
    private var guardJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            when (intent?.action) {
                ACTION_STOP -> {
                    stopGuardInternal()
                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                    START_NOT_STICKY
                }
                ACTION_GUARD -> {
                    createChannels()
                    if (!enterForeground(GUARD_NOTIF_ID, buildGuardNotification())) {
                        abortFgsStart()
                        return START_NOT_STICKY
                    }
                    startGuardInternal(alreadyForeground = true)
                    START_STICKY
                }
                ACTION_CHECK_ONCE, null -> {
                    createChannels()
                    if (!enterForeground(NOTIF_ID, buildQuietNotification())) {
                        abortFgsStart()
                        return START_NOT_STICKY
                    }
                    MonitorWatchdogReceiver.schedule(this)
                    MonitorKeepAliveWorker.enqueue(this)
                    if (checkJob?.isActive != true) {
                        checkJob = scope.launch {
                            try {
                                PendingAlertChecker.checkAndFire(this@AlertMonitorService)
                            } catch (e: Exception) {
                                Log.w(TAG, "Check failed: ${e.message}")
                            } finally {
                                delay(1_500L)
                                if (guardJob?.isActive != true) {
                                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                                    stopSelf()
                                }
                            }
                        }
                    }
                    START_NOT_STICKY
                }
                else -> START_NOT_STICKY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Monitor service failed — stopping to avoid crash loop", e)
            abortFgsStart()
            START_NOT_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatching {
            val loggedIn = runBlocking { !AlertSession(this@AlertMonitorService).token().isNullOrBlank() }
            if (loggedIn) {
                Log.i(TAG, "Task removed — re-arming watchdog (no background FGS)")
                MonitorWatchdogReceiver.schedule(applicationContext)
                MonitorKeepAliveWorker.enqueue(applicationContext)
            }
        }
    }

    /**
     * Always call startForeground after startForegroundService.
     * Returns false only if the platform rejected it — caller must stopSelf immediately.
     */
    private fun enterForeground(notifId: Int, notification: Notification): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                ServiceCompat.startForeground(this, notifId, notification, type)
            } else {
                @Suppress("DEPRECATION")
                startForeground(notifId, notification)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed: ${e.message}", e)
            false
        }
    }

    private fun abortFgsStart() {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        MonitorWatchdogReceiver.schedule(applicationContext)
        MonitorKeepAliveWorker.enqueue(applicationContext)
        stopSelf()
    }

    private fun startGuardInternal(alreadyForeground: Boolean = false) {
        MonitorWatchdogReceiver.schedule(this)
        MonitorKeepAliveWorker.enqueue(this)
        registerNetworkCallback()
        if (guardJob?.isActive == true) return
        if (!alreadyForeground) {
            createChannels()
            if (!enterForeground(GUARD_NOTIF_ID, buildGuardNotification())) {
                abortFgsStart()
                return
            }
        }
        guardJob = scope.launch {
            while (isActive) {
                try {
                    PendingAlertChecker.checkAndFire(this@AlertMonitorService)
                } catch (e: Exception) {
                    Log.w(TAG, "Guard poll failed: ${e.message}")
                }
                delay(GUARD_POLL_MS)
            }
        }
    }

    private fun stopGuardInternal() {
        guardJob?.cancel()
        guardJob = null
        unregisterNetworkCallback()
        MonitorWatchdogReceiver.cancel(this)
        MonitorKeepAliveWorker.cancel(this)
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch {
                    if (!AlertSession(this@AlertMonitorService).token().isNullOrBlank()) {
                        Log.i(TAG, "Network available — guard poll")
                        PendingAlertChecker.checkAndFire(this@AlertMonitorService)
                        FcmRegistrar.refreshAndRegister(this@AlertMonitorService)
                    }
                }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    onAvailable(network)
                }
            }
        }
        networkCallback = callback
        runCatching {
            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                callback
            )
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback?.let { runCatching { cm?.unregisterNetworkCallback(it) } }
        networkCallback = null
    }

    private fun buildQuietNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_MONITOR)
            .setContentTitle("PUKAAR High Alert")
            .setContentText("Checking for emergency alerts…")
            .setSmallIcon(R.drawable.ic_stat_highalert)
            .setOngoing(false)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    private fun buildGuardNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_GUARD)
            .setContentTitle("PUKAAR Alert Guard")
            .setContentText("Ready to receive SOS — do not force stop this app")
            .setSmallIcon(R.drawable.ic_stat_highalert)
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR,
                    "Alert check",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Brief silent check for emergency alerts"
                    setShowBadge(false)
                }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GUARD,
                    "Alert guard",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps High Alert ready to receive SOS even when closed"
                    setShowBadge(false)
                }
            )
            runCatching { AlertFireHelper.ensureAlertChannel(this) }
        }
    }

    override fun onDestroy() {
        checkJob?.cancel()
        guardJob?.cancel()
        unregisterNetworkCallback()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "HighAlert"
        private const val CHANNEL_MONITOR = "highalert_monitor_light"
        private const val CHANNEL_GUARD = "highalert_guard"
        private const val NOTIF_ID = 9001
        private const val GUARD_NOTIF_ID = 9003
        private const val GUARD_POLL_MS = 5_000L
        const val ALERT_NOTIF_ID = AlertFireHelper.ALERT_NOTIF_ID
        const val ACTION_STOP = "com.pukaar.highalert.STOP"
        const val ACTION_CHECK_ONCE = "com.pukaar.highalert.CHECK_ONCE"
        const val ACTION_GUARD = "com.pukaar.highalert.GUARD"

        fun start(ctx: Context) {
            // One-shot check must not use FGS from background — alarms handle that.
            MonitorWatchdogReceiver.schedule(ctx.applicationContext)
            MonitorKeepAliveWorker.enqueue(ctx.applicationContext)
        }

        /** Persistent guard — only from a visible Activity (foreground). */
        fun startGuard(ctx: Context) {
            if (!AppForegroundTracker.isInForeground) {
                Log.w(TAG, "Skip guard FGS — app not in foreground")
                MonitorWatchdogReceiver.schedule(ctx.applicationContext)
                MonitorKeepAliveWorker.enqueue(ctx.applicationContext)
                return
            }
            safeStart(ctx, ACTION_GUARD)
        }

        private fun safeStart(ctx: Context, action: String) {
            runCatching {
                val appCtx = ctx.applicationContext
                val i = Intent(appCtx, AlertMonitorService::class.java).apply { this.action = action }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appCtx.startForegroundService(i)
                } else {
                    appCtx.startService(i)
                }
            }.onFailure { e ->
                Log.w(TAG, "FGS start blocked ($action): ${e.message}")
                MonitorWatchdogReceiver.schedule(ctx.applicationContext)
                MonitorKeepAliveWorker.enqueue(ctx.applicationContext)
            }
        }

        fun armLightMonitoring(ctx: Context) {
            AlertReliabilityEngine.armAll(ctx.applicationContext)
        }

        fun stop(ctx: Context) {
            runCatching {
                val appCtx = ctx.applicationContext
                MonitorWatchdogReceiver.cancel(appCtx)
                MonitorKeepAliveWorker.cancel(appCtx)
                val i = Intent(appCtx, AlertMonitorService::class.java).apply {
                    action = ACTION_STOP
                }
                appCtx.startService(i)
            }
        }
    }
}
