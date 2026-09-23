package com.pukaar.app.ui.screen.elderlyhelp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.ChoiceRow
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.ToggleRow
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary

/** How long without activity before Pukaar raises the alarm on its own. */
enum class InactivityWindow(val hours: Int) {
    TWELVE(12),
    EIGHTEEN(18),
    TWENTY_FOUR(24),
    THIRTY(30),
    THIRTY_SIX(36)
}

/** Menu item 5. Passive monitoring for someone who may not press anything. */
@Composable
fun ElderlyHelpScreen(
    onBack: () -> Unit,
    onSave: (window: InactivityWindow, medicationReminder: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    initialWindow: InactivityWindow = InactivityWindow.TWELVE,
    initialMedicationReminder: Boolean = true,
    verifiedTrustedCount: Int = 0,
    pendingTrustedCount: Int = 0,
    monitoringEnabled: Boolean = true,
    onSetupContacts: () -> Unit = {},
    onManageContacts: () -> Unit = {}
) {
    var window by remember { mutableStateOf(initialWindow) }
    var medicationReminder by remember { mutableStateOf(initialMedicationReminder) }
    val ready = verifiedTrustedCount > 0

    PukaarScreen(
        title = stringResource(R.string.elderly_help_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(window, medicationReminder) }
            )
        }
    ) {
        SectionCard {
            Text(
                text = if (ready) {
                    "Inactivity Protection is ready"
                } else {
                    "Add & verify trusted contacts to enable alerts"
                },
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$verifiedTrustedCount verified · $pendingTrustedCount pending · max 2",
                color = TextPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
            if (!ready) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alerts only go to OTP-verified contacts — not your own number.",
                    color = TextPrimary.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryButton(
                text = if (ready) "Manage trusted contacts" else "Set up trusted contacts",
                onClick = { if (ready) onManageContacts() else onSetupContacts() }
            )
            if (ready) {
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    text = "Re-run full setup",
                    onClick = onSetupContacts
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard {
            Text(
                text = stringResource(R.string.elderly_help_inactivity),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = if (monitoringEnabled) "Monitoring on" else "Monitoring off until you save",
                color = TextPrimary.copy(alpha = 0.65f),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InactivityWindow.entries.forEach { option ->
                ChoiceRow(
                    title = stringResource(R.string.elderly_help_hours, option.hours),
                    selected = window == option,
                    onSelect = { window = option }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            RowDivider()
            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                icon = Icons.Filled.Medication,
                title = stringResource(R.string.elderly_help_medication),
                subtitle = if (medicationReminder) {
                    stringResource(R.string.sos_settings_enabled)
                } else {
                    null
                },
                checked = medicationReminder,
                onCheckedChange = { medicationReminder = it }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun ElderlyHelpScreenPreview() {
    PukaarTheme {
        ElderlyHelpScreen(onBack = {}, onSave = { _, _ -> })
    }
}
