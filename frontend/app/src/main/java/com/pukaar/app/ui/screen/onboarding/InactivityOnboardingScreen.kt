package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.integration.ContactRepositoryBridge
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.util.PhoneNumbers
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary
import kotlinx.coroutines.launch

private const val InactivitySecondaryMax = 2
private const val InactivityMinNumbers = 1
private const val InactivityMaxNumbers = 2

/**
 * The pages, in order. Named rather than numbered because the flow is long
 * enough that `page == 6` stops meaning anything.
 */
private enum class InactivityPage {
    INTRO,
    TIMING,
    ADD_CONTACTS,
    VERIFY_PRIMARY,
    VERIFY_SECONDARY,
    CONTACTS_SUMMARY,
    ADD_NUMBERS,
    SELECT_NUMBER,
    NUMBERS_SUMMARY,
    COMPLETE;

    /**
     * Which of the four segments in the progress bar this page belongs to.
     * [INTRO] shows no bar at all, so its value is never read.
     */
    val stage: Int
        get() = when (this) {
            INTRO, TIMING -> 0
            ADD_CONTACTS, VERIFY_PRIMARY, VERIFY_SECONDARY, CONTACTS_SUMMARY -> 1
            ADD_NUMBERS, SELECT_NUMBER, NUMBERS_SUMMARY -> 2
            COMPLETE -> 3
        }
}

private const val InactivityStages = 4

/**
 * The inactivity setup: when to start worrying, who to tell, and which numbers
 * they can ring.
 *
 * Secondary contacts and their verification are genuinely optional here — the
 * flow lets the user walk past both — but a verified primary is what the whole
 * feature rests on, so that page is the one that holds the line.
 */
