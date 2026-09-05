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
import com.pukaar.app.ui.component.InternationalPhoneField
import com.pukaar.app.ui.component.LabeledTextField
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextTertiary
import com.pukaar.app.util.PhoneNumbers

data class EmergencyInfoForm(
    val bloodGroup: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val doctorPhone: String = ""
)

@Composable
fun EmergencyInfoScreen(
    onBack: () -> Unit,
    onSave: (EmergencyInfoForm) -> Unit,
    modifier: Modifier = Modifier,
    initial: EmergencyInfoForm = EmergencyInfoForm()
) {
    var bloodGroup by remember { mutableStateOf(initial.bloodGroup) }
    var allergies by remember { mutableStateOf(initial.allergies) }
    var conditions by remember { mutableStateOf(initial.conditions) }
    val initialDoctor = remember(initial.doctorPhone) {
        if (initial.doctorPhone.isBlank()) "+91" to ""
        else PhoneNumbers.splitE164(initial.doctorPhone)
    }
    var doctorDial by remember { mutableStateOf(initialDoctor.first) }
    var doctorNational by remember { mutableStateOf(initialDoctor.second) }

    PukaarScreen(
        title = null,
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    val doctorPhone = if (doctorNational.isBlank()) {
                        ""
                    } else {
                        runCatching { PhoneNumbers.fromParts(doctorDial, doctorNational) }
                            .getOrDefault(doctorNational)
                    }
                    onSave(
                        EmergencyInfoForm(
                            bloodGroup = bloodGroup,
                            allergies = allergies,
                            conditions = conditions,
                            doctorPhone = doctorPhone
                        )
                    )
                }
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
            Spacer(modifier = Modifier.height(10.dp))

            LabeledTextField(
                label = stringResource(R.string.emergency_info_blood_group),
                value = bloodGroup,
                onValueChange = { bloodGroup = it },
                placeholder = "e.g. B+"
            )
            Spacer(modifier = Modifier.height(12.dp))
            LabeledTextField(
                label = stringResource(R.string.emergency_info_allergies),
                value = allergies,
                onValueChange = { allergies = it },
                placeholder = stringResource(R.string.emergency_info_allergies_hint)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LabeledTextField(
                label = stringResource(R.string.emergency_info_conditions),
                value = conditions,
                onValueChange = { conditions = it },
                placeholder = stringResource(R.string.emergency_info_conditions_hint)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InternationalPhoneField(
                label = stringResource(R.string.emergency_info_doctor),
                dialCode = doctorDial,
                nationalNumber = doctorNational,
                onDialCodeChange = { doctorDial = it },
                onNationalChange = { doctorNational = it },
                placeholder = stringResource(R.string.add_contact_mobile_hint)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun EmergencyInfoScreenPreview() {
    PukaarTheme {
        EmergencyInfoScreen(onBack = {}, onSave = {})
    }
}
