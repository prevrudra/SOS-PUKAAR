package com.pukaar.highalert.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.highalert.R
import com.pukaar.highalert.audio.AlertTonePlayer
import com.pukaar.highalert.data.AlertTone
import com.pukaar.highalert.data.TonePreferences
import com.pukaar.highalert.ui.components.BellIcon
import com.pukaar.highalert.ui.components.MessageBubbleIcon
import com.pukaar.highalert.ui.theme.InfoBlue
import com.pukaar.highalert.ui.theme.InfoBlueSoft
import com.pukaar.highalert.ui.theme.ExploreGreenSoft
import com.pukaar.highalert.ui.theme.LevelOne
import com.pukaar.highalert.ui.theme.LevelOneSoft
import com.pukaar.highalert.ui.theme.LevelThree
import com.pukaar.highalert.ui.theme.LevelThreeSoft
import com.pukaar.highalert.ui.theme.LevelTwo
import com.pukaar.highalert.ui.theme.LevelTwoSoft
import com.pukaar.highalert.ui.theme.OutlineGrey
import com.pukaar.highalert.ui.theme.PukaarAlertTheme
import com.pukaar.highalert.ui.theme.PukaarRed
import com.pukaar.highalert.ui.theme.PukaarRedSofter
import com.pukaar.highalert.ui.theme.SurfaceWhite
import com.pukaar.highalert.ui.theme.TextMuted
import com.pukaar.highalert.ui.theme.TextPrimary
import com.pukaar.highalert.ui.theme.TextSecondary

private data class ToneStyle(val accent: Color, val background: Color)

private fun styleFor(tone: AlertTone) = when (tone) {
    AlertTone.LEVEL_1 -> ToneStyle(LevelOne, LevelOneSoft)
    AlertTone.LEVEL_2 -> ToneStyle(LevelTwo, LevelTwoSoft)
    AlertTone.LEVEL_3 -> ToneStyle(LevelThree, LevelThreeSoft)
}

@Composable
fun HomeScreen(
    onOpenSampleAlert: () -> Unit,
    modifier: Modifier = Modifier,
    monitoringPhone: String? = null,
    onFixPermissions: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val preferences = remember { TonePreferences(context) }
    val player = remember { AlertTonePlayer(context) }
    var selectedTone by remember { mutableStateOf(preferences.selectedTone()) }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = modifier
            .background(SurfaceWhite)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BellIcon(modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(4.dp))

        Text(
            text = "PUKAAR",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "ALERT",
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PukaarRed,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Be There When It Matters",
            fontSize = 15.sp,
            color = TextSecondary
        )

        if (!monitoringPhone.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("PUKAAR High Alert is active", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(monitoringPhone, color = TextPrimary, fontSize = 14.sp)
                    Text(
                        "Checks for emergency alerts about every minute. Tap “Allow background alerts” so Oppo/Xiaomi do not block them.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TriggerExplainerCard()

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Choose Your Alert Tone",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Select one tone for notifications",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(12.dp))
        AlertTone.entries.forEach { tone ->
            ToneRow(
                tone = tone,
                isSelected = selectedTone == tone,
                isPlaying = player.playingTone == tone,
                onSelect = {
                    selectedTone = tone
                    preferences.setSelectedTone(tone)
                },
                onTogglePlay = {
                    player.toggle(tone)
                    if (selectedTone == null) {
                        selectedTone = tone
                        preferences.setSelectedTone(tone)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(2.dp))
        NavigationCard(
            background = InfoBlueSoft,
            title = "High Alert Tone with a Message",
            subtitle = "See a sample of what you will receive",
            onClick = onOpenSampleAlert,
            leading = {
                MessageBubbleIcon(
                    modifier = Modifier.size(40.dp),
                    color = InfoBlue
                )
            }
        )

        Spacer(Modifier.height(10.dp))
        NavigationCard(
            background = ExploreGreenSoft,
            title = "Explore Main PUKAAR",
            subtitle = "Personal safety, travel safety and more.",
            onClick = { DeviceActions.openMainPukaarApp(context) },
            leading = {
                Image(
                    painter = painterResource(R.drawable.ic_pukaar_main),
                    contentDescription = null,
                    modifier = Modifier.size(58.dp)
                )
            }
        )

        if (onFixPermissions != null) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PukaarRedSofter)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Background activity restricted?",
                    color = PukaarRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "On Oppo / Android 15+, open Battery → Unrestricted (or Allow background activity) for PUKAAR High Alert, or SOS rings will be delayed.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Button(
                    onClick = onFixPermissions,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PukaarRed)
                ) {
                    Text("Fix battery & alert permissions", color = SurfaceWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (onSignOut != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sign out",
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSignOut)
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = "PUKAAR ALERT",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Text(
            text = "A Safer World Together",
            fontSize = 13.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun TriggerExplainerCard() {
    val highlight = SpanStyle(color = PukaarRed, fontWeight = FontWeight.Bold)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PukaarRedSofter)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                append("If your trusted contact presses\n")
                withStyle(highlight) { append("SOS, Help,") }
                append(" or is ")
                withStyle(highlight) { append("Inactive,") }
                append("\nyou will receive a high alert tone with a message.")
            },
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToneRow(
    tone: AlertTone,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onTogglePlay: () -> Unit
) {
    val style = styleFor(tone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(style.background)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) style.accent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BellIcon(modifier = Modifier.size(44.dp), color = style.accent)
        Spacer(Modifier.width(10.dp))
        Text(
            text = tone.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onTogglePlay,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = style.accent,
                contentColor = if (tone == AlertTone.LEVEL_1) TextPrimary else SurfaceWhite
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 14.dp,
                vertical = 8.dp
            ),
            modifier = Modifier.height(42.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (isPlaying) "Stop" else "Play",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        SelectionDot(isSelected = isSelected, accent = style.accent, onClick = onSelect)
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .border(2.dp, if (isSelected) accent else OutlineGrey, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

@Composable
private fun NavigationCard(
    background: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun HomeScreenPreview() {
    PukaarAlertTheme {
        HomeScreen(onOpenSampleAlert = {})
    }
}
