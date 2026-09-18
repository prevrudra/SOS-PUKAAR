package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarDialogShell
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The gate over the Emergency Card builder.
 *
 * It sits on the form rather than replacing it: the user can see the card they
 * were about to make, which is the argument for paying. Hiding the work behind a
 * full screen would turn an invitation into a wall.
 *
 * "Maybe Later" is the only way out, and it leaves them on the form. Somebody who
 * wants to read the fields before deciding should be able to.
 */
@Composable
fun PlanInactiveDialog(
    onActivate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarDialogShell(
        onClose = onDismiss,
        modifier = modifier,
        maxHeight = 440.dp,
        // Narrower than the onboarding gates and with no corner X: this one is an
        // interruption mid-task, not a page in its own right.
        sideMargin = 44.dp,
        showClose = false
    ) {
        AlertMark()

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.card_plan_inactive_title),
            color = TextPrimary,
            fontSize = 19.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.card_plan_inactive_body),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(R.string.action_activate_plan),
            onClick = onActivate
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.action_maybe_later),
            color = TextPrimary,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp)
        )
    }
}

/** The brand mark going off, drawn as the app's square P with three sparks. */
@Composable
private fun AlertMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(width = 78.dp, height = 66.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Struck above the mark rather than around it, so the P stays the thing
        // being read and the sparks only say that it is raised.
        Canvas(modifier = Modifier.fillMaxWidth().height(18.dp)) {
            val stroke = 3.dp.toPx()
            val cx = size.width / 2f
            val rays = listOf(
                Offset(cx, size.height * 0.05f) to Offset(cx, size.height * 0.55f),
                Offset(cx - 22.dp.toPx(), size.height * 0.25f) to
                    Offset(cx - 13.dp.toPx(), size.height * 0.72f),
                Offset(cx + 22.dp.toPx(), size.height * 0.25f) to
                    Offset(cx + 13.dp.toPx(), size.height * 0.72f)
            )
            rays.forEach { (start, end) ->
                drawLine(
                    color = PukaarRed,
                    start = start,
                    end = end,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(PukaarRed, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_name).take(1).uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 600)
@Composable
private fun PlanInactiveDialogPreview() {
    PukaarTheme {
        PlanInactiveDialog(onActivate = {}, onDismiss = {})
    }
}
