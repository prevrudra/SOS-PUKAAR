package com.pukaar.app.ui.screen.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.FieldLabel
import com.pukaar.app.ui.component.LabeledTextField
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SecondaryButton
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.SelectionRow
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.TextTertiary

private val RELATIONSHIP_SUGGESTIONS = listOf(
    "Son", "Daughter", "Spouse", "Brother", "Sister",
    "Father", "Mother", "Friend", "Neighbour", "Doctor", "Caretaker"
)

/**
 * Add or edit a contact — same form for both, with all fields editable.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactFormScreen(
    onBack: () -> Unit,
    onSave: (ContactDraft) -> Unit,
    onDelete: (() -> Unit)? = null,
    onResendVerification: (() -> Unit)? = null,
    initial: ContactDraft? = null,
    modifier: Modifier = Modifier
) {
    val isEdit = initial?.id != null
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var mobile by remember { mutableStateOf(initial?.mobile.orEmpty()) }
    var relationship by remember { mutableStateOf(initial?.relationship.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var type by remember { mutableStateOf(initial?.type ?: ContactType.SOS) }
    var priority by remember { mutableIntStateOf(initial?.priorityOrder ?: 1) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    PukaarScreen(
        title = stringResource(if (isEdit) R.string.edit_contact_title else R.string.add_contact_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            androidx.compose.foundation.layout.Column {
                PrimaryButton(
                    text = stringResource(if (isEdit) R.string.edit_contact_save else R.string.add_contact_save),
                    onClick = {
                        onSave(
                            ContactDraft(
                                id = initial?.id,
                                name = name.trim(),
                                mobile = mobile.trim(),
                                relationship = relationship.trim(),
                                notes = notes.trim(),
                                type = type,
                                priorityOrder = priority
                            )
                        )
                    }
                )
                if (onResendVerification != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = stringResource(R.string.contact_resend_verification),
                        onClick = onResendVerification
                    )
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SecondaryButton(
                        text = stringResource(R.string.contact_delete),
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }
        }
    ) {
        SectionCard {
            LabeledTextField(
                label = stringResource(R.string.add_contact_name),
                value = name,
                onValueChange = { name = it },
                placeholder = stringResource(R.string.add_contact_name_hint)
            )
            Spacer(modifier = Modifier.height(14.dp))
            LabeledTextField(
                label = stringResource(R.string.add_contact_mobile),
                value = mobile,
                onValueChange = { mobile = it },
                placeholder = stringResource(R.string.add_contact_mobile_hint),
                keyboardType = KeyboardType.Phone,
                prefix = stringResource(R.string.add_contact_country_code)
            )
            Spacer(modifier = Modifier.height(14.dp))
            LabeledTextField(
                label = stringResource(R.string.add_contact_relationship),
                value = relationship,
                onValueChange = { relationship = it },
                placeholder = stringResource(R.string.add_contact_relationship_hint)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RELATIONSHIP_SUGGESTIONS.forEach { suggestion ->
                    TextButtonChip(suggestion) { relationship = suggestion }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LabeledTextField(
                label = stringResource(R.string.contact_notes),
                value = notes,
                onValueChange = { notes = it },
                placeholder = stringResource(R.string.contact_notes_hint)
            )
            Spacer(modifier = Modifier.height(14.dp))
            FieldLabel(text = stringResource(R.string.contact_priority))
            (1..3).forEach { level ->
                SelectionRow(
                    icon = type.icon,
                    iconTint = type.accent,
                    title = stringResource(R.string.contact_priority_level, level),
                    subtitle = null,
                    selected = priority == level,
                    onSelect = { priority = level }
                )
                if (level < 3) RowDivider()
            }
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel(text = stringResource(R.string.add_contact_category))
            Text(
                text = stringResource(R.string.add_contact_category_hint),
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            ContactType.entries.forEachIndexed { index, entry ->
                SelectionRow(
                    icon = entry.icon,
                    iconTint = entry.accent,
                    title = stringResource(entry.labelRes),
                    subtitle = stringResource(entry.descriptionRes),
                    selected = entry == type,
                    onSelect = { type = entry }
                )
                if (index != ContactType.entries.lastIndex) RowDivider()
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.contact_delete_confirm_title)) },
            text = { Text(stringResource(R.string.contact_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.contact_delete), color = PukaarRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}

@Composable
private fun TextButtonChip(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(label, fontSize = 12.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun ContactFormScreenPreview() {
    PukaarTheme {
        ContactFormScreen(onBack = {}, onSave = {})
    }
}
