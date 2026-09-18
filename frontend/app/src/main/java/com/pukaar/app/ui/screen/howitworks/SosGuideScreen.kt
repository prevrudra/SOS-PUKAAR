package com.pukaar.app.ui.screen.howitworks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val SosGuidePages = listOf(
    // The thirteen steps of an SOS, one at a time.
    ImagePage(R.drawable.how_it_works_01_trigger_sos, R.string.how_this_works_step1_description),
    ImagePage(R.drawable.how_it_works_02_sos_triggered, R.string.how_this_works_step2_description),
    ImagePage(R.drawable.how_it_works_03_alert_to_contacts, R.string.how_this_works_step3_description),
    ImagePage(R.drawable.how_it_works_04_grabbing_alert, R.string.how_this_works_step4_description),
    ImagePage(R.drawable.how_it_works_05_recording_starts, R.string.how_this_works_step5_description),
    ImagePage(R.drawable.how_it_works_06_saved_on_cloud, R.string.how_this_works_step6_description),
    ImagePage(R.drawable.how_it_works_08_message_details, R.string.how_this_works_step7_description),
    ImagePage(R.drawable.how_it_works_09_emergency_services, R.string.how_this_works_step8_description),
    ImagePage(R.drawable.how_it_works_10_contacts_connected, R.string.how_this_works_step9_description),
    ImagePage(R.drawable.how_it_works_07_pre_saved_contacts, R.string.how_this_works_step10_description),
    ImagePage(R.drawable.how_it_works_11_you_also_receive_it, R.string.how_this_works_step11_description),
    ImagePage(R.drawable.how_it_works_12_everything_ready, R.string.how_this_works_step12_description),
    ImagePage(R.drawable.how_it_works_13_help_reaches_faster, R.string.how_this_works_step13_description)
)

/**
 * The SOS branch of "How PUKAAR Works": what happens when an SOS is triggered,
 * one step per page.
 */
@Composable
fun SosGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = SosGuidePages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun SosGuideScreenPreview() {
    PukaarTheme {
        SosGuideScreen(onBack = {})
    }
}
