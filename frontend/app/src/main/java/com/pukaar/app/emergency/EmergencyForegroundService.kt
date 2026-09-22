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
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
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
        // Promote FIRST — Android kills the process if startForeground is skipped (~5s).
        val isSosHint = intent?.getBooleanExtra(EXTRA_IS_SOS, true) ?: true
        val recordHint = intent?.getBooleanExtra(EXTRA_RECORD_AUDIO, isSosHint) ?: isSosHint
        val promoted = runCatching { startAsForeground(isSosHint, recordHint); true }
            .recoverCatching {
                android.util.Log.e("PUKAAR", "Emergency promote failed, bare fallback", it)
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, buildEmergencyNotification(isSosHint, recordHint))
                true
            }.getOrElse {
                android.util.Log.e("PUKAAR", "Emergency bare promote failed", it)
                false
            }
        if (!promoted) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            stopSelfSafe()
            return START_NOT_STICKY
        }
        eventId = intent?.getStringExtra(EXTRA_EVENT_ID)
            ?: EmergencySessionStore.activeEventId(this)
        val isSos = intent?.getBooleanExtra(EXTRA_IS_SOS, true)
            ?: EmergencySessionStore.isSos(this)
        val recordAudio = if (intent?.hasExtra(EXTRA_RECORD_AUDIO) == true) {
            intent.getBooleanExtra(EXTRA_RECORD_AUDIO, isSos)
        } else {
            EmergencySessionStore.recordAudio(this)
        }
        if (eventId.isNullOrBlank()) {
            android.util.Log.e("PUKAAR", "Emergency FGS started with no eventId — stopping")
            stopSelfSafe()
            return START_NOT_STICKY
        }
        EmergencySessionStore.save(
            this,
            eventId = eventId!!,
            isSos = isSos,
            mockDrill = EmergencySessionStore.isMock(this),
            serverSynced = !eventId!!.startsWith("local-"),
            latitude = EmergencySessionStore.latitude(this),
            longitude = EmergencySessionStore.longitude(this),
            recordAudio = recordAudio
        )
        return try {
            acquireWakeLock()
            startLocationUpdates()
            startPeriodicLocationSync()
            startTelemetryLoop()
            if (recordAudio && hasRecordAudioPermission()) startAudioLoop()
            START_NOT_STICKY
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

    private fun buildEmergencyNotification(isSos: Boolean, recordAudio: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PukaarApp.CHANNEL_EMERGENCY)
            .setContentTitle(if (isSos) "SOS ACTIVE" else "HELP ACTIVE")
            .setContentText(
                if (recordAudio) getString(R.string.emergency_recording_notification)
                else "PUKAAR is running emergency automation"
            )
            .setSmallIcon(R.drawable.ic_stat_pukaar)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun startAsForeground(isSos: Boolean, recordAudio: Boolean) {
        val notification = buildEmergencyNotification(isSos, recordAudio)
        var types = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0
        if (hasLocationPermission() && Build.VERSION.SDK_INT >= 29) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (recordAudio && hasRecordAudioPermission() && Build.VERSION.SDK_INT >= 29) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, notification, types)
        } catch (e: Exception) {
            android.util.Log.e("PUKAAR", "typed emergency startForeground failed, bare", e)
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
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
        // High accuracy only — avoid coarse/cell “SIM” pins that never move with the user.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setMinUpdateDistanceMeters(15f)
            .setWaitForAccurateLocation(true)
            .build()
        try {
            fused?.requestLocationUpdates(request, object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = pickFreshFix(result) ?: return
                    val id = eventId ?: return
                    scope.launch {
                        runCatching {
                            PukaarApp.instance.repository.updateLocation(
                                id, loc.latitude, loc.longitude, loc.accuracy.toDouble()
                            )
                        }
                    }
                }
            }, mainLooper)
            // Kick an immediate fresh GPS fix (not cached lastLocation).
            scope.launch {
                runCatching {
                    val id = eventId ?: return@runCatching
                    val loc = fetchFreshGps() ?: return@runCatching
                    PukaarApp.instance.repository.updateLocation(
                        id, loc.latitude, loc.longitude, loc.accuracy.toDouble()
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission missing — engine continues with last known path
        }
    }

    /** Posts a fresh high-accuracy GPS every 2 min (never stale lastLocation alone). */
    private fun startPeriodicLocationSync() {
        scope.launch {
            while (isActive) {
                delay(120_000L)
                val id = eventId ?: break
                if (!hasLocationPermission()) continue
                runCatching {
                    val loc = fetchFreshGps() ?: return@runCatching
                    PukaarApp.instance.repository.updateLocation(
                        id, loc.latitude, loc.longitude, loc.accuracy.toDouble()
                    )
                }.onFailure { e ->
                    android.util.Log.w("PUKAAR", "Periodic location sync failed: ${e.message}")
                }
            }
        }
    }

    private fun pickFreshFix(result: LocationResult): android.location.Location? {
        val candidates = result.locations.ifEmpty {
            listOfNotNull(result.lastLocation)
        }
        return candidates
            .filter { isUsableGps(it, strict = false) }
            .minByOrNull { it.accuracy }
    }

    /** Prefer a live GPS fix from this device — reject ancient/coarse cell pins. */
    private fun fetchFreshGps(): android.location.Location? {
        val client = fused ?: return null
        val token = CancellationTokenSource()
        val current = runCatching {
            Tasks.await(client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token))
        }.getOrNull()
        if (current != null && isUsableGps(current, strict = true)) return current
        val last = runCatching { Tasks.await(client.lastLocation) }.getOrNull()
        if (last != null && isUsableGps(last, strict = false)) return last
        return current ?: last
    }

    private fun isUsableGps(loc: android.location.Location, strict: Boolean): Boolean {
        if (!loc.hasAccuracy()) return loc.provider == android.location.LocationManager.GPS_PROVIDER
        val maxAccuracy = if (strict) 200f else 500f
        if (loc.accuracy > maxAccuracy) return false
        val ageMs = System.currentTimeMillis() - loc.time
        val maxAge = if (strict) 90_000L else 5 * 60_000L
        return ageMs in 0..maxAge
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
        val recordingStartedAt = System.currentTimeMillis()
        val maxRecordingMs = MAX_RECORDING_MS
        scope.launch {
            android.os.Handler(mainLooper).post {
                android.widget.Toast.makeText(
                    this@EmergencyForegroundService,
                    getString(R.string.emergency_recording_started),
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            while (isActive) {
                if (System.currentTimeMillis() - recordingStartedAt >= maxRecordingMs) {
                    android.util.Log.i("PUKAAR", "Audio recording auto-stopped after 30 minutes")
                    android.os.Handler(mainLooper).post {
                        android.widget.Toast.makeText(
                            this@EmergencyForegroundService,
                            getString(R.string.emergency_recording_auto_stopped),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    break
                }
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
            audio?.release()
            audio = null
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

        val ok = uploaded?.cloudSafe == true
        if (ok) {
            runCatching { pending.file.delete() }
        }
        return ok
    }

    private fun stopSelfSafe() {
        scope.cancel()
        audio?.release()
        wakeLock?.let { if (it.isHeld) it.release() }
        EmergencySessionStore.clear(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSelfSafe()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val MAX_RECORDING_MS = 30 * 60 * 1000L
        const val ACTION_STOP = "com.pukaar.app.STOP_EMERGENCY"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_IS_SOS = "is_sos"
        const val EXTRA_RECORD_AUDIO = "record_audio"

        fun start(context: Context, eventId: String, isSos: Boolean, recordAudio: Boolean = isSos) {
            if (!AppForegroundTracker.isInForeground) {
                android.util.Log.i("PUKAAR", "Emergency FGS deferred until app is foreground (event=$eventId)")
                return
            }
            val intent = Intent(context, EmergencyForegroundService::class.java).apply {
                putExtra(EXTRA_EVENT_ID, eventId)
                putExtra(EXTRA_IS_SOS, isSos)
                putExtra(EXTRA_RECORD_AUDIO, recordAudio)
            }
            runCatching { ContextCompatStart(context, intent) }
                .onFailure { android.util.Log.e("PUKAAR", "Could not start emergency service", it) }
        }

        fun resumeIfNeeded(context: Context) {
            if (!AppForegroundTracker.isInForeground) return
            val eventId = EmergencySessionStore.activeEventId(context) ?: return
            start(
                context,
                eventId,
                isSos = EmergencySessionStore.isSos(context) && !EmergencySessionStore.isMock(context),
                recordAudio = EmergencySessionStore.recordAudio(context)
            )
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, EmergencyForegroundService::class.java)) }
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
