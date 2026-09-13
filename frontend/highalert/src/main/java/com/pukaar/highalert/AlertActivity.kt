package com.pukaar.highalert

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.heightIn
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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

        val initialName = intent.getStringExtra(EXTRA_NAME) ?: "Someone"
        val initialPhone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        val initialLat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val initialLng = intent.getDoubleExtra(EXTRA_LNG, 0.0)
        val initialBattery = intent.getIntExtra(EXTRA_BATTERY, -1)
        val initialNetwork = intent.getStringExtra(EXTRA_NETWORK) ?: ""
        val mock = intent.getBooleanExtra(EXTRA_MOCK, false)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val initialPoliceName = intent.getStringExtra(EXTRA_POLICE_NAME)
        val initialPolicePhone = intent.getStringExtra(EXTRA_POLICE_PHONE)
        val initialHospitalName = intent.getStringExtra(EXTRA_HOSPITAL_NAME)
        val initialHospitalPhone = intent.getStringExtra(EXTRA_HOSPITAL_PHONE)

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
            val scope = rememberCoroutineScope()
            var name by remember { mutableStateOf(initialName) }
            var phone by remember { mutableStateOf(initialPhone) }
            var lat by remember { mutableDoubleStateOf(initialLat) }
            var lng by remember { mutableDoubleStateOf(initialLng) }
            var battery by remember { mutableIntStateOf(initialBattery) }
            var network by remember { mutableStateOf(initialNetwork) }
            var policeName by remember { mutableStateOf(initialPoliceName) }
            var policePhone by remember { mutableStateOf(initialPolicePhone) }
            var hospitalName by remember { mutableStateOf(initialHospitalName) }
            var hospitalPhone by remember { mutableStateOf(initialHospitalPhone) }
            var showFullMessage by remember { mutableStateOf(false) }

            LaunchedEffect(eventId) {
                if (eventId.isNullOrBlank()) return@LaunchedEffect
                // Keep refreshing until location/services arrive (GPS often lands a few seconds after SOS)
                repeat(20) {
                    delay(2_500L)
                    val snap = runCatching {
                        val session = AlertSession(this@AlertActivity)
                        val token = session.token()
                        val api = AlertNetwork.api { token }
                        api.eventSnapshot(eventId)
                    }.getOrNull() ?: return@repeat
                    if (snap.active == false) return@LaunchedEffect
                    snap.victimName?.takeIf { it.isNotBlank() }?.let { name = it }
                    snap.victimPhone?.takeIf { it.isNotBlank() }?.let { phone = it }
                    snap.latitude?.let { lat = it }
                    snap.longitude?.let { lng = it }
                    snap.batteryPct?.let { battery = it }
                    snap.networkType?.takeIf { it.isNotBlank() }?.let { network = it }
                    snap.policeName?.let { policeName = it }
                    snap.policePhone?.let { policePhone = it }
                    snap.hospitalName?.let { hospitalName = it }
                    snap.hospitalPhone?.let { hospitalPhone = it }
                    if ((snap.latitude ?: 0.0) != 0.0 && (snap.longitude ?: 0.0) != 0.0) {
                        // Keep a couple more refreshes for nearby names, then stop
                        if (it >= 3) return@LaunchedEffect
                    }
                }
            }

            val pulse = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by pulse.animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "pulseA"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A0505), Color(0xFF7F1D1D), Color(0xFF450A0A))
                        )
                    )
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "PUKAAR",
                        color = Color(0xFFFECACA),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .size(72.dp)
                            .alpha(pulseAlpha)
                            .background(Color(0xFFEF4444), CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (mock) "PUKAAR TEST ALERT" else "PUKAAR SOS — EMERGENCY",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (mock) {
                            "$name has activated a practice SOS."
                        } else {
                            "$name has activated SOS and may need immediate help."
                        },
                        color = Color(0xFFFEE2E2),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { showFullMessage = true }) {
                        Text(
                            "View full message",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    InfoStrip(
                        listOfNotNull(
                            phone.takeIf { it.isNotBlank() }?.let { "User: $it" },
                            if (lat != 0.0 && lng != 0.0) "Current location: live — open map below"
                            else "Current location: acquiring…",
                            if (battery >= 0) "Battery: $battery%" else null,
                            network.takeIf { it.isNotBlank() }?.let { "Network: $it" }
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "NEAREST EMERGENCY SERVICES",
                        color = Color(0xFFFECACA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    ServiceCard(
                        title = "Police",
                        name = policeName ?: "Police Emergency",
                        phone = policePhone ?: "100",
                        onCall = {
                            stopAllAlerts()
                            dial(policePhone ?: "100")
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                    ServiceCard(
                        title = "Hospital",
                        name = hospitalName ?: "Nearest Hospital",
                        phone = hospitalPhone ?: "112",
                        onCall = {
                            stopAllAlerts()
                            dial(hospitalPhone ?: "112")
                        }
                    )

                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            stopAllAlerts()
                            markRead(eventId)
                            if (phone.isNotBlank()) dial(phone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Call now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            stopAllAlerts()
                            markRead(eventId)
                            if (lat != 0.0 && lng != 0.0) {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://maps.google.com/?q=$lat,$lng")
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        Text("Open map", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            stopAllAlerts()
                            scope.launch { markRead(eventId) }
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop alert", color = Color(0xFFFECACA), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (showFullMessage) {
                val fullMessage = buildFullAlertMessage(
                    name = name,
                    phone = phone,
                    mock = mock,
                    lat = lat,
                    lng = lng,
                    battery = battery,
                    network = network,
                    policeName = policeName,
                    policePhone = policePhone,
                    hospitalName = hospitalName,
                    hospitalPhone = hospitalPhone
                )
                val clipboard = LocalClipboardManager.current
                AlertDialog(
                    onDismissRequest = { showFullMessage = false },
                    containerColor = Color(0xFF1F0A0A),
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFFFEE2E2),
                    title = {
                        Text(
                            if (mock) "Full test alert message" else "Full SOS message",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                fullMessage,
                                color = Color(0xFFFEE2E2),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(fullMessage))
                            Toast.makeText(this@AlertActivity, "Message copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            runCatching {
                                startActivity(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, fullMessage)
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            if (mock) "PUKAAR TEST ALERT" else "PUKAAR SOS — EMERGENCY"
                                        )
                                    }
                                )
                            }
                        }) {
                            Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { showFullMessage = false }) {
                            Text("Close", color = Color(0xFFFECACA))
                        }
                    }
                )
            }
        }
    }

    private fun buildFullAlertMessage(
        name: String,
        phone: String,
        mock: Boolean,
        lat: Double,
        lng: Double,
        battery: Int,
        network: String,
        policeName: String?,
        policePhone: String?,
        hospitalName: String?,
        hospitalPhone: String?
    ): String {
        val sb = StringBuilder()
        sb.append(if (mock) "PUKAAR TEST ALERT" else "PUKAAR SOS — EMERGENCY").append('\n')
        sb.append(name)
        if (phone.isNotBlank()) sb.append(" (").append(phone).append(')')
        sb.append('\n')
        sb.append(
            if (mock) "This is a practice drill."
            else "MAY BE IN DANGER — call immediately."
        ).append('\n')
        if (lat != 0.0 && lng != 0.0) {
            sb.append("Location: https://maps.google.com/?q=").append(lat).append(',').append(lng).append('\n')
        } else {
            sb.append("Location: acquiring / not available yet — call now.\n")
        }
        if (battery >= 0) sb.append("Battery: ").append(battery).append("%\n")
        if (network.isNotBlank()) sb.append("Network: ").append(network).append('\n')
        sb.append("Emergency: 112\n")
        sb.append("Police: ").append(policeName ?: "Police Emergency")
        if (!policePhone.isNullOrBlank()) sb.append(' ').append(policePhone)
        sb.append('\n')
        sb.append("Hospital: ").append(hospitalName ?: "Nearest Hospital")
        if (!hospitalPhone.isNullOrBlank()) sb.append(' ').append(hospitalPhone)
        sb.append('\n')
        sb.append("Open PUKAAR High Alert for live updates.")
        return sb.toString().trim()
    }

    @androidx.compose.runtime.Composable
    private fun InfoStrip(items: List<String>) {
        if (items.isEmpty()) return
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach {
                Text(it, color = Color(0xFFFEE2E2), fontSize = 13.sp)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun ServiceCard(title: String, name: String, phone: String, onCall: () -> Unit) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(title.uppercase(), color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(phone, color = Color(0xFFE5E7EB), fontSize = 14.sp)
                TextButton(onClick = onCall) {
                    Text("Call", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    private fun dial(number: String) {
        if (number.isBlank()) return
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
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
            // Fallback if raw tone missing
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
        const val EXTRA_NAME = "name"
        const val EXTRA_PHONE = "phone"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_BATTERY = "battery"
        const val EXTRA_NETWORK = "network"
        const val EXTRA_MOCK = "mock"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_POLICE_NAME = "police_name"
        const val EXTRA_POLICE_PHONE = "police_phone"
        const val EXTRA_HOSPITAL_NAME = "hospital_name"
        const val EXTRA_HOSPITAL_PHONE = "hospital_phone"

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
                putExtra(EXTRA_POLICE_NAME, alert.policeName)
                putExtra(EXTRA_POLICE_PHONE, alert.policePhone)
                putExtra(EXTRA_HOSPITAL_NAME, alert.hospitalName)
                putExtra(EXTRA_HOSPITAL_PHONE, alert.hospitalPhone)
            }
    }
}
