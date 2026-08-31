package com.pukaar.app.ui.screen.addcontact

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.pukaar.app.ui.component.LabeledDropdownField
import com.pukaar.app.ui.component.LabeledTextField
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SelectionRow
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextTertiary

/**
 * Menu item 1. Collects an emergency contact and what they are alerted for.
 *
 * UI only: the fields hold local state so the screen is usable in a preview and
 * on device, but nothing is persisted — [onSaveContact] is where that goes.
 */
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onSaveContact: (ContactDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    // SOS is preselected so a contact can never be saved reaching nobody.
    var type by remember { mutableStateOf(ContactType.SOS) }

    PukaarScreen(
        title = stringResource(R.string.add_contact_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.add_contact_save),
                onClick = {
                    onSaveContact(
                        ContactDraft(
                            name = name,
                            mobile = mobile,
                            relationship = relationship,
                            type = type
                        )
                    )
                }
            )
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
            LabeledDropdownField(
                label = stringResource(R.string.add_contact_relationship),
                value = relationship,
                placeholder = stringResource(R.string.add_contact_relationship_hint),
                onClick = { /* TODO: show the relationship picker */ }
            )
            Spacer(modifier = Modifier.height(16.dp))

            FieldLabel(text = stringResource(R.string.add_contact_category))
            Text(
                text = stringResource(R.string.add_contact_category_hint),
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // One row per category, in order. Exactly one can be picked: a contact
            // has a single job, so choosing another releases the previous one.
            ContactType.entries.forEachIndexed { index, entry ->
                SelectionRow(
                    icon = entry.icon,
                    iconTint = entry.accent,
                    title = stringResource(entry.labelRes),
                    subtitle = stringResource(entry.descriptionRes),
                    selected = entry == type,
                    onSelect = { type = entry }
                )
                if (index != ContactType.entries.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 760)
@Composable
private fun AddContactScreenPreview() {
    PukaarTheme {
        AddContactScreen(onBack = {}, onSaveContact = {})
    }
}
