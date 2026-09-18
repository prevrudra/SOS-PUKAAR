package com.pukaar.app.ui.screen.howitworks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val EmergencyCardGuidePages = listOf(
    // Opens on the three features together, so the card is introduced as one of
    // them rather than out of nowhere. Then what it is, when it helps and where to
    // keep it — the owner's half. Then the responder's: how they use it and what
    // they actually see. Then managing it, who it reaches, and why it matters.
    ImagePage(
        R.drawable.emergency_card_works_01_how_pukaar_works,
        R.string.emergency_card_works_hub_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_02_card_speaks,
        R.string.emergency_card_works_card_speaks_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_03_when_it_helps,
        R.string.emergency_card_works_when_it_helps_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_04_ways_to_carry,
        R.string.emergency_card_works_ways_to_carry_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_05_responder_steps,
        R.string.emergency_card_works_responder_steps_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_06_responder_view,
        R.string.emergency_card_works_responder_view_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_07_always_up_to_date,
        R.string.emergency_card_works_up_to_date_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_08_safety_network,
        R.string.emergency_card_works_safety_network_description
    ),
    ImagePage(
        R.drawable.emergency_card_works_09_may_not_ask,
        R.string.emergency_card_works_may_not_ask_description
    )
)

/**
 * The Emergency Card branch of "How PUKAAR Works".
 *
 * The other two walkthroughs explain an alert the user sets off. This one
 * explains a document that works when they cannot set off anything — made once,
 * carried, and read by a stranger who has no idea who they are.
 *
 * The file numbers match the running order, so a slide can be found by its
 * position as well as its name. Resequencing means renaming the drawables too,
 * not just moving lines in this list.
 */
@Composable
fun EmergencyCardGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = EmergencyCardGuidePages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun EmergencyCardGuideScreenPreview() {
    PukaarTheme {
        EmergencyCardGuideScreen(onBack = {})
    }
}
