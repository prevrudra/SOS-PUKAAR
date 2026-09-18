package com.pukaar.app.ui.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.screen.onboarding.AccentButton
import com.pukaar.app.ui.screen.onboarding.HoursDropdown
import com.pukaar.app.ui.screen.onboarding.InactivityTiming
import com.pukaar.app.ui.screen.onboarding.OnboardingTextField
import com.pukaar.app.ui.screen.onboarding.RelationDropdown
import com.pukaar.app.ui.screen.onboarding.TextAction
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

/** The shortest string the edit dialog accepts as a phone number. */
private const val MinPhoneDigits = 6

/**
 * Menu item: the people an alert reaches.
 *
 * One section per category, in [ContactType] order, all on screen together — so
 * "who gets my SOS?" is answered by looking rather than by filtering. Each contact
 * belongs to exactly one category, so every name appears under a single heading.
 *
 * Each alert carries a second container: the pre-saved numbers its setup collects,
 * which are not alerted but are handed to the trusted contacts when one fires. The
 * inactivity section also carries its timings, editable here — they are the one
 * part of that setup a user is likely to want changed later without walking the
 * whole flow again.
 *
 * Every row can be edited or removed in place; the screen owns the dialogs and
 * hands finished changes upwards, so it never mutates the lists it was given.
 */
