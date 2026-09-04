package com.pukaar.app.ui.screen.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private const val COUNTDOWN_SECONDS = 5

/**
 * Full-screen overlay shown after SOS/HELP is pressed.
 * Counts down from 5; user can cancel before the alert is sent.
 */
@Composable
fun SosCountdownOverlay(
    mode: HomeMode,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var secondsLeft by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }
    var cancelled by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        cancelled = false
        secondsLeft = COUNTDOWN_SECONDS
        while (secondsLeft > 0 && !cancelled) {
            delay(1000)
            if (!cancelled) secondsLeft--
        }
        if (!cancelled) onComplete()
    }

    val scale by animateFloatAsState(
        targetValue = if (secondsLeft > 0) 1f else 0.85f,
        animationSpec = tween(300),
        label = "countdownScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.96f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(
                    if (mode == HomeMode.SOS) R.string.countdown_sos_title else R.string.countdown_help_title
                ).uppercase(),
                color = mode.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size((220 * scale).dp)
                    .background(SurfaceElevated, CircleShape)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(mode.accent, mode.accentDark))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = secondsLeft.coerceAtLeast(0).toString(),
                        color = TextPrimary,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.countdown_message),
                color = TextSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    cancelled = true
                    onCancel()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.countdown_cancel).uppercase(),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