@Composable
fun InactivityOnboardingScreen(
    onBack: () -> Unit,
    onFinished: (OnboardingResult) -> Unit,
    onShareApp: (phoneE164: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val type = ProtectionType.INACTIVITY
    val accent = type.accent
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var page by remember { mutableStateOf(InactivityPage.INTRO) }
    var timing by remember { mutableStateOf(InactivityTiming()) }
    val primary = rememberContactRoster(max = 1)
    val secondary = rememberContactRoster(max = InactivitySecondaryMax)
    val numbers = rememberHelpNumbers(max = InactivityMaxNumbers)

    // Which of the three number slots [InactivityPage.SELECT_NUMBER] is filling.
    var editingSlot by remember { mutableIntStateOf(0) }
    var editingId by remember { mutableStateOf<String?>(null) }

    val takenPhones = (primary.contacts + secondary.contacts).map { it.phone }

    suspend fun persistTrustedForOtp(roster: ContactRoster, contact: OnboardingContact): OnboardingContact {
        val e164 = runCatching { PhoneNumbers.toE164(contact.phone) }.getOrElse {
            val digits = contact.phone.filter { it.isDigit() }
            PhoneNumbers.fromParts("+91", digits.takeLast(10))
        }
        val (dial, national) = PhoneNumbers.splitE164(e164)
        val existing = ContactRepositoryBridge.loadContacts()
        val existingId = existing.firstOrNull {
            it.type == ContactType.INACTIVITY &&
                ContactRepositoryBridge.phonesMatch(it.phoneNumber, e164)
        }?.id ?: existing.firstOrNull { it.id == contact.id }?.id
        val draft = ContactDraft(
            id = existingId,
            name = contact.name.trim(),
            mobile = national,
            dialCode = dial,
            relationship = contact.relation?.name
                ?.lowercase()
                ?.replaceFirstChar { it.titlecase() }
                .orEmpty(),
            type = ContactType.INACTIVITY,
            priorityOrder = 1
        )
        val savedId = ContactRepositoryBridge.saveQuiet(draft).getOrThrow()
        val updated = contact.copy(id = savedId)
        roster.replace(contact.id, updated)
        return updated
    }

    // The order is written out rather than derived from the enum: the number
    // picker sits between the slots and the summary in the enum, but the flow
    // only visits it when a slot is tapped.
    fun advance() {
        when (page) {
            InactivityPage.INTRO -> page = InactivityPage.TIMING
            InactivityPage.TIMING -> page = InactivityPage.ADD_CONTACTS
            InactivityPage.ADD_CONTACTS -> {
                // Persist contacts so the server can send OTPs, then verify.
                scope.launch {
                    try {
                        primary.contacts.toList().forEach { persistTrustedForOtp(primary, it) }
                        secondary.contacts.toList().forEach { persistTrustedForOtp(secondary, it) }
                        page = InactivityPage.VERIFY_PRIMARY
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            e.message ?: "Could not save contacts",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            InactivityPage.VERIFY_PRIMARY -> page = InactivityPage.VERIFY_SECONDARY
            InactivityPage.VERIFY_SECONDARY -> page = InactivityPage.CONTACTS_SUMMARY
            InactivityPage.CONTACTS_SUMMARY -> page = InactivityPage.ADD_NUMBERS
            InactivityPage.ADD_NUMBERS -> page = InactivityPage.NUMBERS_SUMMARY
            InactivityPage.SELECT_NUMBER -> page = InactivityPage.ADD_NUMBERS
            InactivityPage.NUMBERS_SUMMARY -> page = InactivityPage.COMPLETE
            InactivityPage.COMPLETE -> Unit
        }
    }

    fun goBack() {
        page = when (page) {
            InactivityPage.INTRO -> return onBack()
            InactivityPage.TIMING -> InactivityPage.INTRO
            InactivityPage.ADD_CONTACTS -> InactivityPage.TIMING
            InactivityPage.VERIFY_PRIMARY -> InactivityPage.ADD_CONTACTS
            InactivityPage.VERIFY_SECONDARY -> InactivityPage.VERIFY_PRIMARY
            InactivityPage.CONTACTS_SUMMARY -> InactivityPage.VERIFY_SECONDARY
            InactivityPage.ADD_NUMBERS -> InactivityPage.CONTACTS_SUMMARY
            // Backing out of the picker returns to the slots it was opened from.
            InactivityPage.SELECT_NUMBER -> InactivityPage.ADD_NUMBERS
            InactivityPage.NUMBERS_SUMMARY -> InactivityPage.ADD_NUMBERS
            InactivityPage.COMPLETE -> InactivityPage.NUMBERS_SUMMARY
        }
    }

    OnboardingScaffold(
        onBack = ::goBack,
        accent = accent,
        modifier = modifier,
        progressSteps = if (page == InactivityPage.INTRO) 0 else InactivityStages,
        progressCurrent = page.stage,
        footer = {
            InactivityFooter(
                page = page,
                accent = accent,
                primaryVerified = primary.allVerified,
                contactsAdded = primary.contacts.isNotEmpty(),
                numberCount = numbers.numbers.size,
                onAdvance = ::advance,
                onSkipVerification = { page = InactivityPage.VERIFY_SECONDARY },
                onFinished = {
                    onFinished(
                        OnboardingResult(
                            type = type,
                            primaryContacts = primary.contacts,
                            secondaryContacts = secondary.contacts,
                            helpNumbers = numbers.numbers,
                            timing = timing
                        )
                    )
                }
            )
        }
    ) {
        when (page) {
            InactivityPage.INTRO -> IntroPage(type = type)

            InactivityPage.TIMING -> TimingPage(
                timing = timing,
                onTimingChange = { timing = it },
                accent = accent
            )

            InactivityPage.ADD_CONTACTS -> AddContactsPage(
                primary = primary,
                secondary = secondary,
                accent = accent,
                takenPhones = takenPhones
            )

            InactivityPage.VERIFY_PRIMARY -> VerifyPrimaryPage(
                primary = primary,
                type = type,
                onShareApp = onShareApp,
                onVerify = { contact, code ->
                    scope.launch {
                        ContactRepositoryBridge.verifyContact(contact.id, code)
                            .onSuccess {
                                primary.verify(contact.id)
                                onShareApp(contact.phone)
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Verification failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onResend = { contact ->
                    scope.launch {
                        val ui = com.pukaar.app.ui.screen.contacts.ContactUiModel(
                            id = contact.id,
                            name = contact.name,
                            phoneNumber = contact.phone,
                            type = ContactType.INACTIVITY,
                            relation = contact.relation,
                            relationship = contact.relation?.name.orEmpty(),
                            verified = false
                        )
                        val name = runCatching {
                            PukaarApp.instance.repository.me().fullName
                        }.getOrNull()
                        ContactRepositoryBridge.resendVerification(context, ui, name)
                            .onSuccess { primary.resend(contact.id) }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Could not resend code",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
            )

            InactivityPage.VERIFY_SECONDARY -> VerifySecondaryPage(
                secondary = secondary,
                type = type,
                takenPhones = takenPhones,
                onShareApp = onShareApp,
                onVerify = { contact, code ->
                    scope.launch {
                        var current = contact
                        if (ContactRepositoryBridge.loadContacts().none { it.id == contact.id }) {
                            current = persistTrustedForOtp(secondary, contact)
                        }
                        ContactRepositoryBridge.verifyContact(current.id, code)
                            .onSuccess {
                                secondary.verify(current.id)
                                onShareApp(current.phone)
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Verification failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                },
                onResend = { contact ->
                    scope.launch {
                        var current = contact
                        if (ContactRepositoryBridge.loadContacts().none { it.id == contact.id }) {
                            current = persistTrustedForOtp(secondary, contact)
                        }
                        val ui = com.pukaar.app.ui.screen.contacts.ContactUiModel(
                            id = current.id,
                            name = current.name,
                            phoneNumber = current.phone,
                            type = ContactType.INACTIVITY,
                            relation = current.relation,
                            relationship = current.relation?.name.orEmpty(),
                            verified = false
                        )
                        val name = runCatching {
                            PukaarApp.instance.repository.me().fullName
                        }.getOrNull()
                        ContactRepositoryBridge.resendVerification(context, ui, name)
                            .onSuccess { secondary.resend(current.id) }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    it.message ?: "Could not resend code",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }
            )

            InactivityPage.CONTACTS_SUMMARY -> ContactsSummaryPage(
                primary = primary,
                secondary = secondary
            )

            InactivityPage.ADD_NUMBERS -> AddNumbersPage(
                numbers = numbers,
                accent = accent,
                onSlotClick = { slot, existingId ->
                    editingSlot = slot
                    editingId = existingId
                    page = InactivityPage.SELECT_NUMBER
                }
            )

            InactivityPage.SELECT_NUMBER -> SelectHelpNumberPage(
                slot = editingSlot,
                accent = accent,
                takenPhones = numbers.numbers
                    .filterNot { it.id == editingId }
                    .map { it.phone },
                onSaved = { picked ->
                    val entry = HelpNumber(picked.id, picked.name, picked.phone, picked.relation)
                    val replacing = editingId
                    if (replacing == null) numbers.add(entry) else numbers.replace(replacing, entry)
                    page = InactivityPage.ADD_NUMBERS
                }
            )

            InactivityPage.NUMBERS_SUMMARY -> NumbersSummaryPage(
                numbers = numbers,
                accent = accent,
                onEdit = { number ->
                    editingSlot = numbers.numbers.indexOf(number)
                    editingId = number.id
                    page = InactivityPage.SELECT_NUMBER
                }
            )

            InactivityPage.COMPLETE -> CompletePage(
                timing = timing,
                primaryCount = primary.contacts.size,
                secondaryCount = secondary.contacts.size,
                numberCount = numbers.numbers.size,
                accent = accent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/** The footer is the only thing that differs page to page, so it lives apart. */
@Composable
private fun InactivityFooter(
    page: InactivityPage,
    accent: Color,
    primaryVerified: Boolean,
    contactsAdded: Boolean,
    numberCount: Int,
    onAdvance: () -> Unit,
    onSkipVerification: () -> Unit,
    onFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (page) {
            InactivityPage.INTRO -> AccentButton(
                text = stringResource(R.string.onboarding_get_started),
                onClick = onAdvance,
                accent = accent
            )

            InactivityPage.ADD_CONTACTS -> AccentButton(
                text = stringResource(R.string.action_continue),
                onClick = onAdvance,
                accent = accent,
                enabled = contactsAdded
            )

            InactivityPage.VERIFY_PRIMARY -> {
                AccentButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onAdvance,
                    accent = accent,
                    enabled = primaryVerified
                )
                TextAction(
                    text = stringResource(R.string.onboarding_do_this_later),
                    onClick = onSkipVerification
                )
            }

            InactivityPage.ADD_NUMBERS, InactivityPage.NUMBERS_SUMMARY -> AccentButton(
                text = stringResource(R.string.action_continue),
                onClick = onAdvance,
                accent = accent,
                enabled = numberCount >= InactivityMinNumbers
            )

            // Saving is the picker's own button, inside the page.
            InactivityPage.SELECT_NUMBER -> Unit

            InactivityPage.COMPLETE -> {
                AccentButton(
                    text = stringResource(R.string.action_done),
                    onClick = onFinished,
                    accent = accent
                )
                TextAction(
                    text = stringResource(R.string.onboarding_go_to_dashboard),
                    onClick = onFinished,
                    color = accent
                )
            }

            else -> AccentButton(
                text = stringResource(R.string.action_continue),
                onClick = onAdvance,
                accent = accent
            )
        }
    }
}

@Composable
private fun ColumnScope.IntroPage(type: ProtectionType) {
    Spacer(modifier = Modifier.height(18.dp))
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(70.dp)
            .border(2.dp, type.accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = type.accent,
            modifier = Modifier.size(36.dp)
        )
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = stringResource(type.labelRes),
        color = type.accent,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Text(
        text = stringResource(type.taglineRes),
        color = TextPrimary,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.onboarding_inactivity_intro),
        color = TextSecondary,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))
    val steps = listOf(
        stringResource(R.string.onboarding_inactivity_step1) to
            stringResource(R.string.onboarding_inactivity_step1_note),
        stringResource(R.string.onboarding_inactivity_step2) to
            stringResource(R.string.onboarding_inactivity_step2_note),
        stringResource(R.string.onboarding_inactivity_step3) to
            stringResource(R.string.onboarding_inactivity_step3_note),
        stringResource(R.string.onboarding_inactivity_step4) to
            stringResource(R.string.onboarding_inactivity_step4_note)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        steps.forEachIndexed { index, (title, note) ->
            NumberedHeading(
                number = index + 1,
                title = title,
                accent = type.accent,
                subtitle = note
            )
        }
    }
}

@Composable
private fun ColumnScope.TimingPage(
    timing: InactivityTiming,
    onTimingChange: (InactivityTiming) -> Unit,
    accent: Color
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_timing_title),
        subtitle = stringResource(R.string.onboarding_timing_subtitle)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AlertTimingRow(
            title = stringResource(R.string.onboarding_alert_duration),
            subtitle = stringResource(R.string.onboarding_alert_duration_note),
            hours = timing.durationHours,
            onHoursChange = { onTimingChange(InactivityTiming(durationHours = it)) },
            accent = accent
        )
    }

    InfoBox(
        title = stringResource(R.string.onboarding_timing_info_title),
        body = stringResource(R.string.onboarding_timing_info_body)
    )
}

@Composable
private fun AlertTimingRow(
    title: String,
    subtitle: String,
    hours: Int,
    onHoursChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        HoursDropdown(hours = hours, onHoursChange = onHoursChange, accent = accent)
    }
}

@Composable
private fun ColumnScope.AddContactsPage(
    primary: ContactRoster,
    secondary: ContactRoster,
    accent: Color,
    takenPhones: List<String>
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_add_trusted_contacts),
        subtitle = stringResource(R.string.onboarding_inactivity_contacts_subtitle)
    )
    RequirementChips(
        labels = listOf(
            stringResource(R.string.onboarding_primary_required),
            stringResource(R.string.onboarding_secondary_optional, InactivitySecondaryMax)
        )
    )

    NumberedHeading(
        number = 1,
        title = stringResource(R.string.onboarding_primary_contact),
        accent = accent,
        suffix = stringResource(R.string.onboarding_required),
        subtitle = stringResource(R.string.onboarding_primary_always_notified)
    )
    primary.contacts.forEach { contact ->
        AddedContactRow(
            contact = contact,
            accent = accent,
            onRemove = { primary.remove(contact.id) }
        )
    }
    if (!primary.isFull) {
        ContactAdder(
            label = stringResource(R.string.onboarding_add_primary_contact),
            confirmLabel = stringResource(R.string.onboarding_add_contact),
            accent = accent,
            takenPhones = takenPhones,
            onAdded = primary::add
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
    NumberedHeading(
        number = 2,
        title = stringResource(R.string.onboarding_secondary_contacts),
        accent = accent,
        suffix = stringResource(R.string.onboarding_optional),
        subtitle = stringResource(R.string.onboarding_secondary_contacts_note)
    )
    secondary.contacts.forEach { contact ->
        AddedContactRow(
            contact = contact,
            accent = accent,
            onRemove = { secondary.remove(contact.id) }
        )
    }
    if (!secondary.isFull) {
        ContactAdder(
            label = stringResource(
                R.string.onboarding_add_secondary_contact,
                secondary.contacts.size + 1
            ),
            confirmLabel = stringResource(R.string.onboarding_add_contact),
            accent = accent,
            takenPhones = takenPhones,
            onAdded = secondary::add
        )
    }
}

@Composable
private fun ColumnScope.VerifyPrimaryPage(
    primary: ContactRoster,
    type: ProtectionType,
    onShareApp: (phoneE164: String?) -> Unit,
    onVerify: (OnboardingContact, String) -> Unit,
    onResend: (OnboardingContact) -> Unit
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_verify_primary),
        subtitle = stringResource(R.string.onboarding_verify_primary_subtitle)
    )
    SummaryHeading(text = stringResource(R.string.onboarding_primary_contact))
    primary.contacts.forEach { contact ->
        ContactStatusCard(
            contact = contact,
            accent = type.accent,
            onResend = { onResend(contact) },
            onVerify = { code -> onVerify(contact, code) }
        )
    }

    PukaarAlertShareCard(type = type, onShareApp = { onShareApp(null) })

    InfoBox(
        title = stringResource(R.string.onboarding_how_it_works),
        body = stringResource(R.string.onboarding_how_it_works_inactivity)
    )
}

@Composable
private fun ColumnScope.VerifySecondaryPage(
    secondary: ContactRoster,
    type: ProtectionType,
    takenPhones: List<String>,
    onShareApp: (phoneE164: String?) -> Unit,
    onVerify: (OnboardingContact, String) -> Unit,
    onResend: (OnboardingContact) -> Unit
) {
    val accent = type.accent

    FlowTitle(
        title = stringResource(R.string.onboarding_verify_secondary),
        suffix = stringResource(R.string.onboarding_optional),
        subtitle = stringResource(R.string.onboarding_verify_secondary_subtitle, InactivitySecondaryMax)
    )

    if (secondary.contacts.isEmpty()) {
        Text(
            text = stringResource(R.string.onboarding_no_secondary_yet),
            color = TextTertiary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )
    }
    secondary.contacts.forEachIndexed { index, contact ->
        SummaryHeading(
            text = stringResource(R.string.onboarding_secondary_contact_n, index + 1)
        )
        ContactStatusCard(
            contact = contact,
            accent = accent,
            onResend = { onResend(contact) },
            onVerify = { code -> onVerify(contact, code) },
            onDelete = { secondary.remove(contact.id) }
        )
    }

    if (!secondary.isFull) {
        Spacer(modifier = Modifier.height(4.dp))
        ContactAdder(
            label = stringResource(R.string.onboarding_add_another_contact),
            confirmLabel = stringResource(R.string.onboarding_add_contact),
            accent = accent,
            takenPhones = takenPhones,
            onAdded = secondary::add
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
    PukaarAlertShareCard(type = type, onShareApp = { onShareApp(null) })
}

@Composable
private fun ColumnScope.ContactsSummaryPage(
    primary: ContactRoster,
    secondary: ContactRoster
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_all_verified),
        subtitle = stringResource(R.string.onboarding_all_verified_subtitle)
    )
    SummaryHeading(text = stringResource(R.string.onboarding_primary_contact))
    primary.contacts.forEach { VerifiedContactRow(contact = it) }

    secondary.contacts.forEachIndexed { index, contact ->
        SummaryHeading(
            text = stringResource(R.string.onboarding_secondary_contact_n, index + 1)
        )
        VerifiedContactRow(contact = contact)
    }
}

@Composable
private fun ColumnScope.AddNumbersPage(
    numbers: HelpNumberList,
    accent: Color,
    onSlotClick: (slot: Int, existingId: String?) -> Unit
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_add_presaved_numbers),
        subtitle = stringResource(R.string.onboarding_presaved_numbers_subtitle)
    )
    RequirementChips(
        labels = listOf(
            stringResource(R.string.onboarding_min_numbers, InactivityMinNumbers),
            stringResource(R.string.onboarding_max_numbers, InactivityMaxNumbers)
        )
    )

    NumberedHeading(
        number = 3,
        title = stringResource(R.string.onboarding_add_help_numbers),
        accent = accent,
        subtitle = stringResource(R.string.onboarding_add_help_numbers_note, InactivityMaxNumbers)
    )

    HelpNumberSlots(
        numbers = numbers,
        accent = accent,
        min = InactivityMinNumbers,
        max = InactivityMaxNumbers,
        onSlotClick = onSlotClick
    )

    Spacer(modifier = Modifier.height(4.dp))
    InfoBox(
        title = stringResource(R.string.onboarding_important),
        body = stringResource(R.string.onboarding_help_numbers_important)
    )
}

@Composable
private fun ColumnScope.NumbersSummaryPage(
    numbers: HelpNumberList,
    accent: Color,
    onEdit: (HelpNumber) -> Unit
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_numbers_added),
        subtitle = stringResource(R.string.onboarding_numbers_added_subtitle)
    )
    numbers.numbers.forEachIndexed { index, number ->
        HelpNumberRow(
            index = index + 1,
            number = number,
            accent = accent,
            optional = index >= InactivityMinNumbers,
            onEdit = { onEdit(number) },
            onDelete = { numbers.remove(number.id) }
        )
    }
}

@Composable
private fun ColumnScope.CompletePage(
    timing: InactivityTiming,
    primaryCount: Int,
    secondaryCount: Int,
    numberCount: Int,
    accent: Color
) {
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(96.dp)
            .border(3.dp, accent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(48.dp)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    FlowTitle(
        title = stringResource(R.string.onboarding_inactivity_complete),
        subtitle = stringResource(R.string.onboarding_inactivity_complete_subtitle)
    )

    Spacer(modifier = Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        SummaryLine(
            label = stringResource(R.string.onboarding_alert_duration),
            value = stringResource(R.string.onboarding_hours_value, timing.durationHours)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SummaryLine(
            label = stringResource(R.string.onboarding_primary_contact),
            value = stringResource(R.string.onboarding_added_count, primaryCount)
        )
        SummaryLine(
            label = stringResource(R.string.onboarding_secondary_contacts),
            value = stringResource(R.string.onboarding_added_count, secondaryCount)
        )
        SummaryLine(
            label = stringResource(R.string.onboarding_help_numbers),
            value = stringResource(R.string.onboarding_added_count, numberCount)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1100)
@Composable
private fun InactivityOnboardingScreenPreview() {
    PukaarTheme {
        InactivityOnboardingScreen(onBack = {}, onFinished = {}, onShareApp = { _ -> })
    }
}
