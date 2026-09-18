package com.pukaar.app.ui.screen.howitworks

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.screen.protection.ChooserCard
import com.pukaar.app.ui.screen.protection.ProtectionChooserScreen
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.AccentGrey
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SafeArrivalGreen
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The menu's "How PUKAAR Works" tile now opens a choice rather than a single
 * walkthrough: each feature is explained on its own terms, because what happens
 * after an SOS has little to do with what happens after a quiet day, and neither
 * has much to do with a card somebody finds in a wallet.
 *
 * The Emergency Card sits under the two protections rather than among them: it
 * is not an alert the user sets off, it is a document that speaks once they
 * cannot. It is blue here rather than the purple it wears on the menu, because
 * beside Inactivity's purple two purple cards would read as one pair.
 *
 * Safe Arrival follows, and the list closes on the one card that is not a
 * feature. The walkthroughs answer *what* each feature does; [onWhenToUseClick]
 * answers the question people actually hesitate over — whether this is a moment
 * to use any of it.
 */
@Composable
fun HowItWorksHubScreen(
    onTypeSelected: (ProtectionType) -> Unit,
    onEmergencyCardClick: () -> Unit,
    onWhenToUseClick: () -> Unit,
    onSafeArrivalClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtectionChooserScreen(
        title = stringResource(R.string.menu_how_pukaar_works),
        onTypeSelected = onTypeSelected,
        onBack = onBack,
        modifier = modifier
    ) {
        ChooserCard(
            icon = Icons.Filled.ContactEmergency,
            title = stringResource(R.string.protection_emergency_card),
            tagline = stringResource(R.string.protection_emergency_card_tagline),
            accent = AccentBlue,
            onClick = onEmergencyCardClick
        )
        ChooserCard(
            icon = Icons.Filled.LocationOn,
            title = stringResource(R.string.safe_arrival_title),
            tagline = stringResource(R.string.safe_arrival_tagline),
            accent = SafeArrivalGreen,
            onClick = onSafeArrivalClick,
            titleColor = TextPrimary,
            containerColor = SafeArrivalGreen.copy(alpha = 0.08f).compositeOver(SurfaceCard),
            taglineColor = TextSecondary
        )
        ChooserCard(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = stringResource(R.string.when_to_use_title),
            tagline = stringResource(R.string.when_to_use_tagline),
            accent = AccentGrey,
            onClick = onWhenToUseClick,
            // Not a feature, so it does not shout in a feature's colour.
            titleColor = TextPrimary,
            titleSize = 19.sp,
            borderColor = Outline
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun HowItWorksHubScreenPreview() {
    PukaarTheme {
        HowItWorksHubScreen(
            onTypeSelected = {},
            onEmergencyCardClick = {},
            onWhenToUseClick = {},
            onSafeArrivalClick = {},
            onBack = {}
        )
    }
}
