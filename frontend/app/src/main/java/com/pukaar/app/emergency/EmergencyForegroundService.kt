package com.pukaar.app.emergency

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import kotlinx.coroutines.*

class EmergencyForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var eventId: String? = null
    private var fused: FusedLocationProviderClient? = null
    private var audio: EmergencyAudioRecorder? = null
    private var segmentIndex = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafe()
            return START_NOT_STICKY
        }
        eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        val isSos = intent?.getBooleanExtra(EXTRA_IS_SOS, true) != false
        startAsForeground(isSos)
        acquireWakeLock()
        startLocationUpdates()
        if (isSos) startAudioLoop()
        return START_STICKY
    }

    private fun startAsForeground(isSos: Boolean) {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, PukaarApp.CHANNEL_EMERGENCY)
            .setContentTitle(if (isSos) "SOS ACTIVE" else "HELP ACTIVE")
            .setContentText("PUKAAR is running emergency automation")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        val types = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else if (Build.VERSION.SDK_INT >= 29) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else 0

        ServiceCompat.startForeground(this, NOTIF_ID, notification, types)
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pukaar:emergency").apply {
            setReferenceCounted(false)
            acquire(4 * 60 * 60 * 1000L)
        }
    }

    private fun startLocationUpdates() {
        fused = LocationServices.getFusedLocationProviderClient(this)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .build()
        try {
            fused?.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    val id = eventId ?: return
                    scope.launch {
                        runCatching {
                            PukaarApp.instance.repository.updateLocation(id, loc.latitude, loc.longitude, loc.accuracy.toDouble())
                        }
                    }
                }
            }, mainLooper)
        } catch (_: SecurityException) {
            // Permission missing — engine continues with last known path
        }
    }

    private fun startAudioLoop() {
        audio = EmergencyAudioRecorder(this)
        scope.launch {
            while (isActive) {
                val id = eventId ?: break
                val file = audio?.recordSegment(60_000L) ?: break
                val created = runCatching {
                    PukaarApp.instance.repository.createSegment(id, segmentIndex++)
                }.getOrNull()
                val segmentId = created?.segmentId
                if (segmentId != null) {
                    val key = "local/${id}/${file.name}"
                    // Local persistence first; mark uploaded only when server ack succeeds.
                    runCatching {
                        PukaarApp.instance.repository.markUploaded(id, segmentId, key)
                    }
                }
            }
        }
    }

    private fun stopSelfSafe() {
        scope.cancel()
        audio?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSelfSafe()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.pukaar.app.STOP_EMERGENCY"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_IS_SOS = "is_sos"

        fun start(context: Context, eventId: String, isSos: Boolean) {
            val intent = Intent(context, EmergencyForegroundService::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_IS_SOS, isSos)
            }
            ContextCompatStart(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, EmergencyForegroundService::class.java).apply { action = ACTION_STOP }
            ContextCompatStart(context, intent)
        }

        private fun ContextCompatStart(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
