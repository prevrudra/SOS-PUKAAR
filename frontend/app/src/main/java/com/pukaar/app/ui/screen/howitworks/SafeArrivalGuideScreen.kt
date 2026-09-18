package com.pukaar.app.ui.screen.howitworks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val SafeArrivalGuidePages = listOf(
    // The pitch, then setting it up — when, who, confirm — then the check at the
    // arrival time, the extension and grace period, and the two ways it ends:
    // arriving on time, or no response.
    ImagePage(
        R.drawable.safe_arrival_works_01_intro,
        R.string.safe_arrival_works_intro_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_02_set_arrival_time,
        R.string.safe_arrival_works_set_time_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_03_choose_contacts,
        R.string.safe_arrival_works_choose_contacts_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_04_review_and_start,
        R.string.safe_arrival_works_review_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_05_are_you_safe,
        R.string.safe_arrival_works_are_you_safe_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_06_extend_time,
        R.string.safe_arrival_works_extend_time_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_07_grace_period,
        R.string.safe_arrival_works_grace_period_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_08_arrived,
        R.string.safe_arrival_works_arrived_description
    ),
    ImagePage(
        R.drawable.safe_arrival_works_09_no_response,
        R.string.safe_arrival_works_no_response_description
    )
)

/**
 * The Safe Arrival branch of "How PUKAAR Works".
 *
 * Unlike SOS it is set before anything goes wrong: the user names a time and
 * the people to tell, and PUKAAR only raises an alert if that time passes
 * without an answer.
 *
 * The file numbers match the running order. Resequencing means renaming the
 * drawables too, not just moving lines in this list.
 */
@Composable
fun SafeArrivalGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = SafeArrivalGuidePages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun SafeArrivalGuideScreenPreview() {
    PukaarTheme {
        SafeArrivalGuideScreen(onBack = {})
    }
}
