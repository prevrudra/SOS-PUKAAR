package com.pukaar.app.ui.navigation

import com.pukaar.app.ui.screen.success.SuccessType

/** Every navigable destination in the app, declared in one place. */
sealed class Route(val path: String) {

    data object Splash : Route("splash")
    data object Home : Route("home")
    data object Menu : Route("menu")

    // Ritik menu tiles
    data object WhoIsPukaarFor : Route("who_is_pukaar_for")
    data object HowItWorks : Route("how_it_works")
    data object MockDrill : Route("mock_drill")
    data object TripShield : Route("tripshield")
    data object SosDrill : Route("drill/sos")
    data object PaymentReferral : Route("payment_referral")
    data object QuickOnboarding : Route("quick_onboarding")
    data object ViewContacts : Route("view_contacts")
    data object GeneralSettings : Route("general_settings")
    data object Language : Route("language")
    data object Faq : Route("faq")

    data object EmergencyCard : Route("emergency_card")
    data object EmergencyCardReady : Route("emergency_card/ready")
    data object EmergencyCardDownload : Route("emergency_card/download")
    data object EmergencyCardPrintable : Route("emergency_card/printable")
    data object EmergencyCardLockScreen : Route("emergency_card/lock_screen")

    data object SosGuide : Route("guide/sos")
    data object InactivityGuide : Route("guide/inactivity")
    data object EmergencyCardGuide : Route("guide/emergency_card")
    data object WhenToUse : Route("guide/when_to_use")
    data object SafeArrivalGuide : Route("guide/safe_arrival")

    data object SosOnboarding : Route("onboarding/sos")
    data object InactivityOnboarding : Route("onboarding/inactivity")

    // Production / backend-wired extras
    data object AddContact : Route("add_contact?type={type}") {
        fun pathFor(type: String) = "add_contact?type=$type"
        const val ARG_TYPE = "type"
    }
    data object EditContact : Route("edit_contact/{contactId}") {
        fun pathFor(id: String) = "edit_contact/$id"
        const val ARG_CONTACT_ID = "contactId"
    }
    data object Settings : Route("settings")
    data object SosSettings : Route("sos_settings")
    data object ElderlyHelp : Route("elderly_help")
    data object EmergencyInfo : Route("emergency_info")
    data object HelpVideo : Route("help_video")
    data object Recordings : Route("recordings")
    data object Notifications : Route("notifications")
    data object About : Route("about")
    data object HowThisWorks : Route("how_this_works")
    data object WhatHappensAfterSos : Route("what_happens_after_sos")
    data object HomeModeGuide : Route("home_mode_guide")
    data object LegalTerms : Route("legal_terms")
    data object PrivacySecurity : Route("privacy_security")
    data object HowElderlyHelpWorks : Route("how_elderly_help_works")
    data object InactivityFeature : Route("inactivity_feature")

    data object Success : Route("success/{$ARG_SUCCESS_TYPE}") {
        fun pathFor(type: SuccessType): String = "success/${type.name}"
    }

    companion object {
        const val ARG_SUCCESS_TYPE = "successType"
    }
}
