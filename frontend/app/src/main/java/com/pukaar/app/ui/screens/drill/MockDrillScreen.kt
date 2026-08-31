package com.pukaar.app.ui.screens.drill

import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ContactDto
import com.pukaar.app.data.api.ContactRequest
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.ui.theme.TileGray
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DrillStep {
    START, ADD_CONTACTS, ADD_CONTACT_DETAIL, EMERGENCY_INFO, COUNTDOWN,
    AUDIO_RECORDING, ALERT_SENT, CONTACTS_RECEIVED, LIVE_LOCATION,
    BATTERY_NETWORK, OTHER_CONTACTS, NEARBY_SERVICES, CONFIRM, COMPLETE
}

@Composable
fun MockDrillScreen(onBack: () -> Unit, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(DrillStep.START) }
    var contacts by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var drillEvent by remember { mutableStateOf<EmergencyDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(5) }
    var countdownCancelled by remember { mutableStateOf(false) }
    var audioSecondsLeft by remember { mutableStateOf(30) }
    var contactsConfirmed by remember { mutableStateOf(false) }

    var addName by remember { mutableStateOf("") }
    var addPhone by remember { mutableStateOf("") }
    var verifyCode by remember { mutableStateOf("") }
    var pendingContactId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(step) {
        if (step == DrillStep.ADD_CONTACTS || step == DrillStep.OTHER_CONTACTS) {
            contacts = runCatching { PukaarApp.instance.repository.contacts() }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(step) {
        if (step == DrillStep.COUNTDOWN && !countdownCancelled) {
            countdown = 5
            repeat(5) {
                delay(1000)
                countdown--
                if (countdownCancelled) return@LaunchedEffect
            }
            if (!countdownCancelled) {
                runCatching {
                    val event = PukaarApp.instance.repository.trigger(
                        TriggerRequest(
                            triggerType = "MOCK_DRILL",
                            mockDrill = true,
                            latitude = 28.6139,
                            longitude = 77.2090,
                            accuracyM = 12.0
                        )
                    )
                    drillEvent = event
                    EmergencyForegroundService.start(context, event.id ?: "", true)
                    step = DrillStep.AUDIO_RECORDING
                }.onFailure { error = it.userMessage() }
            }
        }
    }

    LaunchedEffect(step) {
        if (step == DrillStep.AUDIO_RECORDING) {
            audioSecondsLeft = 30
            repeat(30) {
                delay(1000)
                audioSecondsLeft--
            }
            EmergencyForegroundService.stop(context)
            drillEvent = drillEvent?.id?.let {
                runCatching { PukaarApp.instance.repository.getEmergency(it) }.getOrNull()
            } ?: drillEvent
            step = DrillStep.ALERT_SENT
        }
    }

    val stepNumber = when (step) {
        DrillStep.START -> null
        DrillStep.ADD_CONTACTS -> 1
        DrillStep.ADD_CONTACT_DETAIL -> 2
        DrillStep.EMERGENCY_INFO -> 3
        DrillStep.COUNTDOWN -> 4
        DrillStep.AUDIO_RECORDING -> 5
        DrillStep.ALERT_SENT -> 6
        DrillStep.CONTACTS_RECEIVED -> 7
        DrillStep.LIVE_LOCATION -> 8
        DrillStep.BATTERY_NETWORK -> 9
        DrillStep.OTHER_CONTACTS -> 10
        DrillStep.NEARBY_SERVICES -> 11
        DrillStep.CONFIRM, DrillStep.COMPLETE -> null
        else -> null
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step != DrillStep.COMPLETE) {
                IconButton(onClick = {
                    when (step) {
                        DrillStep.START -> onBack()
                        DrillStep.ADD_CONTACT_DETAIL -> step = DrillStep.ADD_CONTACTS
                        else -> step = DrillStep.entries[step.ordinal - 1]
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            } else Spacer(Modifier.width(48.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (step) {
                        DrillStep.START -> "START DRILL"
                        DrillStep.COMPLETE -> "DRILL COMPLETE"
                        else -> "MOCK DRILL"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                stepNumber?.let {
                    Text("Step $it of 11", color = PukaarMuted, fontSize = 12.sp)
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (step) {
                DrillStep.START -> StartStep()
                DrillStep.ADD_CONTACTS -> AddContactsStep(contacts, onAdd = { step = DrillStep.ADD_CONTACT_DETAIL })
                DrillStep.ADD_CONTACT_DETAIL -> AddContactDetailStep(
                    name = addName,
                    phone = addPhone,
                    code = verifyCode,
                    onName = { addName = it },
                    onPhone = { addPhone = it },
                    onCode = { verifyCode = it },
                    error = error
                )
                DrillStep.EMERGENCY_INFO -> EmergencyInfoStep()
                DrillStep.COUNTDOWN -> CountdownStep(countdown)
                DrillStep.AUDIO_RECORDING -> AudioStep(audioSecondsLeft)
                DrillStep.ALERT_SENT -> AlertSentStep()
                DrillStep.CONTACTS_RECEIVED -> ContactsReceivedStep()
                DrillStep.LIVE_LOCATION -> LiveLocationStep(drillEvent)
                DrillStep.BATTERY_NETWORK -> BatteryNetworkStep(context)
                DrillStep.OTHER_CONTACTS -> OtherContactsStep(contacts)
                DrillStep.NEARBY_SERVICES -> NearbyServicesStep(drillEvent)
                DrillStep.CONFIRM -> ConfirmStep(
                    confirmed = contactsConfirmed,
                    onToggle = { contactsConfirmed = it }
                )
                DrillStep.COMPLETE -> CompleteStep()
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = SosRed, textAlign = TextAlign.Center)
            }
        }

        DrillFooter(
            step = step,
            contacts = contacts,
            busy = busy,
            contactsConfirmed = contactsConfirmed,
            onPrimary = {
                when (step) {
                    DrillStep.START -> step = DrillStep.ADD_CONTACTS
                    DrillStep.ADD_CONTACTS -> {
                        val verified = contacts.count { it.verified == true }
                        if (contacts.size < 2) error = "Add at least 2 trusted contacts"
                        else if (verified < 2) error = "Verify at least 2 contacts with the code"
                        else {
                            error = null
                            step = DrillStep.EMERGENCY_INFO
                        }
                    }
                    DrillStep.ADD_CONTACT_DETAIL -> {
                        busy = true
                        scope.launch {
                            try {
                                val c = PukaarApp.instance.repository.addContact(
                                    ContactRequest(addName.trim(), addPhone.trim())
                                )
                                pendingContactId = c.id
                                if (verifyCode == "123456" || verifyCode.length == 6) {
                                    c.id?.let { PukaarApp.instance.repository.verifyContact(it, verifyCode) }
                                    addName = ""
                                    addPhone = ""
                                    verifyCode = ""
                                    contacts = PukaarApp.instance.repository.contacts()
                                    step = DrillStep.ADD_CONTACTS
                                    error = null
                                } else {
                                    error = "Enter the 6-digit verification code (dev: 123456)"
                                }
                            } catch (e: Exception) {
                                error = e.userMessage()
                            } finally {
                                busy = false
                            }
                        }
                    }
                    DrillStep.EMERGENCY_INFO -> {
                        countdownCancelled = false
                        step = DrillStep.COUNTDOWN
                    }
                    DrillStep.COUNTDOWN -> Unit
                    DrillStep.AUDIO_RECORDING -> Unit
                    DrillStep.ALERT_SENT -> step = DrillStep.CONTACTS_RECEIVED
                    DrillStep.CONTACTS_RECEIVED -> step = DrillStep.LIVE_LOCATION
                    DrillStep.LIVE_LOCATION -> step = DrillStep.BATTERY_NETWORK
                    DrillStep.BATTERY_NETWORK -> step = DrillStep.OTHER_CONTACTS
                    DrillStep.OTHER_CONTACTS -> step = DrillStep.NEARBY_SERVICES
                    DrillStep.NEARBY_SERVICES -> step = DrillStep.CONFIRM
                    DrillStep.CONFIRM -> {
                        busy = true
                        scope.launch {
                            try {
                                val result = PukaarApp.instance.repository.completeLatestDrill(contactsConfirmed)
                                if (result.result == "PASS") {
                                    PukaarApp.instance.sessionStore.setMockDrillPassed(true)
                                    if (result.protectionReady == true) {
                                        PukaarApp.instance.sessionStore.setProtectionReady(true)
                                    }
                                    step = DrillStep.COMPLETE
                                } else {
                                    error = result.failureNotes ?: "Drill did not pass. Check contacts and try again."
                                }
                            } catch (e: Exception) {
                                error = e.userMessage()
                            } finally {
                                busy = false
                            }
                        }
                    }
                    DrillStep.COMPLETE -> onFinished()
                }
            },
            onSecondary = when (step) {
                DrillStep.COUNTDOWN -> ({
                    countdownCancelled = true
                    step = DrillStep.EMERGENCY_INFO
                })
                else -> null
            }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StartStep() {
    Icon(Icons.Default.Shield, null, tint = SosRed, modifier = Modifier.size(72.dp))
    Spacer(Modifier.height(16.dp))
    Text("5-Minute Drill", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "This drill builds confidence so you can act fearlessly in a real emergency. " +
                "You will test contacts, SOS countdown, test alert, location, and nearby services.",
        color = PukaarMuted,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}

@Composable
private fun AddContactsStep(contacts: List<ContactDto>, onAdd: () -> Unit) {
    Text("Add Trusted Contacts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("Add min 2 and max 3 trusted contacts.", color = PukaarMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    contacts.forEach { c ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(TileGray, RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(c.name ?: "", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(c.phone ?: "", color = PukaarMuted, fontSize = 13.sp)
            }
            if (c.verified == true) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
            } else {
                Text("Unverified", color = SosRed, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (contacts.size < 3) {
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("ADD CONTACT", color = SosRed)
        }
    }
}

@Composable
private fun AddContactDetailStep(
    name: String, phone: String, code: String,
    onName: (String) -> Unit, onPhone: (String) -> Unit, onCode: (String) -> Unit,
    error: String?
) {
    Text("Add Contact", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "Contacts must accept a verification code before they are added to your safety circle.",
        color = PukaarMuted,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(value = phone, onValueChange = onPhone, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(value = name, onValueChange = onName, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(value = code, onValueChange = onCode, label = { Text("Verification code") }, modifier = Modifier.fillMaxWidth())
    Text("Dev code: 123456", color = PukaarMuted, fontSize = 12.sp)
}

@Composable
private fun EmergencyInfoStep() {
    Text("In Case of Emergency", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier.size(120.dp).background(SosRed, CircleShape),
        contentAlignment = Alignment.Center
    ) { Text("SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp) }
    Spacer(Modifier.height(12.dp))
    Text("Press the large SOS button in the app", color = PukaarMuted, textAlign = TextAlign.Center)
    Spacer(Modifier.height(20.dp))
    Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(48.dp))
    Text("Or press Power Button 3 times quickly", color = PukaarMuted, textAlign = TextAlign.Center)
}

@Composable
private fun CountdownStep(seconds: Int) {
    Text("5-Second Countdown", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .size(160.dp)
            .border(4.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            seconds.coerceAtLeast(0).toString().padStart(2, '0'),
            color = SosRed,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black
        )
    }
    Spacer(Modifier.height(16.dp))
    Text(
        "Alert sends after 5 seconds. You can cancel if triggered accidentally.",
        color = PukaarMuted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AudioStep(secondsLeft: Int) {
    Icon(Icons.Default.Mic, null, tint = SosRed, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(12.dp))
    Text("Audio Recording Started", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(
        "In a real SOS, evidence is saved every minute to secure cloud storage once uploaded. " +
                "For this TEST drill, recording runs for 30 seconds on your device only.",
        color = PukaarMuted,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
    Spacer(Modifier.height(16.dp))
    Text("${secondsLeft}s", color = SosRed, fontSize = 36.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun AlertSentStep() {
    Icon(Icons.Default.NotificationsActive, null, tint = SosRed, modifier = Modifier.size(64.dp))
    Spacer(Modifier.height(12.dp))
    Text("High Alert Sent", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(
        "Trusted contacts receive a high-priority TEST alert that bypasses normal ringtones where supported.",
        color = PukaarMuted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ContactsReceivedStep() {
    Text("What Your Contacts Received", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    DrillInfoRow(Icons.Default.LocationOn, "Live Location", Color(0xFF42A5F5))
    DrillInfoRow(Icons.Default.BatteryChargingFull, "Battery % and Network Status", Color(0xFF66BB6A))
    DrillInfoRow(Icons.Default.Group, "Other Trusted Contacts & 112", Color.White)
    DrillInfoRow(Icons.Default.LocalPolice, "Nearest Police & Hospital details", Color(0xFF42A5F5))
}

@Composable
private fun LiveLocationStep(event: EmergencyDto?) {
    Text("Your Live Location", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(TileGray, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(40.dp))
            Text("Live Location ACTIVE", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold)
            event?.let {
                Text(
                    "${it.latitude?.let { la -> "%.4f".format(la) } ?: "—"}, ${it.longitude?.let { lo -> "%.4f".format(lo) } ?: "—"}",
                    color = PukaarMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun BatteryNetworkStep(context: android.content.Context) {
    val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as BatteryManager
    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    Text("Battery & Network", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BatteryFull, null, tint = Color(0xFF66BB6A), modifier = Modifier.size(48.dp))
            Text("$level%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SignalCellularAlt, null, tint = Color(0xFF66BB6A), modifier = Modifier.size(48.dp))
            Text("Strong", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OtherContactsStep(contacts: List<ContactDto>) {
    Text("Other Contacts & 112", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    contacts.forEach { c ->
        Text("${c.name} · ${c.phone}", color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
    }
    Spacer(Modifier.height(12.dp))
    val context = LocalContext.current
    OutlinedButton(onClick = {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
    }, modifier = Modifier.fillMaxWidth()) {
        Text("Emergency Number 112", color = SosRed)
    }
}

@Composable
private fun NearbyServicesStep(event: EmergencyDto?) {
    Text("Nearby Services Received", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    val police = event?.policeStation
    ServiceCard("Police", police?.name ?: "Nearest station when verified data exists", police?.phone)
    ServiceCard("Ambulance", "National emergency", "108")
    val hospital = event?.nearestHospital
    ServiceCard("Hospital", hospital?.name ?: "Nearest hospital when available", hospital?.phone)
    Text(
        "Verified numbers only — unverified police numbers are never shown.",
        color = PukaarMuted,
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ConfirmStep(confirmed: Boolean, onToggle: (Boolean) -> Unit) {
    Text("Confirm With Your Contacts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "Did your trusted contacts receive the TEST alert? Confirm before completing the drill.",
        color = PukaarMuted,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = confirmed, onCheckedChange = onToggle, colors = CheckboxDefaults.colors(checkedColor = SosRed))
        Text("My contacts received the test alert", color = Color.White)
    }
}

@Composable
private fun CompleteStep() {
    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(80.dp))
    Spacer(Modifier.height(16.dp))
    Text("Drill Complete!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Text(
        "Great job! You completed the mock drill. You are prepared. Stay alert, stay safe.",
        color = PukaarMuted,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}

@Composable
private fun DrillInfoRow(icon: ImageVector, label: String, tint: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White)
    }
}

@Composable
private fun ServiceCard(title: String, subtitle: String, phone: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(TileGray, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(subtitle, color = PukaarMuted, fontSize = 13.sp)
        phone?.let { Text(it, color = SosRed, fontSize = 14.sp) }
    }
}

@Composable
private fun DrillFooter(
    step: DrillStep,
    contacts: List<ContactDto>,
    busy: Boolean,
    contactsConfirmed: Boolean,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)?
) {
    val primaryLabel = when (step) {
        DrillStep.START -> "START DRILL"
        DrillStep.ADD_CONTACTS -> "PROCEED TO NEXT STEP"
        DrillStep.ADD_CONTACT_DETAIL -> "SEND VERIFICATION CODE"
        DrillStep.EMERGENCY_INFO, DrillStep.CONTACTS_RECEIVED, DrillStep.LIVE_LOCATION,
        DrillStep.BATTERY_NETWORK, DrillStep.OTHER_CONTACTS -> "PROCEED TO NEXT STEP"
        DrillStep.COUNTDOWN -> "Sending in…"
        DrillStep.AUDIO_RECORDING -> "Recording…"
        DrillStep.ALERT_SENT, DrillStep.NEARBY_SERVICES -> "NEXT"
        DrillStep.CONFIRM -> "CHECK WITH YOUR CONTACTS"
        DrillStep.COMPLETE -> "DRILL COMPLETE"
        else -> "NEXT"
    }
    val primaryEnabled = when (step) {
        DrillStep.COUNTDOWN, DrillStep.AUDIO_RECORDING -> false
        DrillStep.CONFIRM -> contactsConfirmed && !busy
        else -> !busy
    }
    if (step == DrillStep.COUNTDOWN && onSecondary != null) {
        OutlinedButton(
            onClick = onSecondary,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SosRed))
        ) { Text("CANCEL SOS", color = SosRed, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
    }
    Button(
        onClick = onPrimary,
        enabled = primaryEnabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SosRed),
        shape = RoundedCornerShape(12.dp)
    ) { Text(primaryLabel, fontWeight = FontWeight.Bold) }
}
