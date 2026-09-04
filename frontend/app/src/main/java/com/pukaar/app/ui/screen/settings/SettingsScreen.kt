package com.pukaar.app.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pukaar.app.R
import com.pukaar.app.ui.component.NavigationRow
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onBatteryOptimization: () -> Unit,
    onAutostart: () -> Unit,
    onOverlayPermission: () -> Unit,
    onVolumeSosAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = modifier
    ) {
        SectionCard {
            NavigationRow(
                title = stringResource(R.string.settings_battery),
                subtitle = stringResource(R.string.settings_battery_hint),
                onClick = onBatteryOptimization,
                leading = {
                    Icon(Icons.Filled.BatteryChargingFull, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.settings_autostart),
                subtitle = stringResource(R.string.settings_autostart_hint),
                onClick = onAutostart,
                leading = {
                    Icon(Icons.Filled.Launch, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.settings_overlay),
                subtitle = stringResource(R.string.settings_overlay_hint),
                onClick = onOverlayPermission,
                leading = {
                    Icon(Icons.Filled.Notifications, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.settings_volume_sos),
                subtitle = stringResource(R.string.settings_volume_sos_hint),
                onClick = onVolumeSosAccessibility,
                leading = {
                    Icon(Icons.Filled.VolumeUp, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.settings_permissions),
                subtitle = stringResource(R.string.settings_permissions_hint),
                onClick = onBatteryOptimization,
                leading = {
                    Icon(Icons.Filled.Security, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 500)
@Composable
private fun SettingsScreenPreview() {
    PukaarTheme {
        SettingsScreen(onBack = {}, onBatteryOptimization = {}, onAutostart = {}, onOverlayPermission = {}, onVolumeSosAccessibility = {})
    }
}
