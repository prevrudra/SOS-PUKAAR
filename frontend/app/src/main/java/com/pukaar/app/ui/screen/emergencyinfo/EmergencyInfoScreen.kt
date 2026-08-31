package com.pukaar.app.ui.screen.emergencyinfo

import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.LabeledTextField
import com.pukaar.app.ui.component.NavigationRow
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextTertiary

/** Menu item 6. The details a paramedic would want, kept on the lock screen side. */
@Composable
fun EmergencyInfoScreen(
    onBack: () -> Unit,
    onBloodGroupClick: () -> Unit,
    onAllergiesClick: () -> Unit,
    onConditionsClick: () -> Unit,
    onSave: (doctorContact: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var doctorContact by remember { mutableStateOf("") }

    PukaarScreen(
        title = null,
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(doctorContact) }
            )
        }
    ) {
        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.emergency_info_title),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.emergency_info_subtitle),
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            RowDivider()

            NavigationRow(
                title = stringResource(R.string.emergency_info_blood_group),
                onClick = onBloodGroupClick
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.emergency_info_allergies),
                onClick = onAllergiesClick
            )
            RowDivider()
            NavigationRow(
                title = stringResource(R.string.emergency_info_conditions),
                onClick = onConditionsClick
            )
            RowDivider()

            Spacer(modifier = Modifier.height(14.dp))
            LabeledTextField(
                label = stringResource(R.string.emergency_info_doctor),
                value = doctorContact,
                onValueChange = { doctorContact = it },
                placeholder = stringResource(R.string.add_contact_mobile_hint),
                keyboardType = KeyboardType.Phone,
                prefix = stringResource(R.string.add_contact_country_code)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun EmergencyInfoScreenPreview() {
    PukaarTheme {
        EmergencyInfoScreen(
            onBack = {},
            onBloodGroupClick = {},
            onAllergiesClick = {},
            onConditionsClick = {},
            onSave = {}
        )
    }
}
