package com.pukaar.app.ui.screen.howitworks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val HowPukaarHelpsPages = listOf(
    // The three together, then one page each, then the three together again —
    // the walkthrough opens by saying what is coming and closes by naming it.
    ImagePage(R.drawable.pukaar_helps_01_intro, R.string.pukaar_helps_intro_description),
    ImagePage(R.drawable.pukaar_helps_02_sos, R.string.pukaar_helps_sos_description),
    ImagePage(R.drawable.pukaar_helps_03_inactivity, R.string.pukaar_helps_inactivity_description),
    ImagePage(R.drawable.pukaar_helps_04_emergency_card, R.string.pukaar_helps_card_description),
    ImagePage(R.drawable.pukaar_helps_05_be_prepared, R.string.pukaar_helps_prepared_description)
)

/**
 * The last card on the "How PUKAAR Works" hub.
 *
 * The walkthroughs behind the other three show what the app does once it has
 * been triggered. This one answers the question that comes first — is this a
 * moment to use it at all? — by showing the situations rather than listing them,
 * because somebody deciding whether their own moment counts recognises a scene
 * faster than they read a bullet.
 */
@Composable
fun WhenToUseScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = HowPukaarHelpsPages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun WhenToUseScreenPreview() {
    PukaarTheme {
        WhenToUseScreen(onBack = {})
    }
}
