package com.pukaar.app.ui.screen.tripshield

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Sos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.AccentGrey
import com.pukaar.app.ui.theme.AccentPurple
import com.pukaar.app.ui.theme.AccentSky
import com.pukaar.app.ui.theme.AccentTeal
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen

/**
 * Every row the TripShield menu can show.
 *
 * The same shape as the main menu's `MenuItem`, minus the route: none of these
 * lead anywhere yet, so the screen owns the click and the destinations get added
 * one at a time later.
 */
enum class TripShieldMenuItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    /** The filled circle behind the glyph; the glyph itself is always white. */
    val iconBackground: Color
) {
    HOW_IT_WORKS(
        R.string.tripshield_how_it_works,
        R.string.tripshield_how_it_works_subtitle,
        Icons.Filled.Info,
        AccentBlue
    ),
    QUICK_ONBOARDING(
        R.string.tripshield_quick_onboarding,
        R.string.tripshield_quick_onboarding_subtitle,
        Icons.Filled.AssignmentTurnedIn,
        SuccessGreen
    ),
    SOS(
        R.string.tripshield_sos,
        R.string.tripshield_sos_subtitle,
        Icons.Filled.Sos,
        PukaarRed
    ),
    STAY_WITH_ME(
        R.string.tripshield_stay_with_me,
        R.string.tripshield_stay_with_me_subtitle,
        Icons.Filled.ShareLocation,
        AccentPurple
    ),
    CHECK_ON_ME(
        R.string.tripshield_check_on_me,
        R.string.tripshield_check_on_me_subtitle,
        Icons.Filled.AdminPanelSettings,
        PukaarOrange
    ),
    VIEW_CONTACTS(
        R.string.tripshield_view_contacts,
        R.string.tripshield_view_contacts_subtitle,
        Icons.Filled.People,
        AccentSky
    ),
    EMERGENCY_INFORMATION(
        R.string.tripshield_emergency_information,
        R.string.tripshield_emergency_information_subtitle,
        Icons.Filled.AccountBalance,
        AccentTeal
    ),
    GENERAL(
        R.string.tripshield_general,
        R.string.tripshield_general_subtitle,
        Icons.Filled.Settings,
        AccentGrey
    );

    companion object {
        /**
         * The menu, exactly as drawn — five full-width rows, then the pair that
         * share a row, then General.
         *
         * Every [TripShieldMenuItem] must appear here exactly once.
         */
        val rows: List<List<TripShieldMenuItem>> = listOf(
            listOf(HOW_IT_WORKS),
            listOf(QUICK_ONBOARDING),
            listOf(SOS),
            listOf(STAY_WITH_ME),
            listOf(CHECK_ON_ME),
            listOf(VIEW_CONTACTS, EMERGENCY_INFORMATION),
            listOf(GENERAL)
        )
    }
}
