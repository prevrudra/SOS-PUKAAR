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
    SIX(6),
    TEN(10),
    TWELVE(12)
}

/** Menu item 5. Passive monitoring for someone who may not press anything. */
@Composable
fun ElderlyHelpScreen(
    onBack: () -> Unit,
    onSave: (window: InactivityWindow, medicationReminder: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var window by remember { mutableStateOf(InactivityWindow.TEN) }
    var medicationReminder by remember { mutableStateOf(true) }

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
                text = stringResource(R.string.elderly_help_inactivity),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ChoiceRow(
                title = stringResource(R.string.elderly_help_6_hours),
                selected = window == InactivityWindow.SIX,
                onSelect = { window = InactivityWindow.SIX }
            )
            ChoiceRow(
                title = stringResource(R.string.elderly_help_10_hours),
                selected = window == InactivityWindow.TEN,
                onSelect = { window = InactivityWindow.TEN }
            )
            ChoiceRow(
                title = stringResource(R.string.elderly_help_12_hours),
                selected = window == InactivityWindow.TWELVE,
                onSelect = { window = InactivityWindow.TWELVE }
            )

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
