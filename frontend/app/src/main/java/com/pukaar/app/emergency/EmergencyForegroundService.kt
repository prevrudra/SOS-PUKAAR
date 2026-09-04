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
import com.pukaar.app.util.DeviceTelemetry
import com.pukaar.app.util.FileHash
import com.pukaar.app.util.NetworkUtils
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

class EmergencyForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var eventId: String? = null
    private var fused: FusedLocationProviderClient? = null
    private var audio: EmergencyAudioRecorder? = null
    private var segmentIndex = 0
    private val pendingUploads = ConcurrentLinkedQueue<PendingUpload>()

    private data class PendingUpload(
        val eventId: String,
        val file: File,
        val index: Int,
        var segmentId: String? = null
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafe()
            return START_NOT_STICKY
        }
        eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
        val isSos = intent?.getBooleanExtra(EXTRA_IS_SOS, true) != false
        val recordAudio = intent?.getBooleanExtra(EXTRA_RECORD_AUDIO, isSos) == true
        return try {
            startAsForeground(isSos, recordAudio)
            acquireWakeLock()
            startLocationUpdates()
            startTelemetryLoop()
            if (recordAudio && hasRecordAudioPermission()) startAudioLoop()
            START_STICKY
        } catch (e: Exception) {
            android.util.Log.e("PUKAAR", "EmergencyForegroundService failed to start", e)
            stopSelfSafe()
            START_NOT_STICKY
        }
    }

    private fun hasRecordAudioPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun startAsForeground(isSos: Boolean, recordAudio: Boolean) {
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

        var types = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0
        if (hasLocationPermission()) {
            types = types or if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else 0
        }
        if (recordAudio && hasRecordAudioPermission()) {
            types = types or if (Build.VERSION.SDK_INT >= 29) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else 0
        }

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

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                val id = eventId ?: break
                runCatching {
                    PukaarApp.instance.repository.updateTelemetry(
                        id,
                        DeviceTelemetry.batteryPercent(this@EmergencyForegroundService),
                        DeviceTelemetry.networkType(this@EmergencyForegroundService)
                    )
                }
                delay(30_000L)
            }
        }
    }

    private fun startAudioLoop() {
        audio = EmergencyAudioRecorder(this)
        scope.launch {
            while (isActive) {
                val id = eventId ?: break
                flushPendingUploads()
                val file = audio?.recordSegment(60_000L) ?: break
                val index = segmentIndex++
                val pending = PendingUpload(id, file, index)
                if (!tryUploadPending(pending)) {
                    pendingUploads.add(pending)
                }
            }
            flushPendingUploads()
        }
    }

    private suspend fun flushPendingUploads() {
        if (!NetworkUtils.isOnline(this)) return
        for (pending in pendingUploads.toList()) {
            if (tryUploadPending(pending)) {
                pendingUploads.remove(pending)
            }
        }
    }

    private suspend fun tryUploadPending(pending: PendingUpload): Boolean {
        if (!NetworkUtils.isOnline(this)) return false
        if (!pending.file.exists()) return true

        val segmentId = pending.segmentId ?: run {
            val checksum = FileHash.sha256(pending.file)
            val created = runCatching {
                PukaarApp.instance.repository.createSegment(
                    pending.eventId,
                    pending.index,
                    checksum,
                    pending.file.length()
                )
            }.getOrNull()
            val id = created?.segmentId ?: return false
            pending.segmentId = id
            id
        }

        val uploaded = runCatching {
            PukaarApp.instance.repository.uploadSegment(pending.eventId, segmentId, pending.file)
        }.getOrNull()

        return uploaded?.cloudSafe == true
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
        const val EXTRA_RECORD_AUDIO = "record_audio"

        fun start(context: Context, eventId: String, isSos: Boolean, recordAudio: Boolean = isSos) {
            val intent = Intent(context, EmergencyForegroundService::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_IS_SOS, isSos)
                putExtra(EXTRA_RECORD_AUDIO, recordAudio)
            }
            runCatching { ContextCompatStart(context, intent) }
                .onFailure { android.util.Log.e("PUKAAR", "Could not start emergency service", it) }
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
