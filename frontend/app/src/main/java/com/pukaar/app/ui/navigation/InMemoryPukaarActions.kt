package com.pukaar.app.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pukaar.app.ui.screen.contacts.ContactPriority
import com.pukaar.app.ui.screen.contacts.ContactRelation
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.contacts.PreSavedNumberUiModel
import com.pukaar.app.ui.screen.emergencycard.EmergencyCardDraft
import com.pukaar.app.ui.screen.emergencycard.PrintOptions
import com.pukaar.app.ui.screen.faq.FaqEntry
import com.pukaar.app.ui.screen.general.GeneralSettings
import com.pukaar.app.ui.screen.language.AppLanguage
import com.pukaar.app.ui.screen.onboarding.InactivityTiming
import com.pukaar.app.ui.screen.onboarding.OnboardingContact
import com.pukaar.app.ui.screen.onboarding.OnboardingResult
import com.pukaar.app.ui.screen.payment.SubscriptionPlan
import com.pukaar.app.ui.screen.protection.ProtectionType

/**
 * What the app remembers while it is running.
 *
 * A finished setup lands here and the contacts screen reads it back, so the setup
 * flows and the list they fill are actually joined up. The lists are snapshot
 * state, so a save or a delete redraws whatever is on screen without anything
 * having to ask again.
 *
 * Memory only — nothing survives the process. It is the seam a real repository
 * slots into later: the screens already talk to [PukaarActions] and nothing else.
 */
object InMemoryPukaarActions : PukaarActions {

    private val contacts = mutableStateListOf<ContactUiModel>().apply { addAll(SeedContacts) }

    private val preSavedNumbers =
        mutableStateListOf<PreSavedNumberUiModel>().apply { addAll(SeedPreSavedNumbers) }

    private var timing by mutableStateOf(InactivityTiming())

    private var emergencyCard by mutableStateOf<EmergencyCardDraft?>(null)

    /** The coupon each setup was unlocked with, newest wins. */
    private val coupons = mutableStateMapOf<ProtectionType, String>()

    override fun triggerSos() = Unit

    /**
     * A finished flow replaces everything for its own protection and leaves the
     * other alone — the user has just said, start to finish, who handles this
     * one alert.
     */
    override fun completeOnboarding(
        result: OnboardingResult,
        onDone: (ok: Boolean) -> Unit
    ) {
        // Memory store — always succeeds.
        val type = result.type.contactType
        contacts.removeAll { it.type == type }
        contacts += result.primaryContacts.map { it.toUiModel(type, ContactPriority.PRIMARY) }
        contacts += result.secondaryContacts.map { it.toUiModel(type, ContactPriority.SECONDARY) }
        preSavedNumbers.removeAll { it.type == type }
        preSavedNumbers += result.helpNumbers.map { number ->
            PreSavedNumberUiModel(
                id = number.id,
                name = number.label,
                phoneNumber = number.phone,
                type = type,
                relation = number.relation
            )
        }
        result.timing?.let { timing = it }
        onDone(true)
    }

    /**
     * Remembered rather than checked: there is nothing yet to check it against,
     * and the last code entered is what a real implementation would send.
     */
    override fun redeemCoupon(code: String, type: ProtectionType) {
        coupons[type] = code
    }

    override fun shareApp(phoneE164: String?) = Unit
    override fun saveUserDisplayName(name: String) = Unit

    override fun startMockDrill() = Unit

    override fun loadContacts(): List<ContactUiModel> = contacts.toList()

    override fun loadPreSavedNumbers(): List<PreSavedNumberUiModel> = preSavedNumbers.toList()

