package com.pukaar.app.ui.screen.menu

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.AccentGrey
import com.pukaar.app.ui.theme.AccentPurple
import com.pukaar.app.ui.theme.PukaarCoral
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarRedDark
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextPrimary

/**
 * Every tile the menu can show.
 *
 * Declaration order here is incidental — [stepRows] decides what the user sees
 * and in what order. Keeping the grid data-driven means adding a feature is one
 * entry here, one row placement below, and one `composable` in the nav graph.
 */
enum class MenuItem(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val tint: Color,
    /**
     * Where the tile leads, or null for one that does something instead of going
     * somewhere — the invite opens WhatsApp and never leaves the menu.
     */
    val route: Route?,
    /** Every tile says what it is for; none is left to its label alone. */
    @StringRes val subtitleRes: Int,
    /** Set for the badged tiles: a white glyph inside a filled circle. */
    val iconBackground: Color? = null,
    /**
     * Whether this tile takes a whole row — the wide [FeatureTile] treatment
     * rather than a square in the grid. Declared rather than inferred from the
     * row widths below, so the two can be read against each other.
     */
    val isFeature: Boolean = false,
    /**
     * A rim and a wash in this colour, for a row that should be noticed. Nothing
     * sets it: the menu reads as one list, and a tile wearing its own colour only
     * looked like it belonged to a different screen.
     */
    val accent: Color? = null,
    /** A pill beside the label, e.g. who the feature is for. */
    @StringRes val badgeRes: Int? = null,
    /**
     * Built, listed, but not ready. The tile still shows — taking it out and
     * putting it back would teach people the menu moves around — but it says so
     * and does not open. Only [isFeature] tiles render the mark.
     *
     * Nothing sets it while TripShield's inner flow is being built; put it back on
     * TRIPSHIELD before release if that flow is not finished.
     */
    val comingSoon: Boolean = false,
    /** Character index where the label flips from white to [accent], for wordmarks. */
    val labelAccentFrom: Int? = null
) {
    WHO_IS_PUKAAR_FOR(
        R.string.menu_who_is_pukaar_for,
        Icons.Filled.Groups,
        TextPrimary,
        Route.WhoIsPukaarFor,
        R.string.menu_who_is_pukaar_for_subtitle,
        iconBackground = PukaarCoral,
        isFeature = true
    ),
    HOW_PUKAAR_WORKS(
        R.string.menu_how_pukaar_works,
        Icons.Filled.Schema,
        TextPrimary,
        Route.HowItWorks,
        R.string.menu_how_pukaar_works_subtitle,
        iconBackground = AccentBlue,
        isFeature = true
    ),
    MOCK_DRILL(
        R.string.menu_mock_drill,
        Icons.AutoMirrored.Filled.DirectionsRun,
        TextPrimary,
        Route.MockDrill,
        R.string.menu_mock_drill_subtitle,
        iconBackground = PukaarRed,
        isFeature = true
    ),
    INACTIVITY(
        R.string.menu_inactivity,
        Icons.Filled.Elderly,
        TextPrimary,
        Route.ElderlyHelp,
        R.string.menu_inactivity_subtitle,
        iconBackground = PukaarOrange,
        isFeature = true,
        badgeRes = R.string.menu_inactivity_badge
    ),
    TRIPSHIELD(
        R.string.menu_tripshield,
        Icons.Filled.Flight,
        TextPrimary,
        Route.TripShield,
        R.string.menu_tripshield_subtitle,
        iconBackground = PukaarRedDark,
        isFeature = true,
        badgeRes = R.string.menu_tripshield_badge,
        // "TRIP" stays white, "SHIELD" picks up the red.
        labelAccentFrom = 4
    ),
    EMERGENCY_CARD(
        R.string.menu_emergency_card,
        Icons.Filled.ContactEmergency,
        TextPrimary,
        Route.EmergencyCard,
        R.string.menu_emergency_card_subtitle,
        iconBackground = AccentPurple,
        isFeature = true
    ),
    PAYMENT_REFERRAL(
        R.string.menu_payment_referral,
        Icons.Filled.Wallet,
        TextPrimary,
        Route.PaymentReferral,
        R.string.menu_payment_referral_subtitle,
        iconBackground = SuccessGreen,
        isFeature = true
    ),
    INVITE(
        R.string.menu_invite,
        Icons.AutoMirrored.Filled.Send,
        TextPrimary,
        // Goes nowhere: it hands the invite to WhatsApp and leaves the user here.
        null,
        R.string.menu_invite_subtitle,
        iconBackground = PukaarOrange,
        isFeature = true
    ),
    QUICK_ONBOARDING(
        R.string.menu_quick_onboarding,
        Icons.Filled.HowToReg,
        SuccessGreen,
        Route.QuickOnboarding,
        R.string.menu_quick_onboarding_subtitle
    ),
    ADD_CONTACT(
        R.string.menu_add_contact,
        Icons.Filled.PersonAdd,
        PukaarOrange,
        Route.AddContact,
        R.string.menu_add_contact_sub,
        iconBackground = PukaarOrange
    ),
    VIEW_CONTACTS(
        R.string.menu_view_contacts,
        Icons.Filled.Groups,
        AccentBlue,
        Route.ViewContacts,
        R.string.menu_view_contacts_subtitle
    ),
    GENERAL(
        R.string.menu_general,
        Icons.Filled.Settings,
        AccentGrey,
        Route.GeneralSettings,
        R.string.menu_general_subtitle
    ),
    LANGUAGE(
        R.string.menu_language,
        Icons.Filled.Language,
        TextPrimary,
        Route.Language,
        R.string.menu_language_subtitle
    ),
    FAQ(
        R.string.menu_faq,
        Icons.AutoMirrored.Filled.Help,
        TextPrimary,
        Route.Faq,
        R.string.menu_faq_subtitle,
        iconBackground = AccentBlue,
        // An odd one out since HELP left: five squares will not pair up, and a
        // lone full-width square reads worse than a proper wide row.
        isFeature = true
    );

    companion object {
        /**
         * The menu, exactly as drawn — one ordered run of rows, no sections.
         *
         * Row widths vary deliberately: tiles share their row evenly, so a row of
         * one spans the full width and a row of three splits into thirds.
         *
         * Every [MenuItem] must appear here exactly once.
         */
        val stepRows: List<List<MenuItem>> = listOf(
            listOf(WHO_IS_PUKAAR_FOR),
            listOf(HOW_PUKAAR_WORKS),
            listOf(MOCK_DRILL),
            listOf(INACTIVITY),
            listOf(TRIPSHIELD),
            listOf(EMERGENCY_CARD),
            listOf(PAYMENT_REFERRAL),
            listOf(INVITE),
            listOf(QUICK_ONBOARDING),
            listOf(ADD_CONTACT, VIEW_CONTACTS),
            listOf(GENERAL, LANGUAGE),
            listOf(FAQ)
        )
    }
}
