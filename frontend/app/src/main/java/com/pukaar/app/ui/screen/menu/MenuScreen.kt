package com.pukaar.app.ui.screen.menu

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.FeatureTile
import com.pukaar.app.ui.component.MenuTile
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Screen 2. One ordered run of tiles, in the order [MenuItem.stepRows] declares —
 * the sequence itself does the grouping, so there are no section headings.
 */
@Composable
fun MenuScreen(
    onItemClick: (MenuItem) -> Unit,
    onSettingsClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        MenuHeader(onSettingsClick = onSettingsClick)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TileRows(rows = MenuItem.stepRows, onItemClick = onItemClick)

            Spacer(modifier = Modifier.height(20.dp))
            MenuTagline()
            Spacer(modifier = Modifier.height(20.dp))
        }

        CloseButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MenuHeader(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.menu).uppercase(),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.menu_settings),
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Draws [rows] as given. Built from Rows rather than a lazy grid because the item
 * count is known and the whole menu scrolls as one piece.
 *
 * Tiles share their row evenly, so a row of one spans the full width and a row of
 * three splits into thirds.
 */
@Composable
private fun TileRows(
    rows: List<List<MenuItem>>,
    onItemClick: (MenuItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowItems ->
            // Now that the squares carry a strapline they no longer come out the
            // same height on their own: a shared row takes the taller tile's
            // height and both fill it, so the grid stays a grid.
            //
            // A row of one is left alone. There is nothing to equalise, and the
            // intrinsic height is measured as though the name and its badge sit on
            // one line — which pins a wide tile shorter than it needs and clips the
            // strapline the moment the badge wraps.
            val equalise = rowItems.size > 1

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = if (equalise) Modifier.height(IntrinsicSize.Min) else Modifier
            ) {
                rowItems.forEach { item ->
                    val tile = Modifier
                        .weight(1f)
                        .then(if (equalise) Modifier.fillMaxHeight() else Modifier)

                    if (item.isFeature) {
                        FeatureTile(
                            icon = item.icon,
                            label = stringResource(item.labelRes),
                            subtitle = stringResource(item.subtitleRes),
                            iconTint = item.tint,
                            iconBackground = item.iconBackground ?: item.tint,
                            badge = item.badgeRes?.let { stringResource(it) },
                            labelAccentFrom = item.labelAccentFrom,
                            accent = item.accent,
                            comingSoon = item.comingSoon,
                            onClick = { onItemClick(item) },
                            modifier = tile
                        )
                    } else {
                        MenuTile(
                            icon = item.icon,
                            label = stringResource(item.labelRes),
                            iconTint = item.tint,
                            iconBackground = item.iconBackground,
                            subtitle = stringResource(item.subtitleRes),
                            // Only a feature tile draws the mark, so a square one
                            // would be silently dead. The guard stops that ever
                            // shipping unnoticed; MenuItemTest holds the rule.
                            onClick = { if (!item.comingSoon) onItemClick(item) },
                            modifier = tile
                        )
                    }
                }
            }
        }
    }
}

/**
 * The sign-off under the last tile: a white line and a red call to action,
 * held between two short red rules. It scrolls with the tiles rather than
 * sitting over them, so it never costs a small screen any list space.
 */
@Composable
private fun MenuTagline() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TaglineRule(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.menu_tagline_line1),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.menu_tagline_line2),
                color = PukaarRed,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        TaglineRule(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TaglineRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(PukaarRed)
    )
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .background(SurfaceElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1000)
@Composable
private fun MenuScreenPreview() {
    PukaarTheme {
        MenuScreen(onItemClick = {}, onSettingsClick = {}, onClose = {})
    }
}
