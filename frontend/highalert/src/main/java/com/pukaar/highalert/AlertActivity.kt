package com.pukaar.highalert

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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

        val name = intent.getStringExtra(EXTRA_NAME) ?: "Someone"
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val battery = intent.getIntExtra(EXTRA_BATTERY, -1)
        val network = intent.getStringExtra(EXTRA_NETWORK) ?: ""
        val mock = intent.getBooleanExtra(EXTRA_MOCK, false)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)

        if (!eventId.isNullOrBlank()) {
            Thread {
                runCatching {
                    val session = AlertSession(this)
                    val api = AlertNetwork.api { runBlocking { session.token() } }
                    runBlocking { api.acknowledge(AcknowledgeRequest(eventId, "DELIVERED")) }
                }
            }.start()
        }

        setContent {
            val scope = rememberCoroutineScope()
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF7F1D1D))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (mock) "TEST ALERT" else "EMERGENCY SOS",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (phone.isNotBlank()) Text(phone, color = Color(0xFFFFE4E6), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                if (battery >= 0) Text("Battery: $battery%", color = Color.White)
                if (network.isNotBlank()) Text("Network: $network", color = Color.White)
                if (lat != 0.0 && lng != 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Live location ready", color = Color(0xFFBBF7D0), fontSize = 14.sp)
                }
                Spacer(Modifier.height(28.dp))
                Button(
                    onClick = {
                        stopAllAlerts()
                        markRead(eventId)
                        if (lat != 0.0 && lng != 0.0) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng")))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("Open map", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        stopAllAlerts()
                        markRead(eventId)
                        if (phone.isNotBlank()) {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("Call now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        stopAllAlerts()
                        scope.launch { markRead(eventId) }
                        finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) { Text("Stop alert", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
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
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(this@AlertActivity, uri)
            isLooping = true
            setVolume(1f, 1f)
            prepare()
            start()
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
            // repeat index 0 = loop until cancel()
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800, 400, 1200), 0))
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
        const val EXTRA_NAME = "name"
        const val EXTRA_PHONE = "phone"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_BATTERY = "battery"
        const val EXTRA_NETWORK = "network"
        const val EXTRA_MOCK = "mock"
        const val EXTRA_EVENT_ID = "event_id"

        fun intent(ctx: Context, alert: PendingAlertResponse): Intent =
            Intent(ctx, AlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAME, alert.victimName)
                putExtra(EXTRA_PHONE, alert.victimPhone)
                putExtra(EXTRA_LAT, alert.latitude ?: 0.0)
                putExtra(EXTRA_LNG, alert.longitude ?: 0.0)
                putExtra(EXTRA_BATTERY, alert.batteryPct ?: -1)
                putExtra(EXTRA_NETWORK, alert.networkType)
                putExtra(EXTRA_MOCK, alert.mockDrill == true)
                putExtra(EXTRA_EVENT_ID, alert.eventId)
            }
    }
}
