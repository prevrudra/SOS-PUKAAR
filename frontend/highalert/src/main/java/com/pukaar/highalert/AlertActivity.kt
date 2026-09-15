package com.pukaar.highalert

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pukaar.highalert.data.AlertUiMapper
import com.pukaar.highalert.ui.SampleAlertScreen
import com.pukaar.highalert.ui.theme.PukaarAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AlertActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var previousAlarmVolume: Int? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        runCatching {
            val km = getSystemService(KeyguardManager::class.java)
            if (km.isKeyguardLocked) km.requestDismissKeyguard(this, null)
        }
        getSystemService(NotificationManager::class.java)?.cancel(AlertMonitorService.ALERT_NOTIF_ID)
        boostAlarmVolume()
        playAlarm()
        startVibrate()

        val initial = intentToAlert(intent)
        val eventId = initial.eventId
        val mock = initial.mockDrill == true

        if (!eventId.isNullOrBlank()) {
            Thread {
                runCatching {
                    val session = AlertSession(this)
                    runBlocking { session.markEventHandled(eventId) }
                    val api = AlertNetwork.api { runBlocking { session.token() } }
                    runBlocking { api.acknowledge(AcknowledgeRequest(eventId, "DELIVERED")) }
                }
            }.start()
        }

        setContent {
            var alert by remember { mutableStateOf(initial) }

            LaunchedEffect(eventId) {
                if (eventId.isNullOrBlank()) return@LaunchedEffect
                repeat(24) {
                    delay(if (it == 0) 400L else 2_500L)
                    val snap = runCatching {
                        val session = AlertSession(this@AlertActivity)
                        val token = session.token()
                        AlertNetwork.api { token }.eventSnapshot(eventId)
                    }.getOrNull() ?: return@repeat
                    if (snap.active == false) return@LaunchedEffect
                    alert = mergeAlert(alert, snap)
                    if ((snap.latitude ?: 0.0) != 0.0 && (snap.longitude ?: 0.0) != 0.0 && it >= 4) {
                        return@LaunchedEffect
                    }
                }
            }

            val ui = remember(alert) { AlertUiMapper.fromPending(alert, mock) }

            PukaarAlertTheme {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    SampleAlertScreen(
                        alert = ui,
                        onBack = null,
                        title = if (mock) "PUKAAR TEST ALERT" else "PUKAAR SOS Alert",
                        onBeforeAction = {
                            stopAllAlerts()
                            markRead(eventId)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    TextButton(
                        onClick = {
                            stopAllAlerts()
                            markRead(eventId)
                            finish()
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Stop alert", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    private fun markRead(eventId: String?) {
        if (eventId.isNullOrBlank()) return
        Thread {
            runCatching {
                val session = AlertSession(this)
                runBlocking { session.markEventHandled(eventId) }
                val api = AlertNetwork.api { runBlocking { session.token() } }
                runBlocking { api.acknowledge(AcknowledgeRequest(eventId, "READ")) }
            }
        }.start()
    }

    private fun boostAlarmVolume() {
        runCatching {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            previousAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }
    }

    private fun playAlarm() {
        player = MediaPlayer.create(this, R.raw.sos_alert)?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
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
                    setDataSource(this@AlertActivity, uri)
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
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 250, 700, 250, 1100), 0))
        }
    }

    private fun stopAllAlerts() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        previousAlarmVolume?.let { vol ->
            runCatching {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.setStreamVolume(AudioManager.STREAM_ALARM, vol, 0)
            }
        }
        previousAlarmVolume = null
        getSystemService(NotificationManager::class.java)?.cancel(AlertMonitorService.ALERT_NOTIF_ID)
    }

    override fun onDestroy() {
        stopAllAlerts()
        super.onDestroy()
    }

    companion object {
        fun intent(ctx: Context, alert: PendingAlertResponse): Intent =
            Intent(ctx, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("name", alert.victimName)
                putExtra("phone", alert.victimPhone)
                putExtra("subtitle", alert.victimSubtitle)
                putExtra("lat", alert.latitude ?: 0.0)
                putExtra("lng", alert.longitude ?: 0.0)
                putExtra("battery", alert.batteryPct ?: -1)
                putExtra("network", alert.networkType)
                putExtra("mock", alert.mockDrill == true)
                putExtra("event_id", alert.eventId)
                putExtra("police_name", alert.policeName)
                putExtra("police_phone", alert.policePhone)
                putExtra("police_address", alert.policeAddress)
                putExtra("hospital_name", alert.hospitalName)
                putExtra("hospital_phone", alert.hospitalPhone)
                putExtra("hospital_address", alert.hospitalAddress)
                putExtra("ambulance_name", alert.ambulanceName)
                putExtra("ambulance_phone", alert.ambulancePhone)
                putExtra("ambulance_address", alert.ambulanceAddress)
                putExtra("location_label", alert.locationLabel)
                putExtra("trigger_type", alert.triggerType)
                putExtra("started_at", alert.startedAt)
            }

        private fun intentToAlert(intent: Intent): PendingAlertResponse = PendingAlertResponse(
            active = true,
            eventId = intent.getStringExtra("event_id"),
            victimName = intent.getStringExtra("name"),
            victimPhone = intent.getStringExtra("phone"),
            victimSubtitle = intent.getStringExtra("subtitle"),
            latitude = intent.getDoubleExtra("lat", 0.0).takeIf { it != 0.0 },
            longitude = intent.getDoubleExtra("lng", 0.0).takeIf { it != 0.0 },
            locationLabel = intent.getStringExtra("location_label"),
            batteryPct = intent.getIntExtra("battery", -1).takeIf { it >= 0 },
            networkType = intent.getStringExtra("network"),
            mockDrill = intent.getBooleanExtra("mock", false),
            triggerType = intent.getStringExtra("trigger_type"),
            startedAt = intent.getStringExtra("started_at"),
            policeName = intent.getStringExtra("police_name"),
            policePhone = intent.getStringExtra("police_phone"),
            policeAddress = intent.getStringExtra("police_address"),
            hospitalName = intent.getStringExtra("hospital_name"),
            hospitalPhone = intent.getStringExtra("hospital_phone"),
            hospitalAddress = intent.getStringExtra("hospital_address"),
            ambulanceName = intent.getStringExtra("ambulance_name"),
            ambulancePhone = intent.getStringExtra("ambulance_phone"),
            ambulanceAddress = intent.getStringExtra("ambulance_address")
        )

        private fun mergeAlert(old: PendingAlertResponse, snap: PendingAlertResponse): PendingAlertResponse =
            old.copy(
                victimName = snap.victimName ?: old.victimName,
                victimPhone = snap.victimPhone ?: old.victimPhone,
                victimSubtitle = snap.victimSubtitle ?: old.victimSubtitle,
                latitude = snap.latitude ?: old.latitude,
                longitude = snap.longitude ?: old.longitude,
                locationLabel = snap.locationLabel ?: old.locationLabel,
                batteryPct = snap.batteryPct ?: old.batteryPct,
                networkType = snap.networkType ?: old.networkType,
                triggerType = snap.triggerType ?: old.triggerType,
                startedAt = snap.startedAt ?: old.startedAt,
                policeName = snap.policeName ?: old.policeName,
                policePhone = snap.policePhone ?: old.policePhone,
                policeAddress = snap.policeAddress ?: old.policeAddress,
                hospitalName = snap.hospitalName ?: old.hospitalName,
                hospitalPhone = snap.hospitalPhone ?: old.hospitalPhone,
                hospitalAddress = snap.hospitalAddress ?: old.hospitalAddress,
                ambulanceName = snap.ambulanceName ?: old.ambulanceName,
                ambulancePhone = snap.ambulancePhone ?: old.ambulancePhone,
                ambulanceAddress = snap.ambulanceAddress ?: old.ambulanceAddress,
                trustedContacts = snap.trustedContacts ?: old.trustedContacts,
                helpNumbers = snap.helpNumbers ?: old.helpNumbers
            )
    }
}
