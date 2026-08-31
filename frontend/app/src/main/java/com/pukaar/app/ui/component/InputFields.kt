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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SurfaceInput
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

private val FieldShape = RoundedCornerShape(8.dp)
private val FieldHeight = 40.dp

/** Small caption sitting above each input, as in the mock-ups. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 11.sp,
        modifier = modifier.padding(bottom = 5.dp)
    )
}

@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    prefix: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(text = label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prefix != null) {
                Box(
                    modifier = Modifier
                        .height(FieldHeight)
                        .background(SurfaceInput, FieldShape)
                        .border(1.dp, Outline, FieldShape)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = prefix, color = TextPrimary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(FieldHeight)
                    .background(SurfaceInput, FieldShape)
                    .border(1.dp, Outline, FieldShape)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                    cursorBrush = SolidColor(PukaarRed),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth()
                )
                if (value.isEmpty()) {
                    Text(text = placeholder, color = TextTertiary, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * A read-only field that looks like a dropdown. The menu itself is left to the
 * screen that owns the choices.
 */
@Composable
fun LabeledDropdownField(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(text = label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldHeight)
                .background(SurfaceInput, FieldShape)
                .border(1.dp, Outline, FieldShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                color = if (value.isEmpty()) TextTertiary else TextPrimary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
