package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarDialogShell
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The second gate: the code that unlocks a setup.
 *
 * It is asked here, between picking a protection and the first step, rather than
 * at the end — a coupon refused after somebody has verified three contacts is a
 * wasted evening, and the notice before it has just told them to have the code to
 * hand.
 *
 * [accent] is the protection's own colour, so the dialog belongs to the flow it is
 * opening: red in front of SOS, purple in front of Inactivity. The user has just
 * tapped a coloured card, and the panel that answers it should be the same colour.
 */
@Composable
fun CouponDialog(
    accent: Color,
    onProceed: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    val entered = code.trim()

    PukaarDialogShell(onClose = onDismiss, modifier = modifier, maxHeight = 520.dp) {
        Spacer(modifier = Modifier.height(6.dp))
        Icon(
            imageVector = Icons.Filled.ConfirmationNumber,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.height(56.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.coupon_title),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.coupon_subtitle),
            color = TextSecondary,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))
        OnboardingTextField(
            value = code,
            onValueChange = { code = it },
            placeholder = stringResource(R.string.coupon_hint),
            accent = accent,
            leadingIcon = Icons.Filled.ConfirmationNumber,
            // Coupons are read off a message and typed in; the shift key is one
            // more thing to get wrong.
            capitalization = KeyboardCapitalization.Characters,
            minHeight = 52
        )

        Spacer(modifier = Modifier.height(16.dp))
        AccentButton(
            text = stringResource(R.string.coupon_proceed),
            onClick = { onProceed(entered) },
            accent = accent,
            enabled = entered.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.coupon_footnote),
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun CouponDialogSosPreview() {
    PukaarTheme {
        CouponDialog(
            accent = ProtectionType.SOS.accent,
            onProceed = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun CouponDialogInactivityPreview() {
    PukaarTheme {
        CouponDialog(
            accent = ProtectionType.INACTIVITY.accent,
            onProceed = {},
            onDismiss = {}
        )
    }
}
