package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAddAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pukaar.app.R

/**
 * The pre-saved numbers step, shared by both setups.
 *
 * Whoever an alert reaches is a person standing somewhere with a phone, and the
 * first thing they need is somebody else to ring — a doctor, a neighbour, the
 * relative who lives nearest. Both SOS and Inactivity collect that list, so the
 * slots and the picker live here rather than being written twice and drifting.
 *
 * Each flow keeps its own wording, headings and footer around these: the pages
 * are the same mechanism, not the same page.
 */

/**
 * The numbered slots, filled in order.
 *
 * Only the next empty slot is offered. Numbers past [min] say so, because the
 * difference between "you must" and "you may" is the whole reason somebody
 * abandons a form at slot three.
 */
@Composable
fun ColumnScope.HelpNumberSlots(
    numbers: HelpNumberList,
    accent: Color,
    min: Int,
    max: Int,
    onSlotClick: (slot: Int, existingId: String?) -> Unit
) {
    repeat(max) { slot ->
        val existing = numbers.numbers.getOrNull(slot)
        if (existing != null) {
            HelpNumberRow(
                index = slot + 1,
                number = existing,
                accent = accent,
                optional = slot >= min,
                onEdit = { onSlotClick(slot, existing.id) },
                onDelete = { numbers.remove(existing.id) }
            )
        } else {
            AddSlotButton(
                text = if (slot < min) {
                    stringResource(R.string.onboarding_add_number_n, slot + 1)
                } else {
                    stringResource(R.string.onboarding_add_number_n_optional, slot + 1)
                },
                onClick = { onSlotClick(slot, null) },
                accent = accent,
                icon = Icons.Filled.PersonAddAlt,
                enabled = slot == numbers.numbers.size
            )
        }
    }
}

/**
 * The page a slot opens onto: name, number and relation for one help number.
 *
 * A page of its own rather than a form expanding in place, because the slots
 * above it are a fixed run and a form growing between them moves everything the
 * user was just looking at.
 */
@Composable
fun ColumnScope.SelectHelpNumberPage(
    slot: Int,
    accent: Color,
    takenPhones: List<String>,
    onSaved: (OnboardingContact) -> Unit
) {
    FlowTitle(title = stringResource(R.string.onboarding_add_number_n, slot + 1))
    Spacer(modifier = Modifier.height(4.dp))
    ContactPicker(
        accent = accent,
        takenPhones = takenPhones,
        confirmLabel = stringResource(R.string.action_save),
        onPicked = onSaved
    )
}
