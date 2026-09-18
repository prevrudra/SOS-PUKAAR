package com.pukaar.app.ui.screen.mockdrill

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

/** The SOS drill: the emergency the user hopes never to have. */
private val SosDrillPages = listOf(
    // The drill itself: the briefing, its twelve steps, the finish and the sign-off.
    ImagePage(R.drawable.mock_drill_00_start_drill, R.string.mock_drill_page_start_description),
    ImagePage(R.drawable.mock_drill_01_add_trusted_contacts, R.string.mock_drill_page_step1_description),
    ImagePage(R.drawable.mock_drill_02_add_contact, R.string.mock_drill_page_step2_description),
    // Not one of the twelve numbered steps, so it takes a letter rather than
    // renumbering the images, which carry their step number on screen.
    ImagePage(R.drawable.mock_drill_02a_share_pukaar_alert, R.string.mock_drill_page_share_alert_description),
    ImagePage(R.drawable.mock_drill_03_presaved_numbers, R.string.mock_drill_page_step3_description),
    ImagePage(R.drawable.mock_drill_04_in_case_of_emergency, R.string.mock_drill_page_step4_description),
    ImagePage(R.drawable.mock_drill_05_countdown, R.string.mock_drill_page_step5_description),
    ImagePage(R.drawable.mock_drill_06_audio_recording, R.string.mock_drill_page_step6_description),
    ImagePage(R.drawable.mock_drill_07_high_alert_sent, R.string.mock_drill_page_step7_description),
    ImagePage(R.drawable.mock_drill_08_contacts_received, R.string.mock_drill_page_step8_description),
    ImagePage(R.drawable.mock_drill_09_live_location, R.string.mock_drill_page_step9_description),
    ImagePage(R.drawable.mock_drill_10_battery_network, R.string.mock_drill_page_step10_description),
    ImagePage(R.drawable.mock_drill_11_other_contacts_112, R.string.mock_drill_page_step11_description),
    ImagePage(R.drawable.mock_drill_12_nearby_services, R.string.mock_drill_page_step12_description),
    ImagePage(R.drawable.mock_drill_13_drill_complete, R.string.mock_drill_page_complete_description),
    ImagePage(R.drawable.mock_drill_14_outro, R.string.mock_drill_page_outro_description),

    ImagePage(R.drawable.mock_drill_completed, R.string.mock_drill_page_completed_description),
    ImagePage(R.drawable.mock_drill_referral_offer, R.string.mock_drill_page_referral_description)
)

/** The SOS drill, reached from [MockDrillChooserScreen]. */
@Composable
fun SosDrillScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = SosDrillPages, onBack = onBack, modifier = modifier)
}

@Preview(name = "SOS drill", showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun SosDrillPreview() {
    PukaarTheme {
        SosDrillScreen(onBack = {})
    }
}

