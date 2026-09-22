package com.pukaar.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Shown right after the SOS button is tapped.
 * User picks a mode; countdown starts only after a choice.
 */
@Composable
fun SosModeSelectOverlay(
    onSelect: (HomeMode) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .padding(horizontal = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black)) {
                        append("PUK")
                    }
                    withStyle(SpanStyle(color = PukaarRed, fontWeight = FontWeight.Black)) {
                        append("AAR")
                    }
                },
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.sos_select_title),
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sos_select_subtitle),
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            ModeSelectCard(
                title = stringResource(R.string.sos_select_sos_title),
                buttonLabel = stringResource(R.string.sos_select_send),
                accent = PukaarRed,
                icon = Icons.Filled.PhoneDisabled,
                onClick = { onSelect(HomeMode.SOS) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModeSelectCard(
                title = stringResource(R.string.sos_select_help_title),
                buttonLabel = stringResource(R.string.sos_select_send),
                accent = PukaarOrange,
                icon = Icons.Filled.RingVolume,
                onClick = { onSelect(HomeMode.HELP) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.countdown_cancel),
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ModeSelectCard(
    title: String,
    buttonLabel: String,
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = accent.copy(alpha = 0.35f), spotColor = accent.copy(alpha = 0.45f))
            .clip(shape)
            .background(SurfaceCard)
            .border(1.5.dp, accent.copy(alpha = 0.75f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(accent.copy(alpha = 0.22f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Text(
            text = title.uppercase(),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            modifier = Modifier.padding(vertical = 14.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonLabel,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
