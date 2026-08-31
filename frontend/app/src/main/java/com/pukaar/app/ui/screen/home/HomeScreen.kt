package com.pukaar.app.ui.screen.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarShield
import com.pukaar.app.ui.component.PukaarWordmark
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Screen 1, in either of its two modes.
 *
 * The layout is identical for both; [mode] supplies the colour and the wording,
 * so switching modes never moves anything on screen.
 */
@Composable
fun HomeScreen(
    mode: HomeMode,
    onModeChange: (HomeMode) -> Unit,
    onPrimaryAction: (HomeMode) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header()

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.home_mode_label).uppercase(),
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        ModeToggle(mode = mode, onModeChange = onModeChange)

        Spacer(modifier = Modifier.weight(1f))

        ModeButton(mode = mode, onClick = { onPrimaryAction(mode) })

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = stringResource(mode.headlineRes).uppercase(),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center
        )

        if (mode.descriptionRes != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(mode.descriptionRes),
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        MenuBar(onClick = onMenuClick)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            PukaarWordmark()
            PukaarShield(modifier = Modifier.align(Alignment.CenterEnd))
        }
        Text(
            text = stringResource(R.string.app_tagline),
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * The one control that matters, ringed so it reads as a physical button. Its
 * colour and label come from the active [mode].
 */
@Composable
private fun ModeButton(
    mode: HomeMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent by animateColorAsState(mode.accent, label = "buttonAccent")
    val accentDark by animateColorAsState(mode.accentDark, label = "buttonAccentDark")

    Box(
        modifier = modifier
            .size(268.dp)
            .background(SurfaceElevated, CircleShape)
            .padding(11.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(listOf(accent, accentDark)),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(mode.buttonLabelRes).uppercase(),
                color = TextPrimary,
                fontSize = 62.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/** The pill at the bottom that opens Screen 2. */
@Composable
private fun MenuBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(30.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, Outline),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Apps,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.menu).uppercase(),
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}

@Preview(name = "SOS mode", showBackground = true, backgroundColor = 0xFF000000, heightDp = 780)
@Composable
private fun HomeScreenSosPreview() {
    PukaarTheme {
        HomeScreen(
            mode = HomeMode.SOS,
            onModeChange = {},
            onPrimaryAction = {},
            onMenuClick = {}
        )
    }
}

@Preview(name = "Help mode", showBackground = true, backgroundColor = 0xFF000000, heightDp = 780)
@Composable
private fun HomeScreenHelpPreview() {
    PukaarTheme {
        HomeScreen(
            mode = HomeMode.HELP,
            onModeChange = {},
            onPrimaryAction = {},
            onMenuClick = {}
        )
    }
}
