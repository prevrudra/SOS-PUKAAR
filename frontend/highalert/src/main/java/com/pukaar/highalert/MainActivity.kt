package com.pukaar.highalert

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* monitoring still starts; user can retry from the button */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var phone by remember { mutableStateOf("") }
            var code by remember { mutableStateOf("") }
            var sent by remember { mutableStateOf(false) }
            var loggedIn by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                loggedIn = !HighAlertApp.instance.session.token().isNullOrBlank()
                if (loggedIn) {
                    enableGrabbing()
                }
            }

            Column(
                Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("PUKAAR", color = Color(0xFF22C55E), fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("High Alert", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "For trusted contacts only. Loud full-screen alerts when someone needs help.",
                    color = Color.Gray, fontSize = 13.sp
                )
                Spacer(Modifier.height(24.dp))

                if (loggedIn) {
                    Text("Monitoring active", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Keep notifications, battery unrestricted, and full-screen alerts enabled so grabbing alerts can open on a locked phone.",
                        color = Color.Gray, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { enableGrabbing() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enable grabbing alerts", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                HighAlertApp.instance.session.clear()
                                AlertMonitorService.stop(this@MainActivity)
                                loggedIn = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("Sign out") }
                } else {
                    androidx.compose.material3.OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Your phone (+91...)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (sent) {
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = code, onValueChange = { code = it },
                            label = { Text("OTP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    error?.let { Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                error = null
                                val api = AlertNetwork.api { null }
                                try {
                                    if (!sent) {
                                        api.requestOtp(OtpRequest(phone))
                                        sent = true
                                    } else {
                                        val resp = api.verifyOtp(OtpVerifyRequest(phone, code))
                                        val token = resp.accessToken ?: throw IllegalStateException("No token")
                                        HighAlertApp.instance.session.save(token, phone)
                                        val authedApi = AlertNetwork.api { token }
                                        authedApi.registerDevice(
                                            RegisterDeviceRequest(
                                                phone = phone,
                                                deviceId = "ha-${System.currentTimeMillis()}"
                                            )
                                        )
                                        enableGrabbing()
                                        loggedIn = true
                                    }
                                } catch (e: Exception) {
                                    error = e.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) { Text(if (sent) "Verify & Start Monitoring" else "Send OTP", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    private fun enableGrabbing() {
        AlertMonitorService.start(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            }
        }
    }
}
