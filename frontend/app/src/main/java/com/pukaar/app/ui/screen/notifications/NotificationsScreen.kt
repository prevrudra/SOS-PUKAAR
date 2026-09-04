package com.pukaar.app.ui.screen.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
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

/** Menu item 10. Which notifications the user is willing to receive. */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onSave: (NotificationPreferences) -> Unit,
    modifier: Modifier = Modifier,
    initialPreferences: NotificationPreferences = NotificationPreferences(true, true, true, false)
) {
    var alerts by remember { mutableStateOf(initialPreferences.alertNotifications) }
    var inactivity by remember { mutableStateOf(initialPreferences.inactivityAlerts) }
    var medication by remember { mutableStateOf(initialPreferences.medicationReminders) }
    var promotions by remember { mutableStateOf(initialPreferences.promotions) }

    PukaarScreen(
        title = stringResource(R.string.notifications_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    onSave(
                        NotificationPreferences(
                            alertNotifications = alerts,
                            inactivityAlerts = inactivity,
                            medicationReminders = medication,
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
                title = stringResource(R.string.notifications_alerts),
                checked = alerts,
                onCheckedChange = { alerts = it }
            )
            ToggleRow(
                icon = Icons.Filled.Timer,
                title = stringResource(R.string.notifications_inactivity),
                checked = inactivity,
                onCheckedChange = { inactivity = it }
            )
            ToggleRow(
                icon = Icons.Filled.Medication,
                title = stringResource(R.string.notifications_medication),
                checked = medication,
                onCheckedChange = { medication = it }
            )
            ToggleRow(
                icon = Icons.Filled.Campaign,
                title = stringResource(R.string.notifications_promotions),
                checked = promotions,
                onCheckedChange = { promotions = it }
            )
        }
    }
}

/** UI shape handed back on save. */
data class NotificationPreferences(
    val alertNotifications: Boolean,
    val inactivityAlerts: Boolean,
    val medicationReminders: Boolean,
    val promotions: Boolean
)

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun NotificationsScreenPreview() {
    PukaarTheme {
        NotificationsScreen(onBack = {}, onSave = {})
    }
}
