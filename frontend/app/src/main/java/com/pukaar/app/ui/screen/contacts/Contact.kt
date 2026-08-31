package com.pukaar.app.ui.screen.contacts

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed

/**
 * Which alert a contact is reached for.
 *
 * A contact belongs to exactly one of these. One person, one job: when an alert
 * fires there is no ambiguity about who it goes to, and the contacts list reads
 * as three separate lists rather than the same names repeated under each heading.
 */
enum class ContactType(
    @StringRes val labelRes: Int,
    @StringRes val sectionRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accent: Color
) {
    /** Life-threatening emergency — the red SOS button. */
    SOS(
        labelRes = R.string.contact_type_sos,
        sectionRes = R.string.contact_section_sos,
        descriptionRes = R.string.contact_type_sos_description,
        icon = Icons.Filled.Sos,
        accent = PukaarRed
    ),

    /** Assistance from family, without escalating to emergency services. */
    HELP(
        labelRes = R.string.contact_type_help,
        sectionRes = R.string.contact_section_help,
        descriptionRes = R.string.contact_type_help_description,
        icon = Icons.Filled.VolunteerActivism,
        accent = PukaarOrange
    ),

    /** Notified when Elderly Help sees no activity. */
    INACTIVITY(
        labelRes = R.string.contact_type_inactivity,
        sectionRes = R.string.contact_section_inactivity,
        descriptionRes = R.string.contact_type_inactivity_description,
        icon = Icons.Filled.Timer,
        accent = AccentBlue
    )
}

/** One row in the contacts list. Replace with the domain model later. */
data class ContactUiModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: ContactType = ContactType.SOS
)

/** What the Add Contact form hands back. A UI shape, not a domain model. */
data class ContactDraft(
    val name: String,
    val mobile: String,
    val relationship: String,
    val type: ContactType
)

/**
 * Narrows [this] to the contacts reached for [type], or leaves it untouched when
 * [type] is null — the "All" case.
 */
fun List<ContactUiModel>.filterByType(type: ContactType?): List<ContactUiModel> =
    if (type == null) this else filter { it.type == type }
