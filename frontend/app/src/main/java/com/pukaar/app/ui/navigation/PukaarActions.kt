package com.pukaar.app.ui.navigation

import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.home.HomeMode
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
    fun updateHomeMode(mode: HomeMode)

    // Add Contact
    fun saveContact(draft: ContactDraft)

    // SOS Settings
    fun saveSosSettings(form: SosSettingsForm)
    suspend fun loadSosSettings(): SosSettingsForm

    // Mock Drill
    fun startMockDrill(isSos: Boolean)

    // View Contacts
    fun loadContacts(): List<ContactUiModel>
    fun openContact(contact: ContactUiModel)

    // Elderly Help
    fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean)
    suspend fun loadElderlyHelp(): Pair<InactivityWindow, Boolean>

    // Emergency Info
    fun saveEmergencyInfo(form: EmergencyInfoForm)
    suspend fun loadEmergencyInfo(): EmergencyInfoForm

    // Payment / Plan
    fun upgradePlan(plan: String = "INDIVIDUAL", onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {})
    fun viewPaymentHistory()
    fun shareReferralCode()
    suspend fun loadSubscriptionUi(): SubscriptionUi

    // Help Video
    fun playIntroVideo()
    fun playTopic(topic: HelpTopic)

    // Language & Notifications
    fun saveLanguage(language: AppLanguage)
    fun saveNotificationPreferences(preferences: NotificationPreferences)
    suspend fun loadNotificationPreferences(): NotificationPreferences
    suspend fun loadLanguage(): AppLanguage

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
    override fun updateHomeMode(mode: HomeMode) = Unit
    override fun saveContact(draft: ContactDraft) = Unit
    override fun saveSosSettings(form: SosSettingsForm) = Unit
    override suspend fun loadSosSettings() = SosSettingsForm(true, true, true, true)
    override fun startMockDrill(isSos: Boolean) = Unit
    override fun loadContacts(): List<ContactUiModel> = SampleContacts
    override fun openContact(contact: ContactUiModel) = Unit
    override fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean) = Unit
    override suspend fun loadElderlyHelp() = InactivityWindow.TEN to true
    override fun saveEmergencyInfo(form: EmergencyInfoForm) = Unit
    override suspend fun loadEmergencyInfo() = EmergencyInfoForm()
    override fun upgradePlan(plan: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) = Unit
    override fun viewPaymentHistory() = Unit
    override fun shareReferralCode() = Unit
    override fun playIntroVideo() = Unit
    override fun playTopic(topic: HelpTopic) = Unit
    override fun saveLanguage(language: AppLanguage) = Unit
    override fun saveNotificationPreferences(preferences: NotificationPreferences) = Unit
    override suspend fun loadNotificationPreferences() = NotificationPreferences(true, true, true, false)
    override suspend fun loadLanguage() = AppLanguage.ENGLISH
    override fun openFaqEntry(entry: FaqEntry) = Unit
    override fun openSettings() = Unit
    override suspend fun loadSubscriptionUi() = SubscriptionUi("Premium", "—", "PUKAAR", false)
}

data class SubscriptionUi(
    val planName: String,
    val validTill: String,
    val referralCode: String,
    val isActive: Boolean,
    val individualPrice: Int = 499,
    val familyPrice: Int = 699,
    val referralCount: Long = 0
)

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
