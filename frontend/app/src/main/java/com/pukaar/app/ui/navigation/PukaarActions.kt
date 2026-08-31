package com.pukaar.app.ui.navigation

import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.helpvideo.HelpTopic
import com.pukaar.app.ui.screen.language.AppLanguage
import com.pukaar.app.ui.screen.notifications.NotificationPreferences
import com.pukaar.app.ui.screen.sossettings.SosSettingsForm

/**
 * Every side effect the UI can ask for, declared but not implemented.
 *
 * The screens are pure Compose and never reach for a repository themselves; they
 * call one of these. Wiring the app up later means providing a real implementation
 * — most likely a set of ViewModels over `domain/usecase` — without touching a
 * single composable.
 */
interface PukaarActions {

    // Home
    fun triggerSos()
    fun triggerHelp()

    // Add Contact
    fun saveContact(draft: ContactDraft)

    // SOS Settings
    fun saveSosSettings(form: SosSettingsForm)

    // Mock Drill
    fun startMockDrill()

    // View Contacts
    fun loadContacts(): List<ContactUiModel>
    fun openContact(contact: ContactUiModel)

    // Elderly Help
    fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean)

    // Emergency Info
    fun editBloodGroup()
    fun editAllergies()
    fun editConditions()
    fun saveEmergencyInfo(doctorContact: String)

    // Payment / Plan
    fun upgradePlan()
    fun viewPaymentHistory()
    fun shareReferralCode()

    // Help Video
    fun playIntroVideo()
    fun playTopic(topic: HelpTopic)

    // Language & Notifications
    fun saveLanguage(language: AppLanguage)
    fun saveNotificationPreferences(preferences: NotificationPreferences)

    // FAQ
    fun openFaqEntry(entry: FaqEntry)

    // Settings gear on the menu header
    fun openSettings()
}

/**
 * Stand-in used while the app is UI-only. Every call is a no-op, so the whole
 * flow is clickable end to end without any behaviour behind it.
 */
object NoOpPukaarActions : PukaarActions {
    override fun triggerSos() = Unit
    override fun triggerHelp() = Unit
    override fun saveContact(draft: ContactDraft) = Unit
    override fun saveSosSettings(form: SosSettingsForm) = Unit
    override fun startMockDrill() = Unit
    override fun loadContacts(): List<ContactUiModel> = SampleContacts
    override fun openContact(contact: ContactUiModel) = Unit
    override fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean) = Unit
    override fun editBloodGroup() = Unit
    override fun editAllergies() = Unit
    override fun editConditions() = Unit
    override fun saveEmergencyInfo(doctorContact: String) = Unit
    override fun upgradePlan() = Unit
    override fun viewPaymentHistory() = Unit
    override fun shareReferralCode() = Unit
    override fun playIntroVideo() = Unit
    override fun playTopic(topic: HelpTopic) = Unit
    override fun saveLanguage(language: AppLanguage) = Unit
    override fun saveNotificationPreferences(preferences: NotificationPreferences) = Unit
    override fun openFaqEntry(entry: FaqEntry) = Unit
    override fun openSettings() = Unit
}

/** Placeholder rows so the contacts screen has something to draw — three per category. */
private val SampleContacts = listOf(
    ContactUiModel("1", "Son", "+91 98765 43210", ContactType.SOS),
    ContactUiModel("2", "Spouse", "+91 98765 43211", ContactType.SOS),
    ContactUiModel("3", "Neighbour", "+91 98765 43212", ContactType.SOS),

    ContactUiModel("4", "Daughter", "+91 98765 43213", ContactType.HELP),
    ContactUiModel("5", "Brother", "+91 98765 43214", ContactType.HELP),
    ContactUiModel("6", "Best Friend", "+91 98765 43215", ContactType.HELP),

    ContactUiModel("7", "Sister", "+91 98765 43216", ContactType.INACTIVITY),
    ContactUiModel("8", "Caretaker", "+91 98765 43217", ContactType.INACTIVITY),
    ContactUiModel("9", "Family Doctor", "+91 98765 43218", ContactType.INACTIVITY)
)
