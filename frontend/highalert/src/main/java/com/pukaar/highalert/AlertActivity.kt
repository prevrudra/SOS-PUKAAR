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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.highalert.data.AlertUiMapper
import com.pukaar.highalert.ui.SampleAlertScreen
import com.pukaar.highalert.ui.theme.PukaarAlertTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope

class AlertActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var previousAlarmVolume: Int? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAlarmWindowFlags()
        // Keep the full-screen notification — cancelling it on create makes Oppo
        // drop the lock-screen alarm UI until the user unlocks.
        boostAlarmVolume()
        playAlarm()
        startVibrate()

        val initial = intentToAlert(intent)
        val eventId = initial.eventId
        val mock = initial.mockDrill == true

        // Do NOT ack DELIVERED here — wait until UI is visible (onResume).
        // Early ack previously killed the pending-poll fallback if OEM killed the Activity.

        setContent {
            var alert by remember { mutableStateOf(initial) }

            DisposableEffect(eventId) {
                val listener: (PendingAlertResponse) -> Unit = { updated ->
                    if (updated.eventId == eventId) alert = updated
                }
                AlertFireHelper.setDataListener(listener)
                onDispose { AlertFireHelper.setDataListener(null) }
            }

            LaunchedEffect(eventId) {
                if (eventId.isNullOrBlank()) return@LaunchedEffect
                AlertDataFetcher.fetchEventSnapshot(this@AlertActivity, eventId)?.let { snap ->
                    if (snap.active == true) {
                        alert = AlertMerge.merge(alert, snap)
                        AlertFireHelper.updateAlertData(this@AlertActivity, snap)
                    }
                }
                // Keep polling so "I'm Safe" from the victim stops the loud alert.
                repeat(120) {
                    delay(if (it < 5) 1_000L else 2_500L)
                    val snap = runCatching {
                        val session = AlertSession(this@AlertActivity)
                        val token = session.token()
                        AlertNetwork.api { token }.eventSnapshot(eventId)
                    }.getOrNull()
                    if (snap != null) {
                        if (snap.active == false) {
                            stopAllAlerts()
                            finish()
                            return@LaunchedEffect
                        }
                        alert = AlertMerge.merge(alert, snap)
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
                    Button(
                        onClick = {
                            stopAllAlerts()
                            markRead(eventId)
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111827),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Stop alert",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val updated = intentToAlert(intent)
        lifecycleScope.launch {
            AlertFireHelper.updateAlertData(this@AlertActivity, updated)
        }
    }

    override fun onResume() {
        super.onResume()
        applyAlarmWindowFlags()
        AlertRingState.getActive(this)?.let { stored ->
            if (!stored.eventId.isNullOrBlank()) {
                lifecycleScope.launch {
                    AlertDataFetcher.fetchEventSnapshot(this@AlertActivity, stored.eventId!!)?.let { snap ->
                        if (snap.active == true) {
                            AlertFireHelper.updateAlertData(this@AlertActivity, snap)
                        }
                    }
                }
            }
        }
        val eventId = intent.getStringExtra("event_id")
        if (!eventId.isNullOrBlank() && !deliveredAcked) {
            deliveredAcked = true
            Thread {
                runCatching {
                    val session = AlertSession(this)
                    // Keep local "handled" only after UI is up — poll can still re-fire if process dies.
                    val api = AlertNetwork.api { runBlocking { session.token() } }
                    runBlocking { api.acknowledge(AcknowledgeRequest(eventId, "DELIVERED")) }
                }
            }.start()
        }
    }

    private var deliveredAcked = false

    private fun applyAlarmWindowFlags() {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setInheritShowWhenLocked(true)
        }
        runCatching {
            val km = getSystemService(KeyguardManager::class.java)
            if (km.isKeyguardLocked) km.requestDismissKeyguard(this, null)
        }
    }

    private fun stopAllAlerts() {
        AlertFireHelper.dismissRinging(this)
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
        // Only stop Activity-local MediaPlayer/vibrator. Do NOT call dismissRinging —
        // OEMs often destroy AlertActivity while the alarm must keep ringing via
        // AlertSoundService + AlarmClockRinger until the user taps Stop.
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
        super.onDestroy()
    }

    companion object {
        fun intent(ctx: Context, alert: PendingAlertResponse): Intent =
            Intent(ctx, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
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
                putExtra(
                    "trusted_json",
                    contactsToJson(alert.trustedContacts)
                )
                putExtra(
                    "help_json",
                    contactsToJson(alert.helpNumbers)
                )
            }

        private fun contactsToJson(list: List<AlertContactDto>?): String {
            if (list.isNullOrEmpty()) return "[]"
            return list.joinToString(prefix = "[", postfix = "]") { c ->
                val name = (c.name ?: "").replace("\"", "'")
                val phone = (c.phone ?: "").replace("\"", "'")
                val role = (c.role ?: "").replace("\"", "'")
                val rel = (c.relationship ?: "").replace("\"", "'")
                val status = (c.status ?: "").replace("\"", "'")
                """{"name":"$name","phone":"$phone","role":"$role","relationship":"$rel","status":"$status"}"""
            }
        }

        private fun contactsFromJson(raw: String?): List<AlertContactDto>? {
            if (raw.isNullOrBlank() || raw == "[]") return null
            return runCatching {
                val regex = Regex(
                    """\{"name":"(.*?)","phone":"(.*?)","role":"(.*?)","relationship":"(.*?)","status":"(.*?)"\}"""
                )
                regex.findAll(raw).map {
                    AlertContactDto(
                        name = it.groupValues[1].ifBlank { null },
                        phone = it.groupValues[2].ifBlank { null },
                        role = it.groupValues[3].ifBlank { null },
                        relationship = it.groupValues[4].ifBlank { null },
                        status = it.groupValues[5].ifBlank { null }
                    )
                }.toList().ifEmpty { null }
            }.getOrNull()
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
            ambulanceAddress = intent.getStringExtra("ambulance_address"),
            trustedContacts = contactsFromJson(intent.getStringExtra("trusted_json")),
            helpNumbers = contactsFromJson(intent.getStringExtra("help_json"))
        )

    }
}
