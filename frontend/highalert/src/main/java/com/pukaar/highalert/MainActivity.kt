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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var phoneDigits by remember { mutableStateOf("") }
            var otp by remember { mutableStateOf("") }
            var step by remember { mutableStateOf(Step.Phone) }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            var activePhone by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            val phoneE164 = remember(phoneDigits) { toE164(phoneDigits) }

            LaunchedEffect(Unit) {
                if (!HighAlertApp.instance.session.token().isNullOrBlank()) {
                    activePhone = HighAlertApp.instance.session.phone().orEmpty()
                    step = Step.Active
                    enableGrabbing()
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("PUKAAR", color = Color(0xFF22C55E), fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("High Alert", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (step) {
                            Step.Phone -> "Enter your phone number to receive SOS alerts."
                            Step.Otp -> "Enter the 6-digit code we sent by SMS."
                            Step.Active -> "You will get a loud full-screen alert when someone needs you."
                        },
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(28.dp))

                    when (step) {
                        Step.Phone -> {
                            FieldLabel("Phone number")
                            SimpleBoxField(
                                value = phoneDigits,
                                onValueChange = {
                                    phoneDigits = it.filter(Char::isDigit).take(10)
                                    error = null
                                },
                                placeholder = "10-digit mobile",
                                keyboardType = KeyboardType.Phone,
                                prefix = "+91"
                            )
                            error?.let { Err(it) }
                            Spacer(Modifier.height(20.dp))
                            PrimaryButton(
                                text = if (loading) "Sending…" else "Continue",
                                enabled = phoneDigits.length == 10 && !loading
                            ) {
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        AlertNetwork.api { null }.requestOtp(OtpRequest(phoneE164))
                                        otp = ""
                                        step = Step.Otp
                                    } catch (e: Exception) {
                                        error = friendlyError(e)
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "One SMS code confirms this phone is yours so alerts go to the right person.",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }

                        Step.Otp -> {
                            FieldLabel("OTP code")
                            SimpleBoxField(
                                value = otp,
                                onValueChange = {
                                    otp = it.filter(Char::isDigit).take(6)
                                    error = null
                                },
                                placeholder = "6 digits",
                                keyboardType = KeyboardType.NumberPassword
                            )
                            error?.let { Err(it) }
                            Spacer(Modifier.height(20.dp))
                            PrimaryButton(
                                text = if (loading) "Starting…" else "Start alerts",
                                enabled = otp.length == 6 && !loading
                            ) {
                                scope.launch {
                                    loading = true
                                    error = null
                                    try {
                                        val api = AlertNetwork.api { null }
                                        val resp = api.verifyOtp(OtpVerifyRequest(phoneE164, otp))
                                        val token = resp.accessToken ?: error("Could not sign in")
                                        HighAlertApp.instance.session.save(token, phoneE164)
                                        AlertNetwork.api { token }.registerDevice(
                                            RegisterDeviceRequest(
                                                phone = phoneE164,
                                                deviceId = "ha-${System.currentTimeMillis()}"
                                            )
                                        )
                                        activePhone = phoneE164
                                        enableGrabbing()
                                        step = Step.Active
                                    } catch (e: Exception) {
                                        error = friendlyError(e)
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                            TextButton(onClick = {
                                step = Step.Phone
                                otp = ""
                                error = null
                            }) {
                                Text("Change number", color = Color(0xFF9CA3AF))
                            }
                        }

                        Step.Active -> {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF052E16), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0xFF166534), RoundedCornerShape(14.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text("Monitoring ON", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        activePhone.ifBlank { "This phone is ready" },
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton("Fix alert permissions") { enableGrabbing() }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = {
                                scope.launch {
                                    HighAlertApp.instance.session.clear()
                                    AlertMonitorService.stop(this@MainActivity)
                                    step = Step.Phone
                                    phoneDigits = ""
                                    otp = ""
                                }
                            }) {
                                Text("Sign out", color = Color(0xFF9CA3AF))
                            }
                        }
                    }
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

    private enum class Step { Phone, Otp, Active }
}

private fun toE164(digits: String): String {
    val d = digits.filter(Char::isDigit)
    return when {
        d.length == 10 -> "+91$d"
        d.startsWith("91") && d.length == 12 -> "+$d"
        else -> "+$d"
    }
}

private fun friendlyError(e: Exception): String {
    val msg = e.message.orEmpty()
    return when {
        msg.contains("Failed to connect", true) || msg.contains("Unable to resolve", true) ->
            "Cannot reach PUKAAR server. Check internet and try again."
        msg.contains("timeout", true) -> "Network timeout. Try again on Wi‑Fi or mobile data."
        msg.contains("401") || msg.contains("OTP", true) -> "Wrong or expired OTP. Request a new code."
        else -> msg.ifBlank { "Something went wrong. Try again." }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text.uppercase(),
        color = Color(0xFF9CA3AF),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SimpleBoxField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    prefix: String? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) {
            Text(prefix, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFF4B5563)))
            Spacer(Modifier.width(10.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(Color(0xFF22C55E)),
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = Color(0xFF6B7280), fontSize = 16.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFDC2626),
            disabledContainerColor = Color(0xFF7F1D1D)
        )
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun Err(msg: String) {
    Text(msg, color = Color(0xFFF87171), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp), lineHeight = 18.sp)
}
