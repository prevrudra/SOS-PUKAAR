package com.pukaar.app.ui.screen.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.pukaar.app.ui.theme.PukaarRedBright
import com.pukaar.app.ui.theme.PukaarRedDark
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
    val context = LocalContext.current
    val scroll = rememberScrollState()

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        audio = granted
    }

    PremiumBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(top = 20.dp, bottom = 16.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PukaarRedBright, PukaarRedDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Safety setup",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose how PUKAAR protects you during SOS and HELP.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                )

                PremiumCard {
                    ConsentRow(
                        icon = Icons.Default.Shield,
                        title = "Terms & Privacy",
                        subtitle = "Required to use PUKAAR",
                        checked = terms,
                        onChecked = { terms = it }
                    )
                    ConsentDivider()
                    ConsentRow(
                        icon = Icons.Default.LocationOn,
                        title = "Live location",
                        subtitle = "Share location with contacts during emergencies",
                        checked = location,
                        onChecked = { location = it }
                    )
                    ConsentDivider()
                    ConsentRow(
                        icon = Icons.Default.Mic,
                        title = "Background audio",
                        subtitle = "Record evidence audio during SOS (optional)",
                        checked = audio,
                        onChecked = {
                            if (it && ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                audio = it
                            }
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Emergency alerts go to your trusted contacts via your phone’s SMS. After setup, enable Volume SOS in Accessibility so triple Volume Up opens PUKAAR even when the app is closed.",
                    color = TextTertiary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                error?.let {
                    Text(
                        text = it,
                        color = PukaarRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

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
                text = "You can change these later in Settings",
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun ConsentRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (checked) SuccessGreen.copy(alpha = 0.18f)
                    else Color.White.copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) SuccessGreen else TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SuccessGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = TextTertiary.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun ConsentDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
}
