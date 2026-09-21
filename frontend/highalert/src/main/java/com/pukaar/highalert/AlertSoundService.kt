package com.pukaar.highalert

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Alarm-clock audio — keeps ringing on lock screen even when Activity is blocked.
 */
class AlertSoundService : Service() {
    private var player: MediaPlayer? = null
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var holdWakeLock: PowerManager.WakeLock? = null
    private var previousAlarmVolume: Int? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            onStartCommandInternal(intent)
        } catch (e: Exception) {
            android.util.Log.e("HighAlertSound", "Sound service failed — stopping", e)
            stopSelfSafe()
            START_NOT_STICKY
        }
    }

    private fun onStartCommandInternal(intent: Intent?): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafe()
            return START_NOT_STICKY
        }
        val who = intent?.getStringExtra(EXTRA_WHO)?.takeIf { it.isNotBlank() } ?: "PUKAAR user"
        val isHelp = intent?.getBooleanExtra(EXTRA_HELP, false) == true
        val mock = intent?.getBooleanExtra(EXTRA_MOCK, false) == true
        val alert = AlertRingState.getActive(this)
        wakeScreen()
        startAsForeground(who, isHelp, mock, alert)
        requestAlarmAudioFocus()
        boostVolume()
        startSound()
        startVibrate()
        return START_STICKY
    }

    private fun wakeScreen() {
        runCatching {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            screenWakeLock = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                "pukaar:alert_screen"
            ).also { it.acquire(10 * 60 * 1000L) }
            holdWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pukaar:alert_hold")
                .also { it.acquire(10 * 60 * 1000L) }
        }
    }

    private fun startAsForeground(
        who: String,
        isHelp: Boolean,
        mock: Boolean,
        alert: PendingAlertResponse?
    ) {
        ensureChannel()
        val fullScreenIntent = if (alert != null) {
            PendingIntent.getActivity(
                this, alert.eventId.hashCode(),
                AlertActivity.intent(this, alert).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val title = when {
            mock -> "PUKAAR TEST ALERT"
            isHelp -> "PUKAAR HELP — EMERGENCY"
            else -> "PUKAAR SOS — EMERGENCY"
        }
        val body = when {
            mock -> "$who activated a practice alert — tap to respond"
            isHelp -> "$who needs HELP — tap to respond"
            else -> "$who needs SOS help — tap to respond"
        }
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_stat_highalert)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setSound(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val notif = builder.build()
        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Alert alarm sound", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Loud alarm while SOS is active"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun requestAlarmAudioFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .build()
            am.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun boostVolume() {
        runCatching {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(
                AudioManager.STREAM_ALARM,
                am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                AudioManager.FLAG_SHOW_UI
            )
        }
    }

    private fun startSound() {
        runCatching { player?.stop(); player?.release() }
        player = MediaPlayer.create(this, R.raw.sos_alert)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
            }
            isLooping = true
            setVolume(1f, 1f)
            start()
        }
        if (player == null) {
            runCatching {
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@AlertSoundService, uri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }
    }

    private fun startVibrate() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 700, 250, 700, 250, 1100), 0)
            )
        }
    }

    private fun stopSelfSafe() {
        runCatching { player?.stop(); player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        previousAlarmVolume?.let { vol ->
            runCatching {
                (getSystemService(AUDIO_SERVICE) as AudioManager)
                    .setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
            }
        }
        previousAlarmVolume = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        }
        audioFocusRequest = null
        runCatching { if (screenWakeLock?.isHeld == true) screenWakeLock?.release() }
        runCatching { if (holdWakeLock?.isHeld == true) holdWakeLock?.release() }
        screenWakeLock = null
        holdWakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSelfSafe()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (AlertRingState.isRinging(this)) {
            start(this, "PUKAAR user", false, false, AlertRingState.getActive(this))
        }
    }

    companion object {
        private const val CHANNEL = "highalert_sound_v2"
        private const val NOTIF_ID = 9005
        const val ACTION_STOP = "com.pukaar.highalert.STOP_SOUND"
        private const val EXTRA_WHO = "who"
        private const val EXTRA_HELP = "help"
        private const val EXTRA_MOCK = "mock"

        fun start(
            context: Context,
            who: String,
            isHelp: Boolean,
            mock: Boolean,
            alert: PendingAlertResponse? = null
        ) {
            if (alert != null) AlertRingState.setActive(context, alert)
            val i = Intent(context, AlertSoundService::class.java).apply {
                putExtra(EXTRA_WHO, who)
                putExtra(EXTRA_HELP, isHelp)
                putExtra(EXTRA_MOCK, mock)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            }.onFailure { e ->
                android.util.Log.w("AlertSoundService", "FGS start blocked: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, AlertSoundService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
