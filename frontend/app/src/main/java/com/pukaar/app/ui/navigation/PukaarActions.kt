package com.pukaar.app.ui.navigation

import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.contacts.PreSavedNumberUiModel
import com.pukaar.app.ui.screen.emergencycard.EmergencyCardDraft
import com.pukaar.app.ui.screen.emergencycard.PrintOptions
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.general.GeneralSettings
import com.pukaar.app.ui.screen.onboarding.InactivityTiming
import com.pukaar.app.ui.screen.onboarding.OnboardingResult
import com.pukaar.app.ui.screen.payment.SubscriptionPlan
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow
import com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm
import com.pukaar.app.ui.screen.helpvideo.HelpTopic
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.notifications.NotificationPreferences
import com.pukaar.app.ui.screen.sossettings.SosSettingsForm
import com.pukaar.app.ui.screen.language.AppLanguage

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

    // Onboarding — the finished SOS or inactivity setup
    /** Persist onboarding result, then invoke [onDone] (true = saved). */
    fun completeOnboarding(result: OnboardingResult, onDone: (ok: Boolean) -> Unit = {})

    /**
     * The coupon typed at the gate in front of a setup.
     *
     * Declared so the code has somewhere to go rather than being read and thrown
     * away. Nothing checks it yet — the dialog only requires that something was
     * typed — so real validation lands here, and the gate will need to be told
     * whether it passed before this can be called a check.
     */
    fun redeemCoupon(code: String, type: ProtectionType)

    /** Send a trusted contact the High Alert app download link (WhatsApp-first). */
    fun shareApp(phoneE164: String? = null)

    /** Elder's display name — shown on SOS/inactivity alerts to trusted contacts. */
    fun saveUserDisplayName(name: String)

    // Mock Drill
    fun startMockDrill()

    // View Contacts
    fun loadContacts(): List<ContactUiModel>

    /** The numbers a trusted contact is handed when an inactivity alert fires. */
    fun loadPreSavedNumbers(): List<PreSavedNumberUiModel>

    /** Store a contact edited on the contacts screen, matched on its id. */
    fun saveContact(contact: ContactUiModel)
    fun deleteContact(contact: ContactUiModel)

    fun savePreSavedNumber(number: PreSavedNumberUiModel)
    fun deletePreSavedNumber(number: PreSavedNumberUiModel)

    /** The inactivity timings set during onboarding, shown on the contacts screen. */
    fun loadInactivityTiming(): InactivityTiming

    /** Store timings the user changed after onboarding. */
    fun saveInactivityTiming(timing: InactivityTiming)

    // Payment / Plan
    /**
     * Whether the subscription behind SOS is live.
     *
     * The home button asks before it fires: an SOS that goes nowhere is worse than
     * one that refuses, because only the refusal is noticed in time. Nothing sells
     * a plan yet, so this is false throughout and the button always explains
     * itself — flip it here once plans exist.
     */
    fun isPlanActive(): Boolean

    /** Start the purchase of [plan]; the plans screen calls it when a card is tapped. */
    fun upgradePlan(plan: SubscriptionPlan)

    // General & Language
    fun saveGeneralSettings(settings: GeneralSettings)
    fun saveLanguage(language: AppLanguage)

    // FAQ
    fun openFaqEntry(entry: FaqEntry)

    // Emergency Card
    /** The card as last submitted, so the flow reopens on it rather than blank. */
    fun loadEmergencyCard(): EmergencyCardDraft?

    /** Store a finished card. Called on Generate, and again on every re-edit. */
    fun saveEmergencyCard(draft: EmergencyCardDraft)

    /**
     * The things done with a finished card that leave the app: a file written to
     * the gallery, a share sheet, and a PDF or PNG in Downloads. Declared here like
     * every other side effect — the screens stay pure Compose and the platform
     * work lands in one place.
     *
     * Setting the lock screen is the exception: it needs the card as drawn on
     * screen, so `LockScreenCardScreen` captures it and sets it itself.
     */
    fun saveCardQr(draft: EmergencyCardDraft)
    fun shareCardQr(draft: EmergencyCardDraft)
    fun downloadCard(draft: EmergencyCardDraft, options: PrintOptions)


    // Production SOS / HELP / contacts form
    fun triggerHelp()
    fun updateHomeMode(mode: HomeMode)
    fun saveContact(draft: ContactDraft)
    fun openContact(contact: ContactUiModel)
    fun startMockDrill(isSos: Boolean)
    fun saveSosSettings(form: SosSettingsForm)
    suspend fun loadSosSettings(): SosSettingsForm
    fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean)
    suspend fun loadElderlyHelp(): Pair<InactivityWindow, Boolean>
    fun saveEmergencyInfo(form: EmergencyInfoForm)
    suspend fun loadEmergencyInfo(): EmergencyInfoForm
    fun viewPaymentHistory()
    fun shareReferralCode()
    suspend fun loadSubscriptionUi(): SubscriptionUi
    fun playIntroVideo()
    fun playTopic(topic: HelpTopic)
    fun saveNotificationPreferences(preferences: NotificationPreferences)
    suspend fun loadNotificationPreferences(): NotificationPreferences
    suspend fun loadLanguage(): AppLanguage

    // Settings gear on the menu header
    fun openSettings()
}

