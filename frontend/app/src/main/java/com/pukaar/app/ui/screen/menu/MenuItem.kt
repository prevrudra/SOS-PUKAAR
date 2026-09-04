package com.pukaar.app.ui.screen.menu

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Elderly
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.AccentGrey
import com.pukaar.app.ui.theme.AccentOrange
import com.pukaar.app.ui.theme.AccentPurple
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
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
    val route: Route,
    /** Set for the badged tiles: a white glyph inside a filled circle. */
    val iconBackground: Color? = null
) {
    ADD_CONTACT(R.string.menu_add_contact, Icons.Filled.PersonAdd, TextPrimary, Route.AddContact),
    SOS_SETTINGS(R.string.menu_sos_settings, Icons.Filled.Sos, PukaarRed, Route.SosSettings),
    MOCK_DRILL(R.string.menu_mock_drill, Icons.Filled.PhoneInTalk, PukaarRed, Route.MockDrill),
    VIEW_CONTACTS(R.string.menu_view_contacts, Icons.Filled.Groups, TextPrimary, Route.ViewContacts),
    ELDERLY_HELP(R.string.menu_elderly_help, Icons.Outlined.Elderly, AccentOrange, Route.ElderlyHelp),
    EMERGENCY_INFO(R.string.menu_emergency_info, Icons.Filled.MedicalServices, PukaarRed, Route.EmergencyInfo),
    RECORDINGS(R.string.menu_recordings, Icons.Filled.Mic, AccentPurple, Route.Recordings),
    PAYMENT_REFERRAL(R.string.menu_payment_referral, Icons.Filled.Wallet, SuccessGreen, Route.PaymentReferral),
    HELP_VIDEO(R.string.menu_help_video, Icons.Filled.PlayCircleFilled, AccentPurple, Route.HelpVideo),

    HOW_PUKAAR_WORKS(
        R.string.menu_how_pukaar_works,
        Icons.Filled.Schema,
        TextPrimary,
        Route.HowThisWorks,
        iconBackground = AccentBlue
    ),
    WHAT_HAPPENS_AFTER_SOS(
        R.string.menu_what_happens_after_sos,
        Icons.Filled.Warning,
        TextPrimary,
        Route.WhatHappensAfterSos,
        iconBackground = PukaarRed
    ),
    HOME_MODE_GUIDE(
        R.string.menu_home_mode_guide,
        Icons.Filled.ToggleOn,
        TextPrimary,
        Route.HomeModeGuide,
        iconBackground = AccentBlue
    ),
    HOW_ELDERLY_HELP_WORKS(
        R.string.menu_how_elderly_help_works,
        Icons.Filled.Elderly,
        TextPrimary,
        Route.HowElderlyHelpWorks,
        iconBackground = PukaarOrange
    ),
    INACTIVITY_FEATURE(
        R.string.menu_inactivity_feature,
        Icons.Filled.Timer,
        TextPrimary,
        Route.InactivityFeature,
        iconBackground = PukaarOrange
    ),

    LANGUAGE(R.string.menu_language, Icons.Filled.Language, TextPrimary, Route.Language),
    NOTIFICATIONS(R.string.menu_notifications, Icons.Filled.Notifications, TextPrimary, Route.Notifications),
    LEGAL_TERMS(R.string.menu_legal_terms, Icons.Filled.Description, AccentPurple, Route.LegalTerms),
    PRIVACY_SECURITY(R.string.menu_privacy_security, Icons.Filled.Lock, SuccessGreen, Route.PrivacySecurity),
    FAQ(R.string.menu_faq, Icons.AutoMirrored.Filled.Help, AccentBlue, Route.Faq),
    ABOUT(R.string.menu_about, Icons.Filled.Info, AccentGrey, Route.About);

    companion object {
        /**
         * The menu, exactly as drawn — one ordered run of rows, no sections.
         *
         * Row widths vary deliberately: tiles share their row evenly, so a row of
         * one spans the full width and a row of three splits into thirds.
         *
         * Every [MenuItem] must appear here exactly once; `MenuItemTest` enforces
         * it, so a new tile cannot go missing from the menu.
         */
        val stepRows: List<List<MenuItem>> = listOf(
            listOf(HOW_PUKAAR_WORKS, WHAT_HAPPENS_AFTER_SOS),
            listOf(HOW_ELDERLY_HELP_WORKS, INACTIVITY_FEATURE),
            listOf(MOCK_DRILL),
            listOf(PAYMENT_REFERRAL),
            listOf(ADD_CONTACT, VIEW_CONTACTS),
            listOf(SOS_SETTINGS, ELDERLY_HELP),
            listOf(EMERGENCY_INFO, RECORDINGS),
            listOf(FAQ),
            listOf(HELP_VIDEO, HOME_MODE_GUIDE),
            listOf(LANGUAGE, NOTIFICATIONS),
            listOf(LEGAL_TERMS, PRIVACY_SECURITY),
            listOf(ABOUT)
        )
    }
}
