package com.pukaar.app.ui.screens.elderly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ElderlySettingsDto
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

@Composable
fun ElderlyHelpScreen(onBack: () -> Unit) {
    var settings by remember { mutableStateOf<ElderlySettingsDto?>(null) }
    var soft by remember { mutableStateOf("6") }
    var medium by remember { mutableStateOf("10") }
    var urgent by remember { mutableStateOf("12") }
    var monitoring by remember { mutableStateOf(true) }
    var ambulance by remember { mutableStateOf("108") }
    var doctorName by remember { mutableStateOf("") }
    var doctorPhone by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { PukaarApp.instance.repository.elderlySettings() }.onSuccess { s ->
            settings = s
            soft = (s.softHours ?: 6).toString()
            medium = (s.mediumHours ?: 10).toString()
            urgent = (s.urgentHours ?: 12).toString()
            monitoring = s.inactivityMonitoringEnabled != false
            ambulance = s.ambulanceNumber ?: "108"
            doctorName = s.doctorName ?: ""
            doctorPhone = s.doctorPhone ?: ""
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Elderly Help", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "HELP is not automatic police/112. Monitoring contacts get a call-first alert. Inactivity checks use: No qualifying activity detected.",
            color = PukaarMuted,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = monitoring, onCheckedChange = { monitoring = it },
                colors = CheckboxDefaults.colors(checkedColor = SosRed))
            Text("Enable inactivity monitoring", color = Color.White)
        }

        Spacer(Modifier.height(8.dp))
        NumberField("Soft alert (hours)", soft) { soft = it }
        NumberField("Medium alert (hours)", medium) { medium = it }
        NumberField("Urgent alert (hours)", urgent) { urgent = it }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = ambulance, onValueChange = { ambulance = it },
            label = { Text("Ambulance number") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = doctorName, onValueChange = { doctorName = it },
            label = { Text("Doctor name") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = doctorPhone, onValueChange = { doctorPhone = it },
            label = { Text("Doctor phone") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors())

        error?.let { Text(it, color = SosRed) }
        message?.let { Text(it, color = Color(0xFF66BB6A)) }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    error = null
                    try {
                        val updated = PukaarApp.instance.repository.updateElderlySettings(
                            ElderlySettingsDto(
                                softHours = soft.toIntOrNull() ?: 6,
                                mediumHours = medium.toIntOrNull() ?: 10,
                                urgentHours = urgent.toIntOrNull() ?: 12,
                                inactivityMonitoringEnabled = monitoring,
                                ambulanceNumber = ambulance,
                                doctorName = doctorName.ifBlank { null },
                                doctorPhone = doctorPhone.ifBlank { null }
                            )
                        )
                        settings = updated
                        message = "Settings saved"
                    } catch (e: Exception) {
                        error = e.userMessage()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text("SAVE SETTINGS") }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = SosRed,
    unfocusedBorderColor = PukaarMuted,
    focusedLabelColor = PukaarMuted,
    unfocusedLabelColor = PukaarMuted,
    cursorColor = SosRed
)
