package com.pukaar.app.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

private val ToggleHeight = 52.dp
private val KnobSize = 38.dp

/**
 * Two-position switch between [HomeMode.SOS] and [HomeMode.HELP].
 *
 * The filled half takes the colour of whichever mode is active. The white knob
 * stays on the midpoint and does not travel — it marks the boundary, and the
 * colour is what tells you which side is live.
 */
@Composable
fun ModeToggle(
    mode: HomeMode,
    onModeChange: (HomeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ToggleHeight)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .border(1.dp, Outline, CircleShape)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            HomeMode.entries.forEach { entry ->
                ModeSegment(
                    mode = entry,
                    active = entry == mode,
                    onClick = { onModeChange(entry) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(KnobSize)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun ModeSegment(
    mode: HomeMode,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (active) mode.accent else Color.Transparent,
        label = "segmentBackground"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(background)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(mode.toggleLabelRes).uppercase(),
            color = if (active) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
    }
}
