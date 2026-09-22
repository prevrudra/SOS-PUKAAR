package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.ui.component.InternationalPhoneField
import com.pukaar.app.ui.screen.contacts.ContactRelation
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SurfaceInput
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextTertiary
import com.pukaar.app.util.PhoneNumbers
import java.util.UUID

/** The shortest string accepted as a phone number. */
private const val MinPhoneDigits = 6

/** Compares numbers by digits alone, so spacing and a +91 never hide a duplicate. */
private fun String.phoneDigits() = filter { it.isDigit() }.takeLast(10)

@Composable
fun ContactPicker(
    accent: Color,
    takenPhones: List<String>,
    confirmLabel: String,
    onPicked: (OnboardingContact) -> Unit,
    modifier: Modifier = Modifier,
    indiaOnlyPhones: Boolean = true,
    onUpgradeToGlobal: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var dialCode by remember { mutableStateOf("+91") }
    var national by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf<ContactRelation?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val store = PukaarApp.instance.sessionStore
        val indiaOnly = store.indiaOnlyPhones()
        if (indiaOnly) dialCode = "+91"
    }

    val locked = indiaOnlyPhones
    val e164 = remember(dialCode, national, locked) {
        val dial = if (locked) "+91" else dialCode
        runCatching { PhoneNumbers.fromParts(dial, national) }.getOrDefault("")
    }
    val digits = e164.phoneDigits().ifBlank { national.phoneDigits() }
    val duplicate = digits.isNotEmpty() && takenPhones.any { it.phoneDigits() == digits }
    val ready = name.isNotBlank() &&
        digits.length >= MinPhoneDigits &&
        !duplicate &&
        relation != null

    fun commit() {
        val phone = e164.ifBlank {
            PhoneNumbers.fromParts(if (locked) "+91" else dialCode, national)
        }
        onPicked(
            OnboardingContact(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone,
                relation = relation
            )
        )
        name = ""
        dialCode = if (locked) "+91" else dialCode
        national = ""
        relation = null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        OnboardingTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(R.string.add_contact_name_hint),
            accent = accent
        )
        InternationalPhoneField(
            label = "",
            dialCode = if (locked) "+91" else dialCode,
            nationalNumber = national,
            onDialCodeChange = { if (!locked) dialCode = it },
            onNationalChange = { national = it },
            placeholder = stringResource(R.string.add_contact_mobile_hint),
            indiaOnlyPhones = locked,
            onIndiaLockedClick = onUpgradeToGlobal
        )
        RelationDropdown(
            relation = relation,
            onRelationChange = { relation = it },
            accent = accent
        )

        if (duplicate) {
            Text(
                text = stringResource(R.string.onboarding_number_already_added),
                color = PukaarRed,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        AddSlotButton(
            text = confirmLabel,
            onClick = ::commit,
            accent = accent,
            enabled = ready
        )
    }
}

@Composable
fun RelationDropdown(
    relation: ContactRelation?,
    onRelationChange: (ContactRelation) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(SurfaceInput, shape)
                .border(1.dp, Outline, shape)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = relation?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.onboarding_relation_hint),
                color = if (relation != null) TextPrimary else TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ContactRelation.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(text = stringResource(option.labelRes), fontSize = 13.sp)
                    },
                    onClick = {
                        onRelationChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ContactAdder(
    label: String,
    confirmLabel: String,
    accent: Color,
    takenPhones: List<String>,
    onAdded: (OnboardingContact) -> Unit,
    modifier: Modifier = Modifier,
    startExpanded: Boolean = false,
    indiaOnlyPhones: Boolean = true,
    onUpgradeToGlobal: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(startExpanded) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (expanded) {
            ContactPicker(
                accent = accent,
                takenPhones = takenPhones,
                confirmLabel = confirmLabel,
                onPicked = {
                    onAdded(it)
                    expanded = false
                },
                indiaOnlyPhones = indiaOnlyPhones,
                onUpgradeToGlobal = onUpgradeToGlobal
            )
            TextAction(
                text = stringResource(R.string.action_cancel),
                onClick = { expanded = false }
            )
        } else {
            AccentOutlineButton(
                text = label,
                onClick = { expanded = true },
                accent = accent
            )
        }
    }
}