/**
 * Stand-in that does nothing and remembers nothing — for previews and tests that
 * want a screen with no behaviour behind it. The running app uses
 * [InMemoryPukaarActions].
 */
object NoOpPukaarActions : PukaarActions {
    override fun triggerSos() = Unit
    override fun completeOnboarding(result: OnboardingResult, onDone: (ok: Boolean) -> Unit) {
        onDone(true)
    }
    override fun redeemCoupon(code: String, type: ProtectionType) = Unit
    override fun shareApp(phoneE164: String?) = Unit
    override fun saveUserDisplayName(name: String) = Unit
    override fun startMockDrill() = Unit
    override fun loadContacts(): List<ContactUiModel> = emptyList()
    override fun loadPreSavedNumbers(): List<PreSavedNumberUiModel> = emptyList()
    override fun saveContact(contact: ContactUiModel) = Unit
    override fun deleteContact(contact: ContactUiModel) = Unit
    override fun savePreSavedNumber(number: PreSavedNumberUiModel) = Unit
    override fun deletePreSavedNumber(number: PreSavedNumberUiModel) = Unit
    override fun loadInactivityTiming(): InactivityTiming = InactivityTiming()
    override fun saveInactivityTiming(timing: InactivityTiming) = Unit
    override fun isPlanActive(): Boolean = false
    override fun upgradePlan(plan: SubscriptionPlan) = Unit
    override fun saveGeneralSettings(settings: GeneralSettings) = Unit
    override fun saveLanguage(language: AppLanguage) = Unit
    override fun openFaqEntry(entry: FaqEntry) = Unit
    override fun loadEmergencyCard(): EmergencyCardDraft? = null
    override fun saveEmergencyCard(draft: EmergencyCardDraft) = Unit
    override fun saveCardQr(draft: EmergencyCardDraft) = Unit
    override fun shareCardQr(draft: EmergencyCardDraft) = Unit
    override fun downloadCard(draft: EmergencyCardDraft, options: PrintOptions) = Unit
    override fun openSettings() = Unit
    override fun triggerHelp() = Unit
    override fun updateHomeMode(mode: HomeMode) = Unit
    override fun saveContact(draft: ContactDraft) = Unit
    override fun openContact(contact: ContactUiModel) = Unit
    override fun startMockDrill(isSos: Boolean) = Unit
    override fun saveSosSettings(form: SosSettingsForm) = Unit
    override suspend fun loadSosSettings(): SosSettingsForm =
        SosSettingsForm(
            autoCallEmergencyNumber = true,
            shareLocation = true,
            recordAudio = true,
            alertContacts = true
        )
    override fun saveElderlyHelp(window: InactivityWindow, medicationReminder: Boolean) = Unit
    override suspend fun loadElderlyHelp(): Pair<InactivityWindow, Boolean> =
        InactivityWindow.SIX to false
    override fun saveEmergencyInfo(form: EmergencyInfoForm) = Unit
    override suspend fun loadEmergencyInfo(): EmergencyInfoForm = EmergencyInfoForm()
    override fun viewPaymentHistory() = Unit
    override fun shareReferralCode() = Unit
    override suspend fun loadSubscriptionUi(): SubscriptionUi = SubscriptionUi()
    override fun playIntroVideo() = Unit
    override fun playTopic(topic: HelpTopic) = Unit
    override fun saveNotificationPreferences(preferences: NotificationPreferences) = Unit
    override suspend fun loadNotificationPreferences(): NotificationPreferences =
        NotificationPreferences(
            alertNotifications = true,
            inactivityAlerts = true,
            medicationReminders = false,
            promotions = false
        )
    override suspend fun loadLanguage(): AppLanguage = AppLanguage.ENGLISH
}

data class SubscriptionUi(
    val planName: String = "—",
    val validTill: String = "—",
    val referralCode: String = "PUKAAR",
    val isActive: Boolean = false,
    val individualPrice: Int = 499,
    val familyPrice: Int = 699,
    val referralCount: Int = 0
)
