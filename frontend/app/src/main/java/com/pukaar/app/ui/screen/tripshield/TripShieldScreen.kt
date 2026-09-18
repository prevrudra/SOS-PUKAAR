package com.pukaar.app.ui.screen.tripshield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.ListTile
import com.pukaar.app.ui.component.PukaarShield
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary

/**
 * TripShield's own menu, reached from the TripShield tile on the main menu.
 *
 * Nothing behind these rows is built yet, so [onItemClick] defaults to doing
 * nothing — a row presses and ripples but stays put. Give it a real handler once
 * the destinations exist, and add them to the nav graph then.
 */
@Composable
fun TripShieldScreen(
    onBack: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (TripShieldMenuItem) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        TopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TripShieldHeader()
            Spacer(modifier = Modifier.height(18.dp))
            TileRows(onItemClick = onItemClick)
            Spacer(modifier = Modifier.height(20.dp))
        }

        BackToHomeButton(
            onClick = onBackToHome,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** The back arrow and the shield, sitting either side of the masthead below. */
@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        PukaarShield(
            size = 34,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

/**
 * Draws [TripShieldMenuItem.rows] as declared. Rows of two are measured to the
 * taller card so the pair stays level even when one subtitle wraps further.
 */
@Composable
private fun TileRows(onItemClick: (TripShieldMenuItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TripShieldMenuItem.rows.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(IntrinsicSize.Min)
            ) {
                rowItems.forEach { item ->
                    ListTile(
                        icon = item.icon,
                        iconBackground = item.iconBackground,
                        title = stringResource(item.titleRes),
                        subtitle = stringResource(item.subtitleRes),
                        onClick = { onItemClick(item) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun BackToHomeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceElevated,
            contentColor = TextPrimary
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 28.dp,
            vertical = 14.dp
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.tripshield_back_to_home),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun TripShieldScreenPreview() {
    PukaarTheme {
        TripShieldScreen(onBack = {}, onBackToHome = {})
    }
}
