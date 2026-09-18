package com.pukaar.app.ui.screen.mockdrill

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.pukaar.app.R
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.theme.PukaarRed

/**
 * The drills the user can rehearse.
 *
 * Down to one now that HELP is gone, but the chooser still reads from this: the
 * next drill added is an entry here and nothing else, and the tile behaves the
 * same whether there is one of them or five.
 */
enum class DrillType(
    @StringRes val badgeRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val accent: Color,
    val route: Route
) {
    SOS(
        badgeRes = R.string.sos,
        titleRes = R.string.mock_drill_sos_title,
        descriptionRes = R.string.mock_drill_sos_description,
        accent = PukaarRed,
        route = Route.SosDrill
    )
}
