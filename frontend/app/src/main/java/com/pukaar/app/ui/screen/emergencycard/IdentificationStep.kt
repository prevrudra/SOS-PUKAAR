package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pukaar.app.R

/**
 * Step 2. Government ID, as much or as little of it as the user wants to give.
 *
 * The toggle at the top swaps the whole form rather than greying half of it out:
 * an Indian resident and a visitor carry different documents, and showing one
 * person the other's fields only invites blank answers.
 *
 * Nothing here is required. The note at the foot says why it is being asked for
 * at all, since an identity document is the thing people are most reluctant to
 * type into an app.
 */
@Composable
fun IdentificationStep(
    details: IdentificationDetails,
    onChange: (IdentificationDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPageTitle(
            title = stringResource(R.string.card_id_title),
            subtitle = stringResource(R.string.card_id_subtitle)
        )

        CardSegmentedToggle(
            options = Citizenship.entries,
            selected = details.citizenship,
            onSelect = { onChange(details.copy(citizenship = it)) },
            labelOf = { stringResource(it.labelRes) }
        )

        when (details.citizenship) {
            Citizenship.INDIAN -> IndianFields(details = details, onChange = onChange)
            Citizenship.FOREIGN -> ForeignFields(details = details, onChange = onChange)
        }

        CardNote(text = stringResource(R.string.card_id_privacy_note))
    }
}

@Composable
private fun IndianFields(
    details: IdentificationDetails,
    onChange: (IdentificationDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabelledField(label = stringResource(R.string.card_field_aadhaar)) {
            CardTextField(
                value = details.aadhaarNumber,
                onValueChange = { onChange(details.copy(aadhaarNumber = it)) },
                placeholder = stringResource(R.string.card_hint_aadhaar),
                leadingIcon = Icons.Filled.CreditCard
            )
        }
        LabelledField(label = stringResource(R.string.card_field_passport)) {
            CardTextField(
                value = details.passportNumber,
                onValueChange = { onChange(details.copy(passportNumber = it)) },
                placeholder = stringResource(R.string.card_hint_passport_in),
                leadingIcon = Icons.Filled.Badge
            )
        }
        LabelledField(label = stringResource(R.string.card_field_licence)) {
            CardTextField(
                value = details.drivingLicenceNumber,
                onValueChange = { onChange(details.copy(drivingLicenceNumber = it)) },
                placeholder = stringResource(R.string.card_hint_licence),
                leadingIcon = Icons.Filled.DirectionsCar
            )
        }
    }
}

@Composable
private fun ForeignFields(
    details: IdentificationDetails,
    onChange: (IdentificationDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabelledField(label = stringResource(R.string.card_field_passport)) {
            CardTextField(
                value = details.passportNumber,
                onValueChange = { onChange(details.copy(passportNumber = it)) },
                placeholder = stringResource(R.string.card_hint_passport_intl),
                leadingIcon = Icons.Filled.Badge
            )
        }
        LabelledField(label = stringResource(R.string.card_field_citizenship_country)) {
            CardTextField(
                value = details.countryOfCitizenship,
                onValueChange = { onChange(details.copy(countryOfCitizenship = it)) },
                placeholder = stringResource(R.string.card_hint_country),
                leadingIcon = Icons.Filled.Public
            )
        }
        LabelledField(label = stringResource(R.string.card_field_visa)) {
            CardTextField(
                value = details.visaNumber,
                onValueChange = { onChange(details.copy(visaNumber = it)) },
                placeholder = stringResource(R.string.card_hint_visa),
                leadingIcon = Icons.Filled.Badge
            )
        }
        LabelledField(label = stringResource(R.string.card_field_visa_expiry)) {
            CardDateField(
                value = details.visaExpiry,
                onValueChange = { onChange(details.copy(visaExpiry = it)) },
                placeholder = stringResource(R.string.card_hint_select_date)
            )
        }
    }
}
