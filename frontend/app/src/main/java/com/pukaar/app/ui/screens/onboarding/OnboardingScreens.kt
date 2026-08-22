package com.pukaar.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ProfileUpdateRequest
import com.pukaar.app.ui.theme.HelpOrange
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onDone()
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("PUKAAR", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text("Personal emergency help", color = PukaarMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(48.dp))
        Column {
            Text("PUKAAR", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text(
                "I don't have to think when something goes wrong. PUKAAR automatically starts the emergency process.",
                color = PukaarMuted,
                fontSize = 18.sp
            )
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun LanguageScreen(onNext: () -> Unit) {
    val langs = listOf("English", "हिन्दी", "मराठी", "தமிழ்", "తెలుగు", "বাংলা")
    var selected by remember { mutableStateOf("English") }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("Language", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        langs.forEach { lang ->
            val selectedNow = selected == lang
            Button(
                onClick = { selected = lang },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedNow) SosRed else Color(0xFF1C1C1C)
                )
            ) { Text(lang) }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)) {
            Text("Next")
        }
    }
}

@Composable
fun ConsentScreen(onNext: () -> Unit) {
    var terms by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("Consent", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Emergency location and audio recording activate only during an emergency workflow. Uploaded evidence is cloud-safe only after successful upload.",
            color = PukaarMuted
        )
        Spacer(Modifier.height(16.dp))
        ConsentCheck("I accept Terms & Privacy", terms) { terms = it }
        ConsentCheck("I consent to emergency location sharing", location) { location = it }
        ConsentCheck("I consent to emergency audio evidence", audio) { audio = it }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    runCatching {
                        PukaarApp.instance.repository.updateProfile(
                            ProfileUpdateRequest(
                                consentTerms = terms,
                                consentLocation = location,
                                consentAudio = audio
                            )
                        )
                    }
                    onNext()
                }
            },
            enabled = terms && location && audio,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text("Agree & Continue") }
    }
}

@Composable
private fun ConsentCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(checkedColor = SosRed))
        Text(label, color = Color.White)
    }
}

@Composable
fun OtpScreen(onNext: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("Verify mobile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone (+91...)") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        if (sent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("OTP") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            hint?.let { Text("Dev OTP: $it", color = PukaarMuted) }
        }
        error?.let { Text(it, color = SosRed) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    error = null
                    try {
                        if (!sent) {
                            val resp = PukaarApp.instance.repository.requestOtp(phone)
                            hint = resp.devCode
                            sent = true
                        } else {
                            PukaarApp.instance.repository.verifyOtp(phone, code)
                            onNext()
                        }
                    } catch (e: Exception) {
                        error = e.userMessage()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text(if (sent) "Verify" else "Send OTP") }
    }
}

@Composable
fun ProfileScreen(onNext: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("Your profile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Full name") },
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors()
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    runCatching {
                        PukaarApp.instance.repository.updateProfile(ProfileUpdateRequest(fullName = name))
                    }
                    onNext()
                }
            },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text("Continue") }
    }
}

@Composable
fun HomeModeScreen(onNext: () -> Unit) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Choose home mode", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("This decides your home button. Change later from MENU.", color = PukaarMuted)
        ModeCard("SOS", "Serious danger / emergency", SosRed) {
            scope.launch {
                PukaarApp.instance.repository.updateProfile(ProfileUpdateRequest(homeMode = "SOS"))
                PukaarApp.instance.sessionStore.setHomeMode("SOS")
                onNext()
            }
        }
        ModeCard("HELP", "Elderly assistance / family help", HelpOrange) {
            scope.launch {
                PukaarApp.instance.repository.updateProfile(ProfileUpdateRequest(homeMode = "HELP"))
                PukaarApp.instance.sessionStore.setHomeMode("HELP")
                onNext()
            }
        }
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
fun PermissionsScreen(onNext: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) {
        com.pukaar.app.emergency.OemBatteryHelper.requestUnrestrictedBattery(context)
        com.pukaar.app.emergency.OemBatteryHelper.requestOverlayPermission(context)
        onNext()
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Text("Permissions", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "PUKAAR needs location, microphone, notifications, phone, and battery unrestricted access so emergency automation can run when your screen is off.",
            color = PukaarMuted
        )
        Spacer(Modifier.height(16.dp))
        listOf(
            "Location (including background)",
            "Microphone for emergency audio",
            "Notifications (high priority)",
            "Phone / call pathway",
            "Ignore battery optimizations",
            "Display over other apps (OEM fallback)"
        ).forEach {
            Text("• $it", color = Color.White, modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                val needed = mutableListOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CALL_PHONE,
                    android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.READ_CONTACTS
                )
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    needed += android.Manifest.permission.POST_NOTIFICATIONS
                }
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    needed += android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                }
                launcher.launch(needed.toTypedArray())
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text("Grant & Continue") }
        TextButton(onClick = {
            com.pukaar.app.emergency.OemBatteryHelper.openOemAutostartSettings(context)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Open OEM autostart / battery settings", color = PukaarMuted)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = SosRed,
    unfocusedBorderColor = PukaarMuted,
    focusedLabelColor = PukaarMuted,
    unfocusedLabelColor = PukaarMuted,
    cursorColor = SosRed
)
