package com.pukaar.app.emergency

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps PUKAAR alive after boot so hardware SOS + phone-usage tracking survive OEM kills.
 * Dynamically registers SCREEN_ON (manifest registration is ignored on Android 8+).
 */
class PukaarGuardService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            startForeground(NOTIFICATION_ID, buildNotification())
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
        PukaarGuardService.start(this)
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
            if (hasSession == true) {
                startForegroundSafe(app)
                return
            }
            if (hasSession == false) return
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val token = com.pukaar.app.data.local.SessionStore(app).token()
                if (token != null) startForegroundSafe(app)
            }
        }

        /** Only call while app is in foreground (e.g. MainActivity.onResume). */
        private fun startForegroundSafe(app: Context) {
            runCatching {
                app.startForegroundService(Intent(app, PukaarGuardService::class.java))
            }.onFailure { e ->
                Log.w("PUKAAR", "Guard FGS blocked (background): ${e.message}")
                HardwareReceiverRegistry.register(app)
                GuardBoostWorker.kick(app)
            }
        }
    }
}
