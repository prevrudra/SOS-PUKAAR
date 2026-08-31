package com.pukaar.app.ui.screens.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.emergency.OemBatteryHelper
import com.pukaar.app.ui.theme.HelpOrange
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun HomeScreen(onMenu: () -> Unit, onEmergency: (String) -> Unit) {
    val context = LocalContext.current
    val session = PukaarApp.instance.sessionStore
    val homeMode by session.homeMode.collectAsState(initial = "SOS")
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= 33) needed += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= 29) needed += Manifest.permission.ACCESS_BACKGROUND_LOCATION
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        OemBatteryHelper.requestUnrestrictedBattery(context)
    }

    LaunchedEffect(homeMode) {
        if (homeMode == "HELP") {
            while (true) {
                runCatching { PukaarApp.instance.repository.heartbeat() }
                delay(30 * 60 * 1000L)
            }
        }
    }

    LaunchedEffect(Unit) {
        PukaarApp.instance.hardwareSos.collect {
            if (!busy) triggerEmergency(context, homeMode != "HELP", scope, onBusy = { busy = it }, onError = { error = it }, onEmergency = onEmergency)
        }
    }

    val isSos = homeMode != "HELP"
    val accent = if (isSos) SosRed else HelpOrange
    val label = if (isSos) "SOS" else "HELP"
    val instruction = if (isSos) "Tap when you are in danger" else "Tap when you need assistance"

    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text("PUKAAR", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.weight(0.35f))
        Button(
            onClick = {
                if (busy) return@Button
                triggerEmergency(context, isSos, scope, onBusy = { busy = it }, onError = { error = it }, onEmergency = onEmergency)
            },
            modifier = Modifier.size(260.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            enabled = !busy
        ) {
            Text(label, fontSize = 56.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text(instruction, color = PukaarMuted, textAlign = TextAlign.Center)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = SosRed, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(0.45f))
        Button(
            onClick = onMenu,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1C)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("MENU", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "PUKAAR does not guarantee rescue. It alerts trusted people and emergency pathways.",
            color = PukaarMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun triggerEmergency(
    context: android.content.Context,
    isSos: Boolean,
    scope: kotlinx.coroutines.CoroutineScope,
    onBusy: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onEmergency: (String) -> Unit
) {
    onBusy(true)
    onError(null)
    scope.launch {
        try {
            val loc = currentLocation(context)
            val trigger = if (isSos) "APP" else "HELP"
            val event = PukaarApp.instance.repository.trigger(
                TriggerRequest(
                    triggerType = trigger,
                    latitude = loc?.first,
                    longitude = loc?.second,
                    accuracyM = loc?.third,
                    mockDrill = false
                )
            )
            val id = event.id ?: return@launch
            EmergencyForegroundService.start(context, id, isSos)
            onEmergency(id)
        } catch (e: Exception) {
            onError(e.userMessage())
        } finally {
            onBusy(false)
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: android.content.Context): Triple<Double, Double, Double>? {
    return try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val loc = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
        if (loc != null) Triple(loc.latitude, loc.longitude, loc.accuracy.toDouble()) else null
    } catch (_: Exception) {
        null
    }
}
