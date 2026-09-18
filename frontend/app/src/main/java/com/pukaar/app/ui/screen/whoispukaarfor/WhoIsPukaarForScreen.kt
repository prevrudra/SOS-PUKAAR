package com.pukaar.app.ui.screen.whoispukaarfor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ImagePage
import com.pukaar.app.ui.component.ImagePagerScreen
import com.pukaar.app.ui.theme.PukaarTheme

private val WhoIsPukaarForPages = listOf(
    // Opens on everyone together, then one page per kind of person — numbered 1 to
    // 12 on the artwork itself — and closes on the four features joined as one.
    ImagePage(R.drawable.who_is_pukaar_for_00_intro, R.string.who_is_pukaar_for_intro_description),
    ImagePage(
        R.drawable.who_is_pukaar_for_01_students_india,
        R.string.who_is_pukaar_for_students_india_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_02_students_abroad,
        R.string.who_is_pukaar_for_students_abroad_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_03_working_professionals,
        R.string.who_is_pukaar_for_working_professionals_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_04_business_travellers,
        R.string.who_is_pukaar_for_business_travellers_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_05_on_the_road,
        R.string.who_is_pukaar_for_on_the_road_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_06_women_teenage_girls,
        R.string.who_is_pukaar_for_women_description
    ),
    ImagePage(R.drawable.who_is_pukaar_for_07_elderly, R.string.who_is_pukaar_for_elderly_description),
    ImagePage(
        R.drawable.who_is_pukaar_for_08_night_shift,
        R.string.who_is_pukaar_for_night_shift_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_09_solo_travellers,
        R.string.who_is_pukaar_for_solo_travellers_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_10_outdoor_adventure,
        R.string.who_is_pukaar_for_outdoor_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_11_families_far_away,
        R.string.who_is_pukaar_for_families_far_away_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_12_living_alone,
        R.string.who_is_pukaar_for_living_alone_description
    ),
    ImagePage(
        R.drawable.who_is_pukaar_for_13_connection,
        R.string.who_is_pukaar_for_connection_description
    )
)

/**
 * The first tile on the menu: the people PUKAAR is meant for.
 *
 * Every other walkthrough explains what the app does. This one comes before
 * that — whether it is for you, or for someone you worry about — so it leads
 * the menu.
 *
 * The file numbers match the running order and the number printed on each
 * slide. Resequencing means renaming the drawables too.
 */
@Composable
fun WhoIsPukaarForScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ImagePagerScreen(pages = WhoIsPukaarForPages, onBack = onBack, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun WhoIsPukaarForScreenPreview() {
    PukaarTheme {
        WhoIsPukaarForScreen(onBack = {})
    }
}
