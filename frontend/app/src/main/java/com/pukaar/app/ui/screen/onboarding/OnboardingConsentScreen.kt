package com.pukaar.app.ui.screen.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ProfileUpdateRequest
import com.pukaar.app.ui.component.PremiumBackground
import com.pukaar.app.ui.component.PremiumCard
import com.pukaar.app.ui.component.PremiumPrimaryButton
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

@Composable
fun OnboardingConsentScreen(onComplete: () -> Unit) {
    var terms by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audio = granted
    }

    PremiumBackground {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text("Safety setup", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                "Configure how PUKAAR protects you during SOS and HELP.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(20.dp))

            PremiumCard {
                ConsentRow(Icons.Default.Shield, "I accept Terms & Privacy", terms) { terms = it }
                ConsentRow(Icons.Default.LocationOn, "Share live location during emergencies", location) { location = it }
                ConsentRow(Icons.Default.Mic, "Record background audio during SOS", audio) {
                    if (it && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        audio = it
                    }
                }
            }

            Text(
                "Emergency alerts are sent from your phone via SMS to trusted contacts. OTP login uses secure SMS only.",
                color = TextTertiary, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(top = 14.dp)
            )

            error?.let { Text(it, color = PukaarRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.weight(1f))

            PremiumPrimaryButton(
                text = "Activate protection",
                enabled = terms && location,
                loading = loading,
                onClick = {
                    scope.launch {
                        error = null
                        loading = true
                        try {
                            PukaarApp.instance.repository.updateProfile(
                                ProfileUpdateRequest(
                                    consentLocation = location,
                                    consentAudio = audio,
                                    consentTerms = terms
                                )
                            )
                            PukaarApp.instance.repository.completeOnboarding()
                            onComplete()
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            loading = false
                        }
                    }
                }
            )
            Text(
                "Premium emergency readiness",
                color = SuccessGreen.copy(alpha = 0.75f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ConsentRow(icon: ImageVector, label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = if (checked) SuccessGreen else TextTertiary)
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, color = Color.White, fontSize = 14.sp, lineHeight = 19.sp)
    }
}
