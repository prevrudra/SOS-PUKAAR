package com.pukaar.app.ui.navigation

import com.pukaar.app.ui.screen.success.SuccessType

/** Every navigable destination in the app, declared in one place. */
sealed class Route(val path: String) {

    data object Splash : Route("splash")
    data object Home : Route("home")
    data object Menu : Route("menu")

    // Priority menu items
    data object AddContact : Route("add_contact")
    data object EditContact : Route("edit_contact/{contactId}") {
        fun pathFor(id: String) = "edit_contact/$id"
        const val ARG_CONTACT_ID = "contactId"
    }
    data object Settings : Route("settings")
    data object SosSettings : Route("sos_settings")
    data object MockDrill : Route("mock_drill")
    data object ViewContacts : Route("view_contacts")
    data object ElderlyHelp : Route("elderly_help")
    data object EmergencyInfo : Route("emergency_info")
    data object PaymentReferral : Route("payment_referral")
    data object HelpVideo : Route("help_video")
    data object Recordings : Route("recordings")

    // Non-priority menu items
    data object Language : Route("language")
    data object Notifications : Route("notifications")
    data object Faq : Route("faq")
    data object About : Route("about")
    data object HowThisWorks : Route("how_this_works")
    data object WhatHappensAfterSos : Route("what_happens_after_sos")
    data object HomeModeGuide : Route("home_mode_guide")
    data object LegalTerms : Route("legal_terms")
    data object PrivacySecurity : Route("privacy_security")
    data object HowElderlyHelpWorks : Route("how_elderly_help_works")
    data object InactivityFeature : Route("inactivity_feature")

    /** Shared confirmation screen; the type decides the message it shows. */
    data object Success : Route("success/{$ARG_SUCCESS_TYPE}") {
        fun pathFor(type: SuccessType): String = "success/${type.name}"
    }

    companion object {
        const val ARG_SUCCESS_TYPE = "successType"
    }
}
