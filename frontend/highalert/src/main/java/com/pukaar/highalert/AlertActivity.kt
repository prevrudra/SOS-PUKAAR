package com.pukaar.highalert

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlertActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        playAlarm()
        vibrate()

        val name = intent.getStringExtra(EXTRA_NAME) ?: "Someone"
        val phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val battery = intent.getIntExtra(EXTRA_BATTERY, -1)
        val network = intent.getStringExtra(EXTRA_NETWORK) ?: ""
        val mock = intent.getBooleanExtra(EXTRA_MOCK, false)

        setContent {
            Column(
                Modifier.fillMaxSize().background(Color(0xFF7F1D1D)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (mock) "TEST ALERT" else "EMERGENCY SOS",
                    color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))
                Text(name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(phone, color = Color(0xFFFFE4E6), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                if (battery >= 0) Text("Battery: $battery%", color = Color.White)
                if (network.isNotBlank()) Text("Network: $network", color = Color.White)
                if (lat != 0.0 && lng != 0.0) {
                    Spacer(Modifier.height(8.dp))
                    Text("maps.google.com/?q=$lat,$lng", color = Color(0xFFBBF7D0), fontSize = 13.sp)
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        stopAlarm()
                        if (lat != 0.0 && lng != 0.0) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng")))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Open Location", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        stopAlarm()
                        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Call Now", color = Color.Black, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { stopAlarm(); finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Dismiss", color = Color.White) }
            }
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
            prepare()
            start()
        }
    }

    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 400, 800), 0))
        }
    }

    private fun stopAlarm() {
        player?.run { stop(); release() }
        player = null
    }

    override fun onDestroy() {
        stopAlarm()
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
            }
    }
}
