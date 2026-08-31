package com.pukaar.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.SurfaceInput
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

private val ChipShape = RoundedCornerShape(20.dp)

/**
 * A tappable pill that fills with [accent] when selected. Used both for picking a
 * contact's alert types and for filtering the contacts list.
 */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (selected) accent else SurfaceInput,
        label = "chipBackground"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else Outline,
        label = "chipBorder"
    )

    Box(
        modifier = modifier
            .background(background, ChipShape)
            .border(1.dp, borderColor, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) TextPrimary else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** A small read-only tag, used to show what a contact is reached for. */
@Composable
fun TypeBadge(
    text: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(5.dp))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
