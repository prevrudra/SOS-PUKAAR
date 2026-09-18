package com.pukaar.highalert

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import com.pukaar.highalert.ui.HomeScreen
import com.pukaar.highalert.ui.theme.PukaarAlertTheme
import com.pukaar.highalert.ui.theme.PukaarRed
import com.pukaar.highalert.ui.theme.SurfaceWhite
import com.pukaar.highalert.ui.theme.TextMuted
import com.pukaar.highalert.ui.theme.TextPrimary
import com.pukaar.highalert.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

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
                runCatching {
                    withTimeout(5_000) {
                        val session = (application as HighAlertApp).session
                        if (!session.token().isNullOrBlank()) {
                            activePhone = session.phone().orEmpty()
                            step = Step.Active
                            ensureMonitoring()
                        }
                    }
                }.onFailure {
                    android.util.Log.e("HighAlert", "Startup session read failed: ${it.message}", it)
                }
            }

            PukaarAlertTheme {
                when (step) {
                    Step.Active -> {
                        HomeScreen(
                            onOpenSampleAlert = {
                                startActivity(Intent(this@MainActivity, SamplePreviewActivity::class.java))
                            },
                            monitoringPhone = activePhone.ifBlank { "Ready" },
                            onFixPermissions = { enableGrabbing() },
                            onSignOut = {
                                scope.launch {
                                    (application as HighAlertApp).session.clear()
                                    AlertReliabilityEngine.disarm(this@MainActivity)
                                    step = Step.Phone
                                    phoneDigits = ""
                                    otp = ""
                                    activePhone = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        )
                    }

                    else -> {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(SurfaceWhite)
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(24.dp)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text("PUKAAR", color = PukaarRed, fontSize = 36.sp, fontWeight = FontWeight.Black)
                                Text("ALERT", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    when (step) {
                                        Step.Phone -> "Sign in with your phone to receive loud SOS alerts from people who trust you."
                                        Step.Otp -> "Enter the 6-digit code we sent by SMS."
                                        Step.Active -> ""
                                    },
                                    color = TextSecondary,
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
                                                    val resp = AlertNetwork.api { null }
                                                        .verifyOtp(OtpVerifyRequest(phoneE164, otp))
                                                    val token = resp.accessToken ?: error("Could not sign in")
                                                    (application as HighAlertApp).session.save(token, phoneE164)
                                                    activePhone = phoneE164
                                                    step = Step.Active
                                                    ensureMonitoring()
                                                    // FCM can hang on some OEMs — register in background after UI advances.
                                                    scope.launch {
                                                        FcmRegistrar.registerAfterLogin(this@MainActivity)
                                                    }
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
                                            Text("Change number", color = TextMuted)
                                        }
                                    }

                                    Step.Active -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensureMonitoring() {
        runCatching {
            AlertReliabilityEngine.armAll(this, allowForegroundService = true)
        }.onFailure {
            android.util.Log.e("HighAlert", "ensureMonitoring failed: ${it.message}", it)
        }
        // Only auto-ask notification permission — battery/fullscreen stay on "Fix permissions"
        // so login feels like a normal alarm app, not a permission gauntlet.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            runCatching { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    /**
     * Critical for Oppo/Xiaomi/Vivo — without unrestricted battery, 60s alarms
     * are deferred and High Alert never wakes for SOS.
     */
    private fun enableGrabbing() {
        ensureMonitoring()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                return
            }
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm != null && !nm.canUseFullScreenIntent()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                return
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(android.app.AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                return
            }
        }
        runCatching {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val token = (application as HighAlertApp).session.token()
                if (!token.isNullOrBlank()) {
                    ensureMonitoring()
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
        color = TextMuted,
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
            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) {
            Text(prefix, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFFD1D5DB)))
            Spacer(Modifier.width(10.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(PukaarRed),
            textStyle = TextStyle(color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = TextMuted, fontSize = 16.sp)
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
            containerColor = PukaarRed,
            disabledContainerColor = Color(0xFFFECACA)
        )
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun Err(msg: String) {
    Text(msg, color = Color(0xFFDC2626), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp), lineHeight = 18.sp)
}
