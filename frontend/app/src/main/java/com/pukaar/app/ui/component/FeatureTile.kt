package com.pukaar.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.AccentAmber
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarRedDark
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The wide tile the menu uses for a feature that takes a whole row: the icon in
 * a filled circle on the left, a strapline under the name, and a chevron.
 *
 * By default it is as quiet as [MenuTile] — a plain rim on the same card. Pass
 * an [accent] to make one row ask to be noticed: a rim in that colour and a wash
 * behind it from the icon side. Only one tile should wear it at a time; the
 * emphasis is worth nothing if every row shouts.
 *
 * Pass [labelAccentFrom] to two-tone the label at that character, the way
 * [PukaarWordmark] splits PUKAAR — for a name like TRIPSHIELD that carries its
 * own wordmark. That is what italicises the name too: a wordmark is styled, an
 * ordinary feature name is just set.
 *
 * [comingSoon] is one idea, not two: the tile says so and stops opening. A mark
 * without the block would be a lie, and the block without the mark would read as
 * a bug. Everything but the mark itself fades, so the row still reads as part of
 * the menu rather than as something that failed to load.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeatureTile(
    icon: ImageVector,
    label: String,
    subtitle: String,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    labelAccentFrom: Int? = null,
    accent: Color? = null,
    comingSoon: Boolean = false
) {
    val isWordmark = labelAccentFrom != null
    val dim = if (comingSoon) 0.45f else 1f

    Card(
        onClick = onClick,
        enabled = !comingSoon,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceCard,
            // The card keeps its own surface when disabled; the fade is applied to
            // the content, so a greyed tile still sits on the same black as its
            // neighbours instead of turning a different shade of card.
            disabledContainerColor = SurfaceCard
        ),
        border = BorderStroke(1.dp, accent ?: Outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // A floor rather than a fixed height: it matches MenuTile so the
                // rows keep one rhythm, but the tile grows instead of clipping
                // its strapline when the badge wraps or the font scale is large.
                .heightIn(min = 98.dp)
                .then(
                    if (accent == null) {
                        Modifier
                    } else {
                        // A wash from the icon side, so the rim does not read as
                        // a flat outline pasted onto the same card as everything
                        // else.
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(accent.copy(alpha = 0.28f), Color.Transparent)
                            )
                        )
                    }
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .alpha(dim)
                    .size(44.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // A flow rather than a row: the badge sits beside the name where
                // there is room and drops beneath it on a narrow screen, instead
                // of being truncated to something unreadable.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = twoToneLabel(label, labelAccentFrom, accent ?: PukaarRed),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = if (isWordmark) FontStyle.Italic else FontStyle.Normal,
                        letterSpacing = if (isWordmark) 0.5.sp else 0.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .alpha(dim)
                    )
                    // The mark comes first: on a tile that cannot be opened it is
                    // the one thing the user needs before anything else on the row.
                    if (comingSoon) {
                        ComingSoonPill(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    if (badge != null) {
                        BadgePill(
                            text = badge,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .alpha(dim)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                // The strapline is part of the tile's pitch, not a caption under
                // it: near-white and readable, and allowed a second line rather
                // than being cut off mid-sentence on a narrow screen.
                Text(
                    text = subtitle,
                    color = TextPrimary.copy(alpha = 0.92f),
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(dim)
                )
            }

            // Dropped rather than dimmed: a chevron promises the row goes
            // somewhere, and this one does not.
            if (!comingSoon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** Splits [label] at [accentFrom] so the tail carries [accent]. */
private fun twoToneLabel(label: String, accentFrom: Int?, accent: Color) = buildAnnotatedString {
    val split = accentFrom?.coerceIn(0, label.length) ?: label.length
    withStyle(SpanStyle(color = TextPrimary)) { append(label.take(split)) }
    withStyle(SpanStyle(color = accent)) { append(label.drop(split)) }
}

/**
 * The amber mark on a tile that is listed but not ready.
 *
 * It keeps full strength while the rest of the tile fades — it is the answer to
 * "why won't this open?", and a greyed-out answer would not be one.
 */
@Composable
private fun ComingSoonPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(AccentAmber.copy(alpha = 0.16f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.menu_coming_soon),
            color = AccentAmber,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** The small rounded caption riding beside a feature's name. */
@Composable
private fun BadgePill(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(TextPrimary.copy(alpha = 0.10f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Public,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true, widthDp = 360)
@Composable
private fun FeatureTilePreview() {
    PukaarTheme {
        FeatureTile(
            icon = Icons.Filled.Flight,
            label = "TRIPSHIELD",
            subtitle = "Your Safety Companion in India",
            iconTint = TextPrimary,
            iconBackground = PukaarRedDark,
            badge = "For International Travellers",
            labelAccentFrom = 4,
            accent = PukaarRed,
            onClick = {}
        )
    }
}
