package com.pukaar.app.ui.screen.howitworks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val InactivityGuidePages = listOf(
    // A title card, then the fourteen steps of the escalation, one at a time.
    ImagePage(R.drawable.inactivity_works_00_hero, R.string.inactivity_works_step0_description),
    ImagePage(R.drawable.inactivity_works_01_set_duration, R.string.inactivity_works_step1_description),
    ImagePage(R.drawable.inactivity_works_02_trusted_contacts, R.string.inactivity_works_step2_description),
    ImagePage(R.drawable.inactivity_works_03_pre_saved_contacts, R.string.inactivity_works_step3_description),
    ImagePage(R.drawable.inactivity_works_04_monitoring, R.string.inactivity_works_step4_description),
    ImagePage(R.drawable.inactivity_works_05_soft_notification, R.string.inactivity_works_step5_description),
    ImagePage(R.drawable.inactivity_works_06_soft_alert, R.string.inactivity_works_step6_description),
    ImagePage(R.drawable.inactivity_works_07_high_alert, R.string.inactivity_works_step7_description),
    ImagePage(R.drawable.inactivity_works_08_no_response, R.string.inactivity_works_step8_description),
    ImagePage(R.drawable.inactivity_works_09_second_third_contacts, R.string.inactivity_works_step9_description),
    ImagePage(R.drawable.inactivity_works_10_details_shared, R.string.inactivity_works_step10_description),
    ImagePage(R.drawable.inactivity_works_11_emergency_services, R.string.inactivity_works_step11_description),
    ImagePage(R.drawable.inactivity_works_12_call_flow, R.string.inactivity_works_step12_description),
    ImagePage(R.drawable.inactivity_works_13_contacts_receive, R.string.inactivity_works_step13_description),
    ImagePage(R.drawable.inactivity_works_14_you_also_receive_it, R.string.inactivity_works_step14_description)
)

/**
 * The inactivity branch of "How PUKAAR Works": how a long silence escalates from
 * a soft check-in to a high alert on the trusted contacts, one step per page.
 */
@Composable
fun InactivityGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = InactivityGuidePages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun InactivityGuideScreenPreview() {
    PukaarTheme {
        InactivityGuideScreen(onBack = {})
    }
}
