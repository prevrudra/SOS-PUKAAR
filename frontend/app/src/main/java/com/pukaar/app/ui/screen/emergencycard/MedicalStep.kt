package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pukaar.app.R

/**
 * Step 3. What a paramedic would want to know before touching anyone.
 *
 * All free text on purpose. A structured allergy list would be easier to render
 * and worse to read at the roadside: "penicillin, and carries an EpiPen in the
 * left pocket" is the useful answer, and no dropdown offers it.
 */
@Composable
fun MedicalStep(
    details: MedicalDetails,
    onChange: (MedicalDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPageTitle(
            title = stringResource(R.string.card_medical_title),
            subtitle = stringResource(R.string.card_medical_subtitle)
        )

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

        LabelledField(label = stringResource(R.string.card_field_allergies)) {
            CardTextField(
                value = details.allergies,
                onValueChange = { onChange(details.copy(allergies = it)) },
                placeholder = stringResource(R.string.card_hint_allergies),
                leadingIcon = Icons.Filled.Warning,
                leadingTint = CardPalette.Accent
            )
        }

        LabelledField(label = stringResource(R.string.card_field_conditions)) {
            CardTextField(
                value = details.conditions,
                onValueChange = { onChange(details.copy(conditions = it)) },
                placeholder = stringResource(R.string.card_hint_conditions),
                leadingIcon = Icons.Filled.Favorite,
                leadingTint = CardPalette.Accent
            )
        }

        LabelledField(label = stringResource(R.string.card_field_medications)) {
            CardTextField(
                value = details.medications,
                onValueChange = { onChange(details.copy(medications = it)) },
                placeholder = stringResource(R.string.card_hint_medications),
                leadingIcon = Icons.Filled.Medication,
                leadingTint = CardPalette.Accent
            )
        }

        LabelledField(label = stringResource(R.string.card_field_instructions)) {
            CardTextField(
                value = details.instructions,
                onValueChange = { onChange(details.copy(instructions = it)) },
                placeholder = stringResource(R.string.card_hint_instructions),
                leadingIcon = Icons.Filled.Description,
                // Room for a sentence, not a word: this is the field people use
                // to say the thing that does not fit anywhere else.
                singleLine = false,
                minHeight = 64
            )
        }
    }
}
