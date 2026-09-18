package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pukaar.app.R

/**
 * Step 4. Where the user is, for as long as they are there.
 *
 * The whole step is skippable — someone making a card to keep in a wallet at home
 * has nothing to put here — but for a trip it is the part that tells a responder
 * which consulate to ring and which hotel to send someone to.
 */
@Composable
fun TravelStep(
    details: TravelDetails,
    onChange: (TravelDetails) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPageTitle(
            title = stringResource(R.string.card_travel_title),
            subtitle = stringResource(R.string.card_travel_subtitle)
        )

        LabelledField(label = stringResource(R.string.card_field_destination)) {
            CardTextField(
                value = details.destination,
                onValueChange = { onChange(details.copy(destination = it)) },
                placeholder = stringResource(R.string.card_hint_destination),
                leadingIcon = Icons.Filled.LocationOn,
                leadingTint = CardPalette.Accent
            )
        }

        LabelledField(label = stringResource(R.string.card_field_travel_start)) {
            CardDateField(
                value = details.startDate,
                onValueChange = { onChange(details.copy(startDate = it)) },
                placeholder = stringResource(R.string.card_hint_select_date)
            )
        }

        LabelledField(label = stringResource(R.string.card_field_travel_end)) {
            CardDateField(
                value = details.endDate,
                onValueChange = { onChange(details.copy(endDate = it)) },
                placeholder = stringResource(R.string.card_hint_select_date)
            )
        }

        LabelledField(label = stringResource(R.string.card_field_accommodation)) {
            CardTextField(
                value = details.accommodation,
                onValueChange = { onChange(details.copy(accommodation = it)) },
                placeholder = stringResource(R.string.card_hint_accommodation),
                leadingIcon = Icons.Filled.Hotel
            )
        }

        LabelledField(label = stringResource(R.string.card_field_local_address)) {
            CardTextField(
                value = details.localAddress,
                onValueChange = { onChange(details.copy(localAddress = it)) },
                placeholder = stringResource(R.string.card_hint_local_address),
                leadingIcon = Icons.Filled.LocationOn,
                singleLine = false,
                minHeight = 56
            )
        }
    }
}
