package com.pukaar.app.emergency

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp
import com.pukaar.app.R

/**
 * Placeholder for optional emergency phrase listening — not implemented.
 * MUST call startForeground immediately if ever started as FGS, otherwise Android
 * kills the process with "app keeps stopping".
 */
class PukaarVoiceTriggerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Satisfy the 5s FGS contract even though this feature is not live.
        runCatching { promoteBare() }
            .onFailure { Log.e("PUKAAR", "VoiceTrigger promote failed", it) }
        Log.i("PUKAAR", "Voice trigger placeholder — stopping (not implemented)")
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun promoteBare() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, PukaarApp.CHANNEL_EMERGENCY)
            .setSmallIcon(R.drawable.ic_stat_pukaar)
            .setContentTitle("PUKAAR")
            .setContentText("Voice trigger is not active")
            .setContentIntent(open)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(this, NOTIF_ID, notification, 0)
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val NOTIF_ID = 7002
    }
}
