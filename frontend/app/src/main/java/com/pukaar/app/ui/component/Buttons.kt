package com.pukaar.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextPrimary

private val ButtonShape = RoundedCornerShape(10.dp)
private val ButtonHeight = 46.dp

/** The red call-to-action used for SAVE, OK and every confirming action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PukaarRed,
            contentColor = TextPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        Text(text = text.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Green variant, used where the action adds something rather than confirms it. */
@Composable
fun SuccessButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SuccessGreen,
            contentColor = Color.Black
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        Text(text = text.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Quiet outlined action for secondary choices such as "View History". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = BorderStroke(1.dp, Outline),
        modifier = modifier
            .fillMaxWidth()
            .height(ButtonHeight)
    ) {
        Text(text = text.uppercase(), fontWeight = FontWeight.Medium, fontSize = 12.sp)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ButtonsPreview() {
    PukaarTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PrimaryButton(text = "Save", onClick = {})
            SuccessButton(text = "Upgrade Plan", onClick = {})
            SecondaryButton(text = "View History", onClick = {})
        }
    }
}
