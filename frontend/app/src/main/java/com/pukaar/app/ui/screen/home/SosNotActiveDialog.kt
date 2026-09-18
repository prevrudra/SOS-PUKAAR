package com.pukaar.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarDialogShell
import com.pukaar.app.ui.component.SecondaryButton
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * What the SOS button does when there is no plan behind it.
 *
 * The button is the whole app, so it must never simply do nothing: a press that
 * produced no response would be read as a press that worked, and the one moment
 * somebody finds out otherwise is the moment they cannot afford to.
 *
 * So the refusal is loud, says why, and offers the one thing that fixes it.
 * "Maybe Later" is a real way out rather than a nag — someone who has just pressed
 * SOS by accident should not have to read a sales page to get back.
 */
@Composable
fun SosNotActiveDialog(
    onActivate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarDialogShell(onClose = onDismiss, modifier = modifier, maxHeight = 460.dp) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(PukaarRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PriorityHigh,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.sos_inactive_title),
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.sos_inactive_body),
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.action_activate_plan),
            onClick = onActivate
        )
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(
            text = stringResource(R.string.action_maybe_later),
            onClick = onDismiss
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 640)
@Composable
private fun SosNotActiveDialogPreview() {
    PukaarTheme {
        SosNotActiveDialog(onActivate = {}, onDismiss = {})
    }
}