@Composable
fun ViewContactsScreen(
    contacts: List<ContactUiModel>,
    preSavedNumbers: List<PreSavedNumberUiModel>,
    inactivityTiming: InactivityTiming,
    onBack: () -> Unit,
    onSaveContact: (ContactUiModel) -> Unit,
    onDeleteContact: (ContactUiModel) -> Unit,
    onSavePreSavedNumber: (PreSavedNumberUiModel) -> Unit,
    onDeletePreSavedNumber: (PreSavedNumberUiModel) -> Unit,
    onSaveInactivityTiming: (InactivityTiming) -> Unit,
    onAddContact: (ContactType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // What a dialog is currently open on; null means no dialog.
    var editingContact by remember { mutableStateOf<ContactUiModel?>(null) }
    var removingContact by remember { mutableStateOf<ContactUiModel?>(null) }
    var editingNumber by remember { mutableStateOf<PreSavedNumberUiModel?>(null) }
    var removingNumber by remember { mutableStateOf<PreSavedNumberUiModel?>(null) }

    PukaarScreen(
        title = stringResource(R.string.view_contacts_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (contacts.isEmpty() && preSavedNumbers.isEmpty()) {
                EmptyState(stringResource(R.string.view_contacts_empty))
            }

            // Ritik layout: only SOS + Inactivity. Help/Doctor/Neighbour live as
            // pre-saved numbers under those sections — not as their own headings.
            listOf(ContactType.SOS, ContactType.INACTIVITY).forEach { type ->
                ServiceSection(
                    type = type,
                    contacts = contacts.filterByType(type).orderedByPriority(),
                    preSavedNumbers = preSavedNumbers.filterByAlert(type),
                    onEditContact = { editingContact = it },
                    onRemoveContact = { removingContact = it },
                    onEditNumber = { editingNumber = it },
                    onRemoveNumber = { removingNumber = it }
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AccentButton(
                        text = stringResource(
                            if (type == ContactType.SOS) R.string.view_contacts_add_sos
                            else R.string.view_contacts_add_inactivity
                        ),
                        onClick = { onAddContact(type) },
                        accent = type.accent
                    )
                    if (type == ContactType.INACTIVITY) {
                        TimingCard(
                            saved = inactivityTiming,
                            accent = type.accent,
                            onSave = onSaveInactivityTiming
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }

    editingContact?.let { contact ->
        val (dial, national) = remember(contact.phoneNumber) {
            runCatching {
                com.pukaar.app.util.PhoneNumbers.splitE164(contact.phoneNumber)
            }.getOrElse { "+91" to contact.phoneNumber.filter { it.isDigit() } }
        }
        EditEntryDialog(
            title = stringResource(R.string.view_contacts_edit_contact),
            name = contact.name,
            phone = national,
            relation = contact.relation,
            accent = contact.type.accent,
            onDismiss = { editingContact = null },
            onSave = { name, phone, relation ->
                val digits = phone.filter { it.isDigit() }
                val e164 = runCatching {
                    if (phone.trim().startsWith("+")) {
                        com.pukaar.app.util.PhoneNumbers.toE164(phone)
                    } else {
                        com.pukaar.app.util.PhoneNumbers.fromParts(dial, digits)
                    }
                }.getOrElse { phone }
                onSaveContact(
                    contact.copy(
                        name = name,
                        phoneNumber = e164,
                        relation = relation,
                        relationship = relation?.name
                            ?.lowercase()
                            ?.replaceFirstChar { it.titlecase() }
                            .orEmpty()
                    )
                )
                editingContact = null
            }
        )
    }

    editingNumber?.let { number ->
        val (dial, national) = remember(number.phoneNumber) {
            runCatching {
                com.pukaar.app.util.PhoneNumbers.splitE164(number.phoneNumber)
            }.getOrElse { "+91" to number.phoneNumber.filter { it.isDigit() } }
        }
        EditEntryDialog(
            title = stringResource(R.string.view_contacts_edit_number),
            name = number.name,
            phone = national,
            relation = number.relation,
            accent = number.type.accent,
            onDismiss = { editingNumber = null },
            onSave = { name, phone, relation ->
                val digits = phone.filter { it.isDigit() }
                val e164 = runCatching {
                    if (phone.trim().startsWith("+")) {
                        com.pukaar.app.util.PhoneNumbers.toE164(phone)
                    } else {
                        com.pukaar.app.util.PhoneNumbers.fromParts(dial, digits)
                    }
                }.getOrElse { phone }
                onSavePreSavedNumber(
                    number.copy(name = name, phoneNumber = e164, relation = relation)
                )
                editingNumber = null
            }
        )
    }

    removingContact?.let { contact ->
        ConfirmRemoveDialog(
            name = contact.name,
            body = stringResource(R.string.view_contacts_remove_contact_body),
            onDismiss = { removingContact = null },
            onConfirm = {
                onDeleteContact(contact)
                removingContact = null
            }
        )
    }

    removingNumber?.let { number ->
        ConfirmRemoveDialog(
            name = number.name,
            body = stringResource(R.string.view_contacts_remove_number_body),
            onDismiss = { removingNumber = null },
            onConfirm = {
                onDeletePreSavedNumber(number)
                removingNumber = null
            }
        )
    }
}

@Composable
private fun ServiceSection(
    type: ContactType,
    contacts: List<ContactUiModel>,
    preSavedNumbers: List<PreSavedNumberUiModel>,
    onEditContact: (ContactUiModel) -> Unit,
    onRemoveContact: (ContactUiModel) -> Unit,
    onEditNumber: (PreSavedNumberUiModel) -> Unit,
    onRemoveNumber: (PreSavedNumberUiModel) -> Unit,
    modifier: Modifier = Modifier,
    footer: @Composable () -> Unit = {}
) {
    Column(modifier = modifier) {
        SectionHeader(
            icon = type.icon,
            title = stringResource(type.sectionRes),
            tagline = stringResource(type.sectionTaglineRes),
            accent = type.accent
        )

        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            if (contacts.isEmpty()) {
                EmptyRow(stringResource(R.string.contact_section_empty))
            } else {
                contacts.forEachIndexed { index, contact ->
                    EntryRow(
                        name = contact.name,
                        phone = contact.phoneNumber,
                        accent = type.accent,
                        relation = contact.relation,
                        onEdit = { onEditContact(contact) },
                        onRemove = { onRemoveContact(contact) }
                    )
                    if (index != contacts.lastIndex) {
                        RowDivider()
                    }
                }
            }
        }

        // Not alerted themselves — the numbers a trusted contact is handed, so
        // they sit in their own container rather than among the people above.
        if (type.hasPreSavedNumbers) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(
                icon = Icons.Filled.PhoneForwarded,
                title = stringResource(R.string.contact_section_presaved),
                tagline = stringResource(R.string.contact_section_presaved_tagline),
                accent = type.accent
            )
            SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
                if (preSavedNumbers.isEmpty()) {
                    EmptyRow(stringResource(R.string.contact_section_presaved_empty))
                } else {
                    preSavedNumbers.forEachIndexed { index, number ->
                        EntryRow(
                            name = number.name,
                            phone = number.phoneNumber,
                            accent = type.accent,
                            relation = number.relation,
                            onEdit = { onEditNumber(number) },
                            onRemove = { onRemoveNumber(number) }
                        )
                        if (index != preSavedNumbers.lastIndex) {
                            RowDivider()
                        }
                    }
                }
            }
        }

        footer()
    }
}

/** A container's coloured title, with the line that says what it holds. */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    tagline: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = tagline,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(top = 3.dp, start = 24.dp)
        )
    }
}

/**
 * One row in either container: a name, a number, a [relation], and the two things
 * that can be done to it.
 *
 * No tag says how a person is reached — the container they sit in already answers
 * that. The relation is the one thing a row cannot say for itself, and it is what
 * tells a trusted contact reading the list who they are about to ring.
 */
@Composable
private fun EntryRow(
    name: String,
    phone: String,
    accent: Color,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    relation: ContactRelation? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar()
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (relation != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    ContactTag(text = stringResource(relation.labelRes))
                }
            }
            Text(text = phone, color = TextTertiary, fontSize = 11.sp)
        }

        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.view_contacts_edit),
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.view_contacts_delete),
                tint = PukaarRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** A pill beside a name — quiet by design, it labels rather than shouts. */