    override fun saveContact(contact: ContactUiModel) {
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index >= 0) contacts[index] = contact else contacts += contact
    }

    override fun deleteContact(contact: ContactUiModel) {
        contacts.removeAll { it.id == contact.id }
    }

    override fun savePreSavedNumber(number: PreSavedNumberUiModel) {
        val index = preSavedNumbers.indexOfFirst { it.id == number.id }
        if (index >= 0) preSavedNumbers[index] = number else preSavedNumbers += number
    }

    override fun deletePreSavedNumber(number: PreSavedNumberUiModel) {
        preSavedNumbers.removeAll { it.id == number.id }
    }

    override fun loadInactivityTiming(): InactivityTiming = timing

    override fun saveInactivityTiming(timing: InactivityTiming) {
        this.timing = timing
    }

    /** Nothing activates a plan yet, so SOS always explains itself instead of firing. */
    override fun isPlanActive(): Boolean = false

    override fun upgradePlan(plan: SubscriptionPlan) = Unit

    override fun saveGeneralSettings(settings: GeneralSettings) = Unit

    override fun saveLanguage(language: AppLanguage) = Unit

    override fun openFaqEntry(entry: FaqEntry) = Unit

    /**
     * The card is remembered like everything else here: the flow reopens on what
     * was last submitted, so "Edit Card" is an edit rather than a fresh start.
     */
    override fun loadEmergencyCard(): EmergencyCardDraft? = emergencyCard

    override fun saveEmergencyCard(draft: EmergencyCardDraft) {
        emergencyCard = draft
    }

    /**
     * The three that leave the app. Writing to the gallery, opening a share sheet
     * and generating a PDF are all platform work with no UI
     * of their own, so they stop here alongside [shareApp] and [openSettings] and
     * wait for the real implementation.
     */
    override fun saveCardQr(draft: EmergencyCardDraft) = Unit

    override fun shareCardQr(draft: EmergencyCardDraft) = Unit

    override fun downloadCard(draft: EmergencyCardDraft, options: PrintOptions) = Unit

    override fun openSettings() = Unit

    // Production extras
    override fun triggerHelp() = Unit
    override fun updateHomeMode(mode: com.pukaar.app.ui.screen.home.HomeMode) = Unit
    override fun saveContact(draft: com.pukaar.app.ui.screen.contacts.ContactDraft) = Unit
    override fun openContact(contact: ContactUiModel) = Unit
    override fun startMockDrill(isSos: Boolean) = Unit
    override fun saveSosSettings(form: com.pukaar.app.ui.screen.sossettings.SosSettingsForm) = Unit
    override suspend fun loadSosSettings() = com.pukaar.app.ui.screen.sossettings.SosSettingsForm(
        autoCallEmergencyNumber = true,
        shareLocation = true,
        recordAudio = true,
        alertContacts = true
    )
    override fun saveElderlyHelp(
        window: com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow,
        medicationReminder: Boolean
    ) = Unit
    override suspend fun loadElderlyHelp() =
        com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow.SIX to false
    override fun saveEmergencyInfo(form: com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm) = Unit
    override suspend fun loadEmergencyInfo() =
        com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm()
    override fun viewPaymentHistory() = Unit
    override fun shareReferralCode() = Unit
    override suspend fun loadSubscriptionUi() = SubscriptionUi()
    override fun playIntroVideo() = Unit
    override fun playTopic(topic: com.pukaar.app.ui.screen.helpvideo.HelpTopic) = Unit
    override fun saveNotificationPreferences(
        preferences: com.pukaar.app.ui.screen.notifications.NotificationPreferences
    ) = Unit
    override suspend fun loadNotificationPreferences() =
        com.pukaar.app.ui.screen.notifications.NotificationPreferences(
            alertNotifications = true,
            inactivityAlerts = true,
            medicationReminders = false,
            promotions = false
        )
    override suspend fun loadLanguage(): AppLanguage = AppLanguage.ENGLISH
}

/** Which list a finished setup fills. */
private val ProtectionType.contactType: ContactType
    get() = when (this) {
        ProtectionType.SOS -> ContactType.SOS
        ProtectionType.INACTIVITY -> ContactType.INACTIVITY
    }

/**
 * SOS reaches everybody at once, so its contacts carry no priority; inactivity
 * works through its people in order, and the list is shown in that order. The
 * relation comes across in both cases — every setup asks for it, and the contacts
 * list is where it gets read back.
 */
private fun OnboardingContact.toUiModel(
    type: ContactType,
    priority: ContactPriority
) = ContactUiModel(
    id = id,
    name = name,
    phoneNumber = phone,
    type = type,
    relation = relation,
    priority = priority.takeIf { type != ContactType.SOS }
)

/** Something to look at before any setup has been walked through. */
private val SeedContacts = listOf(
    ContactUiModel(
        "seed-1", "Son", "+91 98765 43210", ContactType.SOS,
        relation = ContactRelation.CHILD
    ),
    ContactUiModel(
        "seed-2", "Spouse", "+91 98765 43211", ContactType.SOS,
        relation = ContactRelation.SPOUSE
    ),
    ContactUiModel(
        "seed-3", "Neighbour", "+91 98765 43212", ContactType.SOS,
        relation = ContactRelation.NEIGHBOUR
    ),

    ContactUiModel(
        "seed-7", "Sister", "+91 98765 43216", ContactType.INACTIVITY,
        relation = ContactRelation.SIBLING,
        priority = ContactPriority.PRIMARY
    ),
    ContactUiModel(
        "seed-8", "Caretaker", "+91 98765 43217", ContactType.INACTIVITY,
        relation = ContactRelation.OTHER,
        priority = ContactPriority.SECONDARY
    )
)

/** The same, for the numbers a trusted contact is handed during an alert. */
private val SeedPreSavedNumbers = listOf(
    PreSavedNumberUiModel(
        "seed-n1", "Family Doctor", "+91 98765 43220",
        ContactType.SOS, ContactRelation.OTHER
    ),
    PreSavedNumberUiModel(
        "seed-n2", "Brother", "+91 98765 43221",
        ContactType.SOS, ContactRelation.SIBLING
    ),

    PreSavedNumberUiModel(
        "seed-n3", "Family Doctor", "+91 98765 43222",
        ContactType.INACTIVITY, ContactRelation.OTHER
    ),
    PreSavedNumberUiModel(
        "seed-n4", "Uncle", "+91 98765 43223",
        ContactType.INACTIVITY, ContactRelation.RELATIVE
    ),
    PreSavedNumberUiModel(
        "seed-n5", "Neighbour", "+91 98765 43224",
        ContactType.INACTIVITY, ContactRelation.NEIGHBOUR
    )
)
