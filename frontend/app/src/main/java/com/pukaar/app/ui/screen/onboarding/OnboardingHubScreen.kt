package com.pukaar.app.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.screen.protection.ProtectionChooserScreen
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.theme.PukaarTheme

/**
 * Where onboarding starts: the user picks which of the three protections to set
 * up, and the choice decides both the flow and the colour it runs in.
 *
 * Nothing is set up here — this screen only routes into
 * [ProtectionType.setupRoute].
 */
@Composable
fun OnboardingHubScreen(
    onTypeSelected: (ProtectionType) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtectionChooserScreen(
        title = stringResource(R.string.onboarding_hub_title),
        subtitle = stringResource(R.string.onboarding_hub_subtitle),
        onTypeSelected = onTypeSelected,
        onBack = onBack,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun OnboardingHubScreenPreview() {
    PukaarTheme {
        OnboardingHubScreen(onTypeSelected = {}, onBack = {})
    }
}
