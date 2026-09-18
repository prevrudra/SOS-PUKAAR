package com.pukaar.app.ui.screen.emergencycard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactEmergency
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.screen.contacts.ContactRelation

/**
 * Step 1. Who the card is for, and who to ring about them.
 *
 * The only step that can block progress, and it asks for three things: a name, a
 * nationality and one contact. Everything else on the page is optional, because a
 * card with a name and a number on it already does the job.
 */
@Composable
fun PersonalDetailsStep(
    details: PersonalDetails,
    onChange: (PersonalDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    // The system picker; no runtime permission needed for a single chosen file.
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onChange(details.copy(photoUri = uri.toString()))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPageTitle(
            title = stringResource(R.string.card_personal_title),
            subtitle = stringResource(R.string.card_personal_subtitle)
        )

        RequirementKey()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionLabel(text = stringResource(R.string.card_personal_section))

                LabelledField(
                    label = stringResource(R.string.card_field_full_name),
                    required = true
                ) {
                    CardTextField(
                        value = details.fullName,
                        onValueChange = { onChange(details.copy(fullName = it)) },
                        placeholder = stringResource(R.string.card_hint_full_name)
                    )
                }

                LabelledField(
                    label = stringResource(R.string.card_field_nationality),
                    required = true
                ) {
                    CardTextField(
                        value = details.nationality,
                        onValueChange = { onChange(details.copy(nationality = it)) },
                        placeholder = stringResource(R.string.card_hint_nationality),
                        leadingIcon = Icons.Filled.Flag
                    )
                }
            }

            PhotoPicker(
                photoUri = details.photoUri,
                onPick = { pickPhoto.launch("image/*") },
                onClear = { onChange(details.copy(photoUri = null)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LabelledField(
                label = stringResource(R.string.card_field_dob),
                modifier = Modifier.weight(1f)
            ) {
                CardDateField(
                    value = details.dateOfBirth,
                    onValueChange = { onChange(details.copy(dateOfBirth = it)) },
                    placeholder = stringResource(R.string.card_hint_select_date)
                )
            }
            LabelledField(
                label = stringResource(R.string.card_field_gender),
                modifier = Modifier.weight(1f)
            ) {
                CardDropdown(
                    options = Gender.entries,
                    selected = details.gender,
                    onSelect = { onChange(details.copy(gender = it)) },
                    labelOf = { stringResource(it.labelRes) },
                    placeholder = stringResource(R.string.card_hint_select)
                )
            }
        }

        LabelledField(label = stringResource(R.string.card_field_blood_group)) {
            CardDropdown(
                options = BloodGroup.entries,
                selected = details.bloodGroup,
                onSelect = { onChange(details.copy(bloodGroup = it)) },
                labelOf = { it.label },
                placeholder = stringResource(R.string.card_hint_select),
                leadingIcon = Icons.Filled.Bloodtype,
                leadingTint = CardPalette.Accent
            )
        }

        EmergencyContactsBlock(
            primary = details.primaryContact,
            secondary = details.secondaryContact,
            onPrimaryChange = { onChange(details.copy(primaryContact = it)) },
            onSecondaryChange = { onChange(details.copy(secondaryContact = it)) }
        )
    }
}

/** The star-and-circle key that says which marks mean what. */
@Composable
private fun RequirementKey(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "★", color = CardPalette.Accent, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.card_key_required),
                color = CardPalette.TextSecondary,
                fontSize = 11.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // A hollow ring against the filled star, so the two marks read as a
            // pair rather than as two unrelated symbols.
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .border(1.dp, CardPalette.TextTertiary, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.card_key_optional),
                color = CardPalette.TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = CardPalette.TextPrimary,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

/**
 * The portrait well.
 *
 * Tapping it either picks a photo or replaces the one there; the small cross
 * removes it. Kept beside the name fields rather than above them, as in the
 * design, so step 1 stays one screenful.
 */
@Composable
private fun PhotoPicker(
    photoUri: String?,
    onPick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FieldLabel(text = stringResource(R.string.card_field_photo))
        Spacer(modifier = Modifier.height(5.dp))

        Box {
            CardPhoto(
                photoUri = photoUri,
                modifier = Modifier
                    .size(88.dp)
                    .clickable(onClick = onPick)
            )
            if (photoUri != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(CardPalette.TextPrimary, RoundedCornerShape(percent = 50))
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.onboarding_remove),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .background(CardPalette.Accent, RoundedCornerShape(6.dp))
                .clickable(onClick = onPick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(
                    if (photoUri == null) R.string.card_add_photo else R.string.card_change_photo
                ),
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * The red-tinted block at the foot of step 1.
 *
 * A primary contact is required and always shown. The secondary is added on
 * request and removed the same way — an empty second row invites people to fill
 * it in out of obligation, and a card with one good number beats one with two
 * half-remembered ones.
 */
@Composable
private fun EmergencyContactsBlock(
    primary: CardContact,
    secondary: CardContact?,
    onPrimaryChange: (CardContact) -> Unit,
    onSecondaryChange: (CardContact?) -> Unit,
    modifier: Modifier = Modifier
) {
    CardPanel(
        modifier = modifier,
        background = CardPalette.AccentWash,
        border = CardPalette.Accent.copy(alpha = 0.25f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.ContactEmergency,
                contentDescription = null,
                tint = CardPalette.Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.card_emergency_contacts),
                color = CardPalette.Accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        ContactFields(
            heading = stringResource(R.string.card_primary_contact),
            contact = primary,
            onChange = onPrimaryChange,
            required = true
        )

        if (secondary == null) {
            CardAddButton(
                text = stringResource(R.string.card_add_secondary),
                onClick = { onSecondaryChange(CardContact()) }
            )
        } else {
            ContactFields(
                heading = stringResource(R.string.card_secondary_contact),
                contact = secondary,
                onChange = onSecondaryChange,
                required = false,
                onRemove = { onSecondaryChange(null) }
            )
        }
    }
}

/** Name, relation and number on one line, as the design has them. */
@Composable
private fun ContactFields(
    heading: String,
    contact: CardContact,
    onChange: (CardContact) -> Unit,
    required: Boolean,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = heading,
                color = CardPalette.TextPrimary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (required) {
                Text(text = "★", color = CardPalette.Accent, fontSize = 8.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onRemove != null) {
                Text(
                    text = stringResource(R.string.onboarding_remove),
                    color = CardPalette.Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onRemove)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(stringResource(R.string.card_field_name))
                CardTextField(
                    value = contact.name,
                    onValueChange = { onChange(contact.copy(name = it)) },
                    placeholder = stringResource(R.string.add_contact_name_hint),
                    minHeight = 38
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel(stringResource(R.string.onboarding_relation_label))
                CardDropdown(
                    options = ContactRelation.entries,
                    selected = contact.relation,
                    onSelect = { onChange(contact.copy(relation = it)) },
                    labelOf = { stringResource(it.labelRes) },
                    placeholder = stringResource(R.string.card_hint_select)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        MicroLabel(stringResource(R.string.card_field_phone))
        CardTextField(
            value = contact.phone,
            onValueChange = { onChange(contact.copy(phone = it)) },
            placeholder = stringResource(R.string.add_contact_mobile_hint),
            leadingIcon = Icons.Filled.Phone,
            leadingTint = CardPalette.Success,
            keyboardType = KeyboardType.Phone,
            minHeight = 38
        )
    }
}

@Composable
private fun MicroLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = CardPalette.TextSecondary,
        fontSize = 10.sp,
        modifier = modifier.padding(bottom = 3.dp)
    )
}
