package com.pukaar.app.ui.screen.home

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pukaar.app.R
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarOrangeDark
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarRedDark

/**
 * The two things the home button can be.
 *
 * SOS is a life-threatening emergency; HELP is the softer case where the user
 * wants family rather than the emergency services. Everything that differs
 * between the two — colour, wording, the button label — is declared here, so the
 * screen itself is written once.
 */
enum class HomeMode(
    @StringRes val toggleLabelRes: Int,
    @StringRes val buttonLabelRes: Int,
    @StringRes val headlineRes: Int,
    @StringRes val descriptionRes: Int?,
    val accent: Color,
    val accentDark: Color
) {
    SOS(
        toggleLabelRes = R.string.mode_sos,
        buttonLabelRes = R.string.sos,
        headlineRes = R.string.home_sos_headline,
        descriptionRes = null,
        accent = PukaarRed,
        accentDark = PukaarRedDark
    ),
    HELP(
        toggleLabelRes = R.string.mode_help,
        buttonLabelRes = R.string.help,
        headlineRes = R.string.home_help_headline,
        descriptionRes = R.string.home_help_description,
        accent = PukaarOrange,
        accentDark = PukaarOrangeDark
    )
}
