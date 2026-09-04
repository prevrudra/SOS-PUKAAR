package com.pukaar.app.emergency

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps PUKAAR alive after boot so hardware SOS intents and emergency workers survive OEM kills.
 */
class PukaarGuardService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
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
            // Boot / task-removed: check session off the main thread
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val token = com.pukaar.app.data.local.SessionStore(app).token()
                if (token != null) startForegroundSafe(app)
            }
        }

        private fun startForegroundSafe(app: Context) {
            runCatching {
                app.startForegroundService(Intent(app, PukaarGuardService::class.java))
            }.onFailure {
                runCatching { app.startService(Intent(app, PukaarGuardService::class.java)) }
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(Intent(context, PukaarGuardService::class.java))
        }
    }
}
