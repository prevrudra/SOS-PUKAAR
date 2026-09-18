package com.pukaar.app.ui.screen.protection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.CircularBackButton
import com.pukaar.app.ui.component.PukaarShield
import com.pukaar.app.ui.component.PukaarWordmark
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

/**
 * The wordmark, a title, the three protections as cards, and the shield.
 *
 * Both hubs are this screen: onboarding passes [ProtectionType.setupRoute]
 * through [onTypeSelected], the guide hub passes [ProtectionType.guideRoute].
 * Keeping one implementation means the two can never drift apart visually.
 *
 * [subtitle] is the explanatory paragraph the onboarding hub carries; without
 * one the title gets the short red rule instead, as the guide hub does.
 *
 * [extraCards] is for anything that belongs under the three but is not a
 * protection — the guide hub's "When to use PUKAAR?" card — so the onboarding
 * hub is unaffected by it.
 */
@Composable
fun ProtectionChooserScreen(
    title: String,
    onTypeSelected: (ProtectionType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    extraCards: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(top = 10.dp)) {
                CircularBackButton(onClick = onBack)
            }
            PukaarWordmark(
                fontSize = 34,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(26.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (subtitle == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(2.dp)
                        .background(PukaarRed, RoundedCornerShape(1.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProtectionType.entries.forEach { type ->
                    ChooserCard(
                        icon = type.icon,
                        title = stringResource(type.labelRes),
                        tagline = stringResource(type.taglineRes),
                        accent = type.accent,
                        onClick = { onTypeSelected(type) }
                    )
                }
                extraCards()
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PukaarShield(size = 30)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_tagline_caps).uppercase(),
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
        }
    }
}

/**
 * One choice: a filled disc in the choice's colour, its name and what it does,
 * and a chevron. The whole card is the target.
 *
 * The three protections take the defaults, where [accent] carries the name, the
 * disc and the border alike. The guide hub's fourth card is not a protection and
 * says so by overriding [titleColor], [titleSize] and [borderColor] — a white
 * question in a plain grey frame rather than a coloured shout. Safe Arrival
 * keeps a white name too, but sits in a wash of its green via [containerColor].
 */
@Composable
fun ChooserCard(
    icon: ImageVector,
    title: String,
    tagline: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = accent,
    titleSize: TextUnit = 22.sp,
    borderColor: Color = accent.copy(alpha = 0.7f),
    containerColor: Color = SurfaceCard,
    taglineColor: Color = TextPrimary
) {
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = titleSize,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tagline,
                color = taglineColor,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun ProtectionChooserScreenPreview() {
    PukaarTheme {
        ProtectionChooserScreen(
            title = "How PUKAAR Works",
            onTypeSelected = {},
            onBack = {}
        )
    }
}
