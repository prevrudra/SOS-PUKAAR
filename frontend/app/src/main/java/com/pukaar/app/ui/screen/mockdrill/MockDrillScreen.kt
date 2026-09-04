package com.pukaar.app.ui.screen.mockdrill

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.theme.PukaarTheme

/** The SOS drill: the emergency the user hopes never to have. */
private val SosDrillPages = listOf(
    ImagePage(R.drawable.mock_drill_choose, R.string.mock_drill_page_choose_description),

    // The drill itself: the briefing, its eleven steps, the finish and the sign-off.
    ImagePage(R.drawable.mock_drill_00_start_drill, R.string.mock_drill_page_start_description),
    ImagePage(R.drawable.mock_drill_01_add_trusted_contacts, R.string.mock_drill_page_step1_description),
    ImagePage(R.drawable.mock_drill_02_add_contact, R.string.mock_drill_page_step2_description),
    ImagePage(R.drawable.mock_drill_03_in_case_of_emergency, R.string.mock_drill_page_step3_description),
    ImagePage(R.drawable.mock_drill_04_countdown, R.string.mock_drill_page_step4_description),
    ImagePage(R.drawable.mock_drill_05_audio_recording, R.string.mock_drill_page_step5_description),
    ImagePage(R.drawable.mock_drill_06_high_alert_sent, R.string.mock_drill_page_step6_description),
    ImagePage(R.drawable.mock_drill_07_contacts_received, R.string.mock_drill_page_step7_description),
    ImagePage(R.drawable.mock_drill_08_live_location, R.string.mock_drill_page_step8_description),
    ImagePage(R.drawable.mock_drill_09_battery_network, R.string.mock_drill_page_step9_description),
    ImagePage(R.drawable.mock_drill_10_other_contacts_112, R.string.mock_drill_page_step10_description),
    ImagePage(R.drawable.mock_drill_11_nearby_services, R.string.mock_drill_page_step11_description),
    ImagePage(R.drawable.mock_drill_12_drill_complete, R.string.mock_drill_page_complete_description),
    ImagePage(R.drawable.mock_drill_13_outro, R.string.mock_drill_page_outro_description),

    ImagePage(R.drawable.mock_drill_completed, R.string.mock_drill_page_completed_description),
    ImagePage(R.drawable.mock_drill_referral_offer, R.string.mock_drill_page_referral_description)
)

/** The HELP drill: family and trusted contacts, primary first then secondary. */
private val HelpDrillPages = listOf(
    ImagePage(R.drawable.help_drill_00_help_mock_drill, R.string.help_drill_page_start_description),
    ImagePage(R.drawable.help_drill_01_select_help_contacts, R.string.help_drill_page_step1_description),
    ImagePage(R.drawable.help_drill_02_primary_secondary, R.string.help_drill_page_step2_description),
    ImagePage(R.drawable.help_drill_03_presaved_numbers, R.string.help_drill_page_step3_description),
    ImagePage(R.drawable.help_drill_04_dont_call_elderly, R.string.help_drill_page_step4_description),
    ImagePage(R.drawable.help_drill_05_trigger_help, R.string.help_drill_page_step5_description),
    ImagePage(R.drawable.help_drill_06_countdown, R.string.help_drill_page_step6_description),
    ImagePage(R.drawable.help_drill_07_alert_sent_primary, R.string.help_drill_page_step7_description),
    ImagePage(R.drawable.help_drill_08_information_shared, R.string.help_drill_page_step8_description),
    ImagePage(R.drawable.help_drill_09_no_response, R.string.help_drill_page_step9_description),
    ImagePage(R.drawable.help_drill_10_contacts_received, R.string.help_drill_page_step10_description),
    ImagePage(R.drawable.help_drill_11_they_take_action, R.string.help_drill_page_step11_description),
    ImagePage(R.drawable.help_drill_12_drill_complete, R.string.help_drill_page_step12_description)
)

/**
 * Menu item 3. The drill walkthrough, one screen mock-up per page.
 *
 * Which drill depends on [mode]: rehearsing an SOS and rehearsing a HELP are
 * different flows, so the home toggle decides which one this screen shows.
 */
@Composable
fun MockDrillScreen(
    mode: HomeMode,
    onBack: () -> Unit,
    onStartLiveDrill: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pages = when (mode) {
        HomeMode.SOS -> SosDrillPages
        HomeMode.HELP -> HelpDrillPages
    }
    Box(modifier = modifier.fillMaxSize()) {
        ImagePagerScreen(pages = pages, onBack = onBack, modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 56.dp)
        ) {
            Text(
                text = stringResource(R.string.mock_drill_start_hint),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            PrimaryButton(
                text = stringResource(R.string.mock_drill_start_live),
                onClick = onStartLiveDrill
            )
        }
    }
}

@Preview(name = "SOS drill", showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun MockDrillSosPreview() {
    PukaarTheme {
        MockDrillScreen(mode = HomeMode.SOS, onBack = {})
    }
}

@Preview(name = "HELP drill", showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun MockDrillHelpPreview() {
    PukaarTheme {
        MockDrillScreen(mode = HomeMode.HELP, onBack = {})
    }
}