@Composable
private fun ContactTag(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .background(SurfaceElevated, shape)
            .border(1.dp, Outline, shape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/**
 * The name, number and relation of one row, editable in place.
 *
 * The draft lives here and nothing leaves until Save, so a half-typed number
 * never reaches the list. The relation is required, matching the setup flows —
 * a row saved here must not come out less complete than one added there.
 */
@Composable
private fun EditEntryDialog(
    title: String,
    name: String,
    phone: String,
    relation: ContactRelation?,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, relation: ContactRelation?) -> Unit
) {
    var draftName by remember { mutableStateOf(name) }
    var draftPhone by remember { mutableStateOf(phone) }
    var draftRelation by remember { mutableStateOf(relation) }

    val ready = draftName.isNotBlank() &&
        draftPhone.count { it.isDigit() } >= MinPhoneDigits &&
        draftRelation != null

    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, shape)
                .border(1.dp, Outline, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            OnboardingTextField(
                value = draftName,
                onValueChange = { draftName = it },
                placeholder = stringResource(R.string.add_contact_name_hint),
                accent = accent
            )
            OnboardingTextField(
                value = draftPhone,
                onValueChange = { draftPhone = it },
                placeholder = stringResource(R.string.add_contact_mobile_hint),
                accent = accent,
                keyboardType = KeyboardType.Phone
            )
            RelationDropdown(
                relation = draftRelation,
                onRelationChange = { draftRelation = it },
                accent = accent
            )
            AccentButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(draftName.trim(), draftPhone.trim(), draftRelation) },
                accent = accent,
                enabled = ready
            )
            TextAction(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    }
}

/** Removing somebody is not undoable here, so it is asked for twice. */
@Composable
private fun ConfirmRemoveDialog(
    name: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCard, shape)
                .border(1.dp, Outline, shape)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.view_contacts_remove_title, name),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = body, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
            AccentButton(
                text = stringResource(R.string.view_contacts_remove_confirm),
                onClick = onConfirm,
                accent = PukaarRed
            )
            TextAction(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    }
}

/**
 * The inactivity timings, editable in place.
 *
 * Held as a draft so a half-made change is never saved by accident: Save is the
 * only thing that reports it, and it stays disabled until something differs from
 * what came in.
 */
@Composable
private fun TimingCard(
    saved: InactivityTiming,
    accent: Color,
    onSave: (InactivityTiming) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keyed on what was passed in, so a save that comes back around resets the
    // draft rather than leaving the card looking dirty.
    var draft by remember(saved) { mutableStateOf(saved) }

    Column(
        modifier = modifier
            .padding(top = 10.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.view_contacts_timing_title),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.view_contacts_timing_subtitle),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 3.dp, bottom = 4.dp)
            )

            TimingRow(
                title = stringResource(R.string.onboarding_alert_soft),
                subtitle = stringResource(R.string.onboarding_alert_soft_note),
                hours = draft.softCheckHours,
                onHoursChange = { draft = draft.copy(softCheckHours = it) },
                accent = accent
            )
            TimingRow(
                title = stringResource(R.string.onboarding_alert_normal),
                subtitle = stringResource(R.string.onboarding_alert_normal_note),
                hours = draft.alertHours,
                onHoursChange = { draft = draft.copy(alertHours = it) },
                accent = accent
            )
            TimingRow(
                title = stringResource(R.string.onboarding_alert_high),
                subtitle = stringResource(R.string.onboarding_alert_high_note),
                hours = draft.highAlertHours,
                onHoursChange = { draft = draft.copy(highAlertHours = it) },
                accent = accent
            )
        }

        AccentButton(
            text = stringResource(R.string.view_contacts_timing_save),
            onClick = { onSave(draft) },
            accent = accent,
            enabled = draft != saved
        )
    }
}

@Composable
private fun TimingRow(
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
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 12.5.sp)
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.5.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        HoursDropdown(hours = hours, onHoursChange = onHoursChange, accent = accent)
    }
}

@Composable
private fun ContactAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(SurfaceElevated, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Groups,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun EmptyRow(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = TextTertiary,
        fontSize = 12.sp,
        modifier = modifier.padding(vertical = 14.dp)
    )
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 1400)
@Composable
private fun ViewContactsScreenPreview() {
    PukaarTheme {
        ViewContactsScreen(
            contacts = listOf(
                ContactUiModel(
                    "1", "Son", "+91 98765 43210", ContactType.SOS,
                    relation = ContactRelation.CHILD
                ),
                ContactUiModel(
                    "2", "Spouse", "+91 98765 43211", ContactType.SOS,
                    relation = ContactRelation.SPOUSE
                ),
                ContactUiModel(
                    "7", "Sister", "+91 98765 43216", ContactType.INACTIVITY,
                    relation = ContactRelation.SIBLING,
                    priority = ContactPriority.PRIMARY
                )
            ),
            preSavedNumbers = listOf(
                PreSavedNumberUiModel(
                    "n1", "Family Doctor", "+91 98765 43220",
                    ContactType.SOS, ContactRelation.OTHER
                ),
                PreSavedNumberUiModel(
                    "n2", "Neighbour", "+91 98765 43221",
                    ContactType.INACTIVITY, ContactRelation.NEIGHBOUR
                )
            ),
            inactivityTiming = InactivityTiming(),
            onBack = {},
            onSaveContact = {},
            onDeleteContact = {},
            onSavePreSavedNumber = {},
            onDeletePreSavedNumber = {},
            onSaveInactivityTiming = {}
        )
    }
}
