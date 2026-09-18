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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

private const val SosMinContacts = 2
private const val SosMaxContacts = 3
private const val SosMinNumbers = 1
private const val SosMaxNumbers = 3

/**
 * The pages of the SOS setup, in order.
 *
 * Named rather than numbered: [SELECT_NUMBER] is reached only by tapping a slot
 * on [ADD_NUMBERS] and returns to it, so the run is no longer a straight count.
 */
private enum class SosPage {
    ADD_CONTACTS,
    VERIFY,
    ADD_NUMBERS,
    SELECT_NUMBER,
    COMPLETE;

    /** Which dot is lit. The number picker belongs to the page that opened it. */
    val dot: Int
        get() = when (this) {
            ADD_CONTACTS -> 0
            VERIFY -> 1
            ADD_NUMBERS, SELECT_NUMBER -> 2
            COMPLETE -> 3
        }
}

private const val SosStepCount = 4

/**
 * The SOS setup: pick trusted contacts, send them a code, watch it come back,
 * then leave them a list of numbers they can ring.
 *
 * The pre-saved numbers matter as much here as they do for inactivity, and for
 * the same reason: the alert reaches a person, not a service, and that person is
 * often standing somewhere unable to do anything except make a call. Who they
 * should call is not something to work out afterwards.
 *
 * Every page shares one set of contacts and one list of numbers held here, so
 * going back a step never loses what was already entered. Nothing leaves the
 * screen until [onFinished].
 */
