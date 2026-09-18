package com.pukaar.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pukaar.app.R
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextPrimary

/**
 * The card every blocking panel in the app is drawn on: a rounded surface, a close
 * control in the corner, and a body that scrolls if it has to.
 *
 * One shell rather than one per screen. The onboarding gates appear back to back,
 * and two panels that almost matched would look like a bug rather than a pair.
 *
 * Nothing dismisses it but the X. These are questions that cannot be asked again
 * later, and a tap landing outside the card must not count as an answer.
 */
@Composable
fun PukaarDialogShell(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 620.dp,
    sideMargin: Dp = 18.dp,
    /**
     * Off where the panel's own buttons are the only way out. A corner X beside a
     * "Maybe Later" would be two ways to say the same no.
     */
    showClose: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = sideMargin)
                .background(SurfaceCard, shape)
                .border(1.dp, Outline, shape)
        ) {
            Column(
                modifier = Modifier
                    // A ceiling rather than a height: a long panel at a large font
                    // scale must not push its own button off the bottom of a short
                    // screen, where nothing could reach it.
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )

            if (showClose) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(36.dp)
                        .background(Black.copy(alpha = 0.55f), CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
