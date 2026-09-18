package com.pukaar.app.ui.screen.general

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.ToggleRow
import com.pukaar.app.ui.theme.PukaarTheme

/** The app-wide switches: what Pukaar may send, sound and share. */
@Composable
fun GeneralSettingsScreen(
    onBack: () -> Unit,
    onSave: (GeneralSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var alerts by remember { mutableStateOf(true) }
    var soundAndVibration by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }
    var promotions by remember { mutableStateOf(false) }

    PukaarScreen(
        title = stringResource(R.string.general_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    onSave(
                        GeneralSettings(
                            alertNotifications = alerts,
                            soundAndVibration = soundAndVibration,
                            locationSharing = locationSharing,
                            promotions = promotions
                        )
                    )
                }
            )
        }
    ) {
        SectionCard {
            ToggleRow(
                icon = Icons.Filled.NotificationsActive,
                title = stringResource(R.string.general_alerts),
                subtitle = stringResource(R.string.general_alerts_subtitle),
                checked = alerts,
                onCheckedChange = { alerts = it }
            )
            ToggleRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(R.string.general_sound),
                subtitle = stringResource(R.string.general_sound_subtitle),
                checked = soundAndVibration,
                onCheckedChange = { soundAndVibration = it }
            )
            ToggleRow(
                icon = Icons.Filled.LocationOn,
                title = stringResource(R.string.general_location),
                subtitle = stringResource(R.string.general_location_subtitle),
                checked = locationSharing,
                onCheckedChange = { locationSharing = it }
            )
            ToggleRow(
                icon = Icons.Filled.Campaign,
                title = stringResource(R.string.general_promotions),
                subtitle = stringResource(R.string.general_promotions_subtitle),
                checked = promotions,
                onCheckedChange = { promotions = it }
            )
        }
    }
}

/** UI shape handed back on save. */
data class GeneralSettings(
    val alertNotifications: Boolean,
    val soundAndVibration: Boolean,
    val locationSharing: Boolean,
    val promotions: Boolean
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun GeneralSettingsScreenPreview() {
    PukaarTheme {
        GeneralSettingsScreen(onBack = {}, onSave = {})
    }
}