@Composable
fun SosOnboardingScreen(
    onBack: () -> Unit,
    onFinished: (OnboardingResult) -> Unit,
    onShareApp: (phoneE164: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val type = ProtectionType.SOS
    val accent = type.accent

    var page by remember { mutableStateOf(SosPage.ADD_CONTACTS) }
    val slots = rememberContactSlots(SosMaxContacts)
    val numbers = rememberHelpNumbers(max = SosMaxNumbers)

    // Which of the number slots [SosPage.SELECT_NUMBER] is filling, and the entry
    // it is replacing when that slot already held one.
    var editingSlot by remember { mutableIntStateOf(0) }
    var editingId by remember { mutableStateOf<String?>(null) }

    val filled = slots.filled
    val allVerified = filled.isNotEmpty() && filled.all { it.verified }

    fun goBack() {
        page = when (page) {
            SosPage.ADD_CONTACTS -> return onBack()
            SosPage.VERIFY -> SosPage.ADD_CONTACTS
            SosPage.ADD_NUMBERS -> SosPage.VERIFY
            // Backing out of the picker returns to the slots it was opened from.
            SosPage.SELECT_NUMBER -> SosPage.ADD_NUMBERS
            SosPage.COMPLETE -> SosPage.ADD_NUMBERS
        }
    }

    OnboardingScaffold(
        onBack = ::goBack,
        accent = accent,
        modifier = modifier,
        footer = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (page) {
                    SosPage.ADD_CONTACTS -> {
                        AccentButton(
                            text = stringResource(R.string.action_continue),
                            onClick = {
                                // Local verify is enough for the UI; server marks verified on save.
                                filled.forEach { slots.verify(it.id) }
                                page = SosPage.ADD_NUMBERS
                            },
                            accent = accent,
                            enabled = filled.size >= SosMinContacts
                        )
                        SecureFootnote()
                    }

                    SosPage.VERIFY -> AccentButton(
                        text = stringResource(R.string.action_continue),
                        onClick = { page = SosPage.ADD_NUMBERS },
                        accent = accent,
                        enabled = allVerified
                    )

                    SosPage.ADD_NUMBERS -> AccentButton(
                        text = stringResource(R.string.action_continue),
                        onClick = { page = SosPage.COMPLETE },
                        accent = accent,
                        enabled = numbers.numbers.size >= SosMinNumbers
                    )

                    // Saving is the picker's own button, inside the page.
                    SosPage.SELECT_NUMBER -> Unit

                    SosPage.COMPLETE -> {
                        var saving by remember { mutableStateOf(false) }
                        AccentButton(
                            text = if (saving) {
                                stringResource(R.string.action_saving)
                            } else {
                                stringResource(R.string.action_done)
                            },
                            onClick = {
                                if (saving) return@AccentButton
                                saving = true
                                onFinished(
                                    OnboardingResult(
                                        type = type,
                                        primaryContacts = filled,
                                        helpNumbers = numbers.numbers
                                    )
                                )
                            },
                            accent = accent,
                            enabled = !saving,
                            leadingIcon = Icons.Filled.Check
                        )
                        Text(
                            text = stringResource(R.string.onboarding_sos_complete_note),
                            color = TextTertiary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                StepDots(total = SosStepCount, current = page.dot, accent = accent)
            }
        }
    ) {
        FlowBrandHeader(type = type)
        Spacer(modifier = Modifier.height(6.dp))

        when (page) {
            SosPage.ADD_CONTACTS -> SosAddContactsStep(slots = slots, accent = accent)

            SosPage.VERIFY -> SosVerifyStep(
                contacts = filled,
                type = type,
                onResend = { id -> slots.resend(id) },
                onVerify = { id -> slots.verify(id) },
                onShareApp = onShareApp
            )

            SosPage.ADD_NUMBERS -> SosAddNumbersStep(
                numbers = numbers,
                accent = accent,
                onSlotClick = { slot, existingId ->
                    editingSlot = slot
                    editingId = existingId
                    page = SosPage.SELECT_NUMBER
                }
            )

            SosPage.SELECT_NUMBER -> SelectHelpNumberPage(
                slot = editingSlot,
                accent = accent,
                takenPhones = numbers.numbers
                    .filterNot { it.id == editingId }
                    .map { it.phone },
                onSaved = { picked ->
                    val entry = HelpNumber(picked.id, picked.name, picked.phone, picked.relation)
                    val replacing = editingId
                    if (replacing == null) numbers.add(entry) else numbers.replace(replacing, entry)
                    page = SosPage.ADD_NUMBERS
                }
            )

            SosPage.COMPLETE -> SosCompleteStep(
                contacts = filled,
                numbers = numbers.numbers,
                accent = accent
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ColumnScope.SosAddContactsStep(
    slots: ContactSlots,
    accent: Color
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_add_trusted_contacts),
        subtitle = stringResource(R.string.onboarding_sos_contacts_subtitle)
    )
    RequirementChips(
        labels = listOf(
            stringResource(R.string.onboarding_min_contacts, SosMinContacts),
            stringResource(R.string.onboarding_max_contacts, SosMaxContacts)
        )
    )
    Spacer(modifier = Modifier.height(2.dp))

    for (index in 0 until slots.size) {
        val optional = index >= SosMinContacts
        Text(
            text = stringResource(R.string.onboarding_add_contact_n, index + 1),
            color = TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
        ContactSlotCard(
            contact = slots[index],
            optional = optional,
            accent = accent,
            takenPhones = slots.filled.map { it.phone },
            onPicked = { slots.set(index, it) },
            onCleared = { slots.set(index, null) }
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
    ImportantNote(
        title = stringResource(R.string.onboarding_important),
        body = stringResource(R.string.onboarding_sos_important_body)
    )
}

@Composable
private fun ColumnScope.SosVerifyStep(
    contacts: List<OnboardingContact>,
    type: ProtectionType,
    onResend: (String) -> Unit,
    onVerify: (String) -> Unit,
    onShareApp: (phoneE164: String?) -> Unit
) {
    val accent = type.accent

    FlowTitle(
        title = stringResource(R.string.onboarding_verify_trusted_contacts),
        subtitle = stringResource(R.string.onboarding_verify_subtitle)
    )
    RequirementChips(
        labels = listOf(
            stringResource(R.string.onboarding_min_contacts, SosMinContacts),
            stringResource(R.string.onboarding_max_contacts, SosMaxContacts)
        )
    )

    contacts.forEachIndexed { index, contact ->
        Text(
            text = stringResource(R.string.onboarding_contact_n, index + 1),
            color = TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
        ContactStatusCard(
            contact = contact,
            accent = accent,
            onResend = { onResend(contact.id) },
            onVerify = { onVerify(contact.id); onShareApp(contact.phone) }
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
    PukaarAlertShareCard(type = type, onShareApp = { onShareApp(null) })

    Spacer(modifier = Modifier.height(4.dp))
    InfoBox(
        title = stringResource(R.string.onboarding_how_it_works),
        body = stringResource(R.string.onboarding_how_it_works_sos)
    )
}

/**
 * The numbers handed to whoever answers an SOS.
 *
 * The same slots the inactivity setup uses, with the wording that fits this
 * alert: these people are not alerted themselves, they are who a trusted contact
 * rings once they know something is wrong.
 */
@Composable
private fun ColumnScope.SosAddNumbersStep(
    numbers: HelpNumberList,
    accent: Color,
    onSlotClick: (slot: Int, existingId: String?) -> Unit
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_add_presaved_numbers),
        subtitle = stringResource(R.string.onboarding_sos_presaved_subtitle)
    )
    RequirementChips(
        labels = listOf(
            stringResource(R.string.onboarding_min_numbers, SosMinNumbers),
            stringResource(R.string.onboarding_max_numbers, SosMaxNumbers)
        )
    )

    NumberedHeading(
        number = 3,
        title = stringResource(R.string.onboarding_add_help_numbers),
        accent = accent,
        subtitle = stringResource(R.string.onboarding_add_help_numbers_note, SosMaxNumbers)
    )

    HelpNumberSlots(
        numbers = numbers,
        accent = accent,
        min = SosMinNumbers,
        max = SosMaxNumbers,
        onSlotClick = onSlotClick
    )

    Spacer(modifier = Modifier.height(4.dp))
    InfoBox(
        title = stringResource(R.string.onboarding_important),
        body = stringResource(R.string.onboarding_sos_numbers_important)
    )
}

@Composable
private fun ColumnScope.SosCompleteStep(
    contacts: List<OnboardingContact>,
    numbers: List<HelpNumber>,
    accent: Color
) {
    FlowTitle(
        title = stringResource(R.string.onboarding_contacts_verified),
        subtitle = stringResource(R.string.onboarding_contacts_verified_subtitle)
    )

    contacts.forEachIndexed { index, contact ->
        Text(
            text = stringResource(R.string.onboarding_contact_n, index + 1),
            color = TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp)
        )
        VerifiedContactRow(contact = contact)
    }

    if (numbers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        SummaryHeading(text = stringResource(R.string.onboarding_help_numbers))
        numbers.forEachIndexed { index, number ->
            HelpNumberRow(
                index = index + 1,
                number = number,
                accent = accent,
                optional = index >= SosMinNumbers
            )
        }
    }
}

/**
 * One numbered slot on the add-contacts page.
 *
 * Empty, it offers the two ways of naming somebody; filled, it collapses to the
 * chosen person with a control to clear it again.
 */
@Composable
fun ContactSlotCard(
    contact: OnboardingContact?,
    optional: Boolean,
    accent: Color,
    takenPhones: List<String>,
    onPicked: (OnboardingContact) -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, shape)
            .border(1.dp, Outline, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (contact != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ContactAvatar(name = contact.name, size = 34)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ContactNameLine(contact = contact)
                    Text(text = contact.phone, color = TextSecondary, fontSize = 11.5.sp)
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(accent, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(onClick = onCleared, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.onboarding_remove),
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            ContactPicker(
                accent = accent,
                takenPhones = takenPhones,
                confirmLabel = stringResource(R.string.onboarding_add_contact),
                onPicked = onPicked
            )
            if (optional) {
                Text(
                    text = stringResource(R.string.onboarding_optional_slot),
                    color = TextTertiary,
                    fontSize = 10.5.sp
                )
            }
        }
    }
}

/** The secure-storage line under the send button. */
@Composable
fun SecureFootnote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.onboarding_secure_footnote),
            color = TextTertiary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1100)
@Composable
private fun SosOnboardingScreenPreview() {
    PukaarTheme {
        SosOnboardingScreen(onBack = {}, onFinished = {}, onShareApp = { _ -> })
    }
}
