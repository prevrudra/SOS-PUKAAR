package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarDialogShell
import com.pukaar.app.ui.component.PukaarShield
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.pukaar.app.ui.theme.InactivityPurple
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The gate in front of Quick Onboarding.
 *
 * Setting PUKAAR up for somebody else is the one mistake in this flow that cannot
 * be undone from inside it: the contacts, the codes and the coupon all bind to
 * whichever phone walked through onboarding. Somebody setting it up for a parent
 * on their own handset finds out at the end, having built the wrong person's
 * safety net.
 *
 * So it is asked before the first step rather than explained after the last, and
 * the acknowledgement is a real one — [onContinue] stays shut until the box is
 * ticked. Dismissing is deliberately only the X: a stray tap outside must not
 * count as having read it.
 */
@Composable
fun BeforeYouStartDialog(
    onContinue: (userName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialName: String = ""
) {
    var acknowledged by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf(initialName) }

    PukaarDialogShell(onClose = onDismiss, modifier = modifier) {
        PukaarShield(size = 44)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name).uppercase(),
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.before_start_title),
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.before_start_subtitle),
            color = TextSecondary,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))
        AudiencePanel(
            icon = Icons.Filled.Person,
            accent = PukaarRed,
            lead = stringResource(R.string.before_start_yourself_lead),
            highlight = stringResource(R.string.before_start_yourself_highlight),
            body = stringResource(R.string.before_start_yourself_body)
        )

        Spacer(modifier = Modifier.height(12.dp))
        AudiencePanel(
            icon = Icons.Filled.Groups,
            accent = InactivityPurple,
            lead = stringResource(R.string.before_start_someone_lead),
            highlight = stringResource(R.string.before_start_someone_highlight),
            body = stringResource(R.string.before_start_someone_body),
            footnote = stringResource(R.string.before_start_someone_footnote)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_your_name_title),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        OnboardingTextField(
            value = userName,
            onValueChange = { userName = it },
            placeholder = stringResource(R.string.onboarding_your_name_hint),
            accent = PukaarRed,
            capitalization = KeyboardCapitalization.Words
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.onboarding_your_name_body),
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(14.dp))
        AcknowledgeRow(
            checked = acknowledged,
            onCheckedChange = { acknowledged = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
        AccentButton(
            text = stringResource(R.string.action_continue),
            onClick = { onContinue(userName.trim()) },
            accent = PukaarRed,
            enabled = acknowledged && userName.trim().length >= 2
        )
    }
}

/**
 * One of the two cases, in its own colour.
 *
 * The colours are doing work rather than decorating: the two panels say opposite
 * things about whose phone this is, and a reader skimming needs to see at a glance
 * that they are alternatives, not steps.
 */
@Composable
private fun AudiencePanel(
    icon: ImageVector,
    accent: Color,
    lead: String,
    highlight: String,
    body: String,
    modifier: Modifier = Modifier,
    footnote: String? = null
) {
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.10f), shape)
            .border(1.dp, accent.copy(alpha = 0.55f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextPrimary)) { append("$lead ") }
                    withStyle(SpanStyle(color = accent)) { append(highlight) }
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 21.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = body,
                color = TextPrimary.copy(alpha = 0.90f),
                fontSize = 13.5.sp,
                lineHeight = 19.sp
            )
            if (footnote != null) {
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = footnote,
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/** The tick that unlocks Continue. The whole row is the target, not just the box. */
@Composable
private fun AcknowledgeRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(7.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (checked) PukaarRed else Color.Transparent, shape)
                .border(2.dp, if (checked) PukaarRed else TextSecondary, shape),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.before_start_acknowledge),
            color = TextPrimary,
            fontSize = 14.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun BeforeYouStartDialogPreview() {
    PukaarTheme {
        BeforeYouStartDialog(onContinue = { _ -> }, onDismiss = {})
    }
}
