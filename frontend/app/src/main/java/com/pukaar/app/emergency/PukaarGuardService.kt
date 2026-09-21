package com.pukaar.app.emergency

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.pukaar.app.MainActivity
import com.pukaar.app.R
import com.pukaar.app.data.local.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps PUKAAR alive after boot so hardware SOS + phone-usage tracking survive OEM kills.
 * Must only enter foreground while the app is visible — Android 12+ kills background FGS starts.
 */
class PukaarGuardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("PUKAAR", "Guard FGS skipped — POST_NOTIFICATIONS not granted")
                HardwareReceiverRegistry.register(this)
                stopSelf()
                return START_NOT_STICKY
            }
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            HardwareReceiverRegistry.register(this)
            if (PhoneUsageTracker.isEnabled(this)) {
                PhoneUsageTracker.arm(this)
            }
            START_STICKY
        } catch (e: Exception) {
            Log.e("PUKAAR", "Guard service failed — stopping to avoid crash loop", e)
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Never restart FGS from background (causes "keeps stopping" on Motorola/Android 12+).
        HardwareReceiverRegistry.register(this)
        GuardBoostWorker.kick(this)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_GUARD)
            .setSmallIcon(R.drawable.ic_stat_pukaar)
            .setContentTitle(getString(R.string.guard_notification_title))
            .setContentText(getString(R.string.guard_notification_body))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 7001
        const val CHANNEL_GUARD = "pukaar_guard"

        fun start(context: Context, hasSession: Boolean? = null) {
            val app = context.applicationContext
            if (hasSession == false) return

            if (!AppForegroundTracker.isInForeground) {
                HardwareReceiverRegistry.register(app)
                GuardBoostWorker.kick(app)
                return
            }

            if (hasSession == true) {
                startForegroundSafe(app)
                return
            }

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                if (SessionStore(app).token() != null && AppForegroundTracker.isInForeground) {
                    startForegroundSafe(app)
                } else {
                    HardwareReceiverRegistry.register(app)
                }
            }
        }

        private fun startForegroundSafe(app: Context) {
            if (!AppForegroundTracker.isInForeground) {
                HardwareReceiverRegistry.register(app)
                GuardBoostWorker.kick(app)
                return
            }
            runCatching {
                app.startForegroundService(Intent(app, PukaarGuardService::class.java))
            }.onFailure { e ->
                Log.w("PUKAAR", "Guard FGS blocked: ${e.message}")
                HardwareReceiverRegistry.register(app)
                GuardBoostWorker.kick(app)
            }
        }
    }
}
