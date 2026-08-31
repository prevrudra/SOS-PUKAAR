package com.pukaar.app.ui.screen.sossettings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TouchApp
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
import com.pukaar.app.ui.component.ValueRow
import com.pukaar.app.ui.theme.PukaarTheme

/** Menu item 2. What happens the moment SOS is pressed. */
@Composable
fun SosSettingsScreen(
    onBack: () -> Unit,
    onSave: (SosSettingsForm) -> Unit,
    modifier: Modifier = Modifier
) {
    var autoCall by remember { mutableStateOf(true) }
    var locationSharing by remember { mutableStateOf(true) }
    var audioRecording by remember { mutableStateOf(true) }
    var alertContacts by remember { mutableStateOf(true) }

    PukaarScreen(
        title = stringResource(R.string.sos_settings_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    onSave(
                        SosSettingsForm(
                            autoCallEmergencyNumber = autoCall,
                            shareLocation = locationSharing,
                            recordAudio = audioRecording,
                            alertContacts = alertContacts
                        )
                    )
                }
            )
        }
    ) {
        SectionCard {
            ValueRow(
                icon = Icons.Filled.TouchApp,
                title = stringResource(R.string.sos_settings_trigger),
                value = stringResource(R.string.sos_settings_trigger_value)
            )
            ToggleRow(
                icon = Icons.AutoMirrored.Filled.PhoneForwarded,
                title = stringResource(R.string.sos_settings_auto_call),
                subtitle = stringResource(R.string.sos_settings_enabled),
                checked = autoCall,
                onCheckedChange = { autoCall = it }
            )
            ToggleRow(
                icon = Icons.Filled.LocationOn,
                title = stringResource(R.string.sos_settings_location),
                subtitle = stringResource(R.string.sos_settings_enabled),
                checked = locationSharing,
                onCheckedChange = { locationSharing = it }
            )
            ToggleRow(
                icon = Icons.Filled.Mic,
                title = stringResource(R.string.sos_settings_audio),
                subtitle = stringResource(R.string.sos_settings_enabled),
                checked = audioRecording,
                onCheckedChange = { audioRecording = it }
            )
            ToggleRow(
                icon = Icons.Filled.Groups,
                title = stringResource(R.string.sos_settings_alert_contacts),
                subtitle = stringResource(R.string.sos_settings_enabled),
                checked = alertContacts,
                onCheckedChange = { alertContacts = it }
            )
        }
    }
}

/** What the screen hands back on save. Not a domain model — UI shape only. */
data class SosSettingsForm(
    val autoCallEmergencyNumber: Boolean,
    val shareLocation: Boolean,
    val recordAudio: Boolean,
    val alertContacts: Boolean
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun SosSettingsScreenPreview() {
    PukaarTheme {
        SosSettingsScreen(onBack = {}, onSave = {})
    }
}
