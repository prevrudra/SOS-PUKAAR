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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Shown right after the SOS button is tapped.
 * User picks SOS or HELP; countdown starts only after a choice.
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
            .background(Black.copy(alpha = 0.96f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.sos_select_title).uppercase(),
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.0.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.sos_select_subtitle),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))

            ModePickCard(
                mode = HomeMode.SOS,
                title = stringResource(R.string.sos_select_sos_title),
                description = stringResource(R.string.sos_select_sos_desc),
                onClick = { onSelect(HomeMode.SOS) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ModePickCard(
                mode = HomeMode.HELP,
                title = stringResource(R.string.sos_select_help_title),
                description = stringResource(R.string.sos_select_help_desc),
                onClick = { onSelect(HomeMode.HELP) }
            )

            Spacer(modifier = Modifier.height(36.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
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

@Composable
private fun ModePickCard(
    mode: HomeMode,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(listOf(mode.accent, mode.accentDark))
            )
            .border(1.dp, Outline.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
        )
        Text(
            text = description,
            color = TextPrimary.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
