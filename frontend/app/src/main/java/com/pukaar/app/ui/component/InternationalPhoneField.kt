package com.pukaar.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.util.PhoneNumbers
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SurfaceInput
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

@Composable
fun InternationalPhoneField(
    label: String,
    dialCode: String,
    nationalNumber: String,
    onDialCodeChange: (String) -> Unit,
    onNationalChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Mobile number",
    enabled: Boolean = true
) {
    var menuOpen by remember { mutableStateOf(false) }
    val selected = PhoneNumbers.countryForDialCode(dialCode)
        ?: PhoneNumbers.Country("XX", dialCode, dialCode)

    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(text = label)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(SurfaceInput, RoundedCornerShape(8.dp))
                .border(1.dp, Outline, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    Modifier
                        .clickable(enabled = enabled) { menuOpen = true }
                        .padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selected.dialCode,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.width(18.dp)
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    PhoneNumbers.countries.forEach { country ->
                        DropdownMenuItem(
                            text = {
                                Text("${country.dialCode}  ${country.name}", fontSize = 13.sp)
                            },
                            onClick = {
                                onDialCodeChange(country.dialCode)
                                menuOpen = false
                            }
                        )
                    }
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Outline)
            )
            BasicTextField(
                value = nationalNumber,
                onValueChange = { onNationalChange(it.filter { ch -> ch.isDigit() }.take(15)) },
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                cursorBrush = SolidColor(PukaarRed),
                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                decorationBox = { inner ->
                    if (nationalNumber.isEmpty()) {
                        Text(placeholder, color = TextTertiary, fontSize = 13.sp)
                    }
                    inner()
                }
            )
        }
    }
}
