package com.pukaar.app.ui.screens.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.ui.theme.HelpOrange
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmergencyActiveScreen(eventId: String, onClosed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<EmergencyDto?>(null) }

    LaunchedEffect(eventId) {
        while (true) {
            event = runCatching { PukaarApp.instance.repository.activeEmergency() }.getOrNull()
            delay(3000)
        }
    }

    val isHelp = event?.triggerType == "HELP"
    val accent = if (isHelp) HelpOrange else SosRed
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text(if (isHelp) "HELP ACTIVE" else "SOS ACTIVE", color = accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        StatusRow("Location", if (event?.latitude != null) "ACTIVE" else "ACQUIRING")
        StatusRow("Audio evidence", "RECORDING")
        val uploaded = event?.audioSegments?.count { it.cloudSafe == true } ?: 0
        val total = event?.audioSegments?.size ?: 0
        StatusRow("Cloud", if (total == 0) "WAITING" else "$uploaded/$total UPLOADED")
        StatusRow("112", event?.call112Status ?: "INITIATED")
        Spacer(Modifier.height(16.dp))
        Text("Trusted contacts", color = Color.White, fontWeight = FontWeight.Bold)
        event?.deliveries.orEmpty().forEach {
            Text("${it.name}: ${it.status}", color = PukaarMuted)
        }
        event?.policeStation?.let { p ->
            Spacer(Modifier.height(12.dp))
            Text("Nearest police: ${p.name}", color = Color.White)
            if (p.phoneVerified == true && !p.phone.isNullOrBlank()) {
                Text("Verified: ${p.phone}", color = PukaarMuted)
            } else {
                Text("Verified number unavailable — use 112", color = PukaarMuted)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:112"))
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C))
        ) { Text("CALL 112") }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                scope.launch {
                    runCatching { PukaarApp.instance.repository.markSafe(eventId) }
                    EmergencyForegroundService.stop(context)
                    onClosed()
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            shape = RoundedCornerShape(14.dp)
        ) { Text(if (isHelp) "I'M OK" else "I'M SAFE", fontWeight = FontWeight.Black, fontSize = 20.sp) }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PukaarMuted)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TrustedAlertScreen(eventId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("PUKAAR EMERGENCY ALERT", color = SosRed, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Text("A trusted person may be in danger. PUKAAR is sharing location and assistance options.", color = PukaarMuted)
        Spacer(Modifier.height(20.dp))
        ActionButton("CALL PERSON") {
            context.startActivity(Intent(Intent.ACTION_DIAL))
        }
        ActionButton("LIVE LOCATION") {}
        ActionButton("CALL 112") {
            context.startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:112")))
        }
        ActionButton("NEAREST POLICE") {}
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onBack) { Text("Back", color = PukaarMuted) }
        Text("Event: $eventId", color = PukaarMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C))
    ) { Text(label, fontWeight = FontWeight.Bold) }
}
