package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R

/**
 * Step 5. Everything given so far, and one way back into each part of it.
 *
 * Each row summarises its step in a line and carries an Edit link straight to it,
 * so a wrong blood group is three taps to fix rather than four Backs. A step left
 * entirely empty says so plainly instead of showing a blank line — the review is
 * also the last chance to notice something was skipped.
 */
@Composable
fun ReviewStep(
    draft: EmergencyCardDraft,
    onEdit: (EmergencyCardStep) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPageTitle(
            title = stringResource(R.string.card_review_title),
            subtitle = stringResource(R.string.card_review_subtitle)
        )

        CardPanel {
            ReviewRow(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.card_review_personal),
                summary = personalSummary(draft),
                onEdit = { onEdit(EmergencyCardStep.PERSONAL) }
            )
            ReviewDivider()
            ReviewRow(
                icon = Icons.Filled.Badge,
                title = stringResource(R.string.card_review_id),
                summary = identificationSummary(draft),
                onEdit = { onEdit(EmergencyCardStep.IDENTIFICATION) }
            )
            ReviewDivider()
            ReviewRow(
                icon = Icons.Filled.MonitorHeart,
                title = stringResource(R.string.card_review_medical),
                summary = medicalSummary(draft),
                onEdit = { onEdit(EmergencyCardStep.MEDICAL) }
            )
            ReviewDivider()
            ReviewRow(
                icon = Icons.Filled.Flight,
                title = stringResource(R.string.card_review_travel),
                summary = travelSummary(draft),
                onEdit = { onEdit(EmergencyCardStep.TRAVEL) }
            )
        }

        CardNote(
            text = stringResource(R.string.card_review_note),
            icon = Icons.Filled.Info,
            tint = CardPalette.Accent,
            background = CardPalette.AccentWash
        )
    }
}

@Composable
private fun ReviewRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(CardPalette.AccentWash, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CardPalette.Accent,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CardPalette.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary,
                color = CardPalette.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.onboarding_edit),
            color = CardPalette.Accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onEdit)
        )
    }
}

@Composable
private fun ReviewDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardPalette.Border)
    )
}

/**
 * The four summary lines.
 *
 * Each falls back to "not added" rather than an empty string, so a skipped step
 * is visibly skipped instead of looking like a rendering bug.
 */
@Composable
private fun personalSummary(draft: EmergencyCardDraft): String {
    val parts = listOfNotNull(
        draft.personal.fullName.takeIf { it.isNotBlank() },
        draft.personal.nationality.takeIf { it.isNotBlank() },
        draft.personal.primaryContact.name.takeIf { it.isNotBlank() }
    )
    return parts.ifEmpty { null }?.joinToString(", ")
        ?: stringResource(R.string.card_review_empty)
}

@Composable
private fun identificationSummary(draft: EmergencyCardDraft): String {
    val labels = draft.identification.filledLabels
    return if (labels.isEmpty()) {
        stringResource(R.string.card_review_empty)
    } else {
        labels.joinToString(", ")
    }
}

@Composable
private fun medicalSummary(draft: EmergencyCardDraft): String {
    if (draft.medical.isEmpty) return stringResource(R.string.card_review_empty)

    val parts = mutableListOf<String>()
    draft.medical.bloodGroup?.let {
        parts += stringResource(R.string.card_face_blood_group, it.label)
    }
    if (draft.medical.allergies.isNotBlank()) {
        parts += stringResource(R.string.card_review_allergies, draft.medical.allergies)
    }
    if (draft.medical.conditions.isNotBlank()) parts += draft.medical.conditions
    return parts.joinToString(" · ")
}

@Composable
private fun travelSummary(draft: EmergencyCardDraft): String {
    if (draft.travel.isEmpty) return stringResource(R.string.card_review_empty)

    val dates = when {
        draft.travel.startDate != null && draft.travel.endDate != null ->
            "${formatCardDate(draft.travel.startDate)} - ${formatCardDate(draft.travel.endDate)}"

        draft.travel.startDate != null -> formatCardDate(draft.travel.startDate)
        else -> null
    }

    return listOfNotNull(
        draft.travel.destination.takeIf { it.isNotBlank() },
        dates
    ).joinToString(", ")
}
