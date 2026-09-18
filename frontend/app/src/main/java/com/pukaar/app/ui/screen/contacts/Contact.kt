package com.pukaar.app.ui.screen.contacts

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.util.PhoneNumbers

/**
 * Contact roles used by Ritik's UI screens and our backend API roles.
 */
enum class ContactType(
    @StringRes val labelRes: Int,
    @StringRes val sectionRes: Int,
    @StringRes val sectionTaglineRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accent: Color,
    val hasPreSavedNumbers: Boolean,
    val apiRole: String
) {
    SOS(
        labelRes = R.string.contact_type_sos,
        sectionRes = R.string.contact_section_sos,
        sectionTaglineRes = R.string.contact_section_sos_tagline,
        descriptionRes = R.string.contact_type_sos_description,
        icon = Icons.Filled.Sos,
        accent = PukaarRed,
        hasPreSavedNumbers = true,
        apiRole = "SOS_TRUSTED"
    ),
    HELP(
        labelRes = R.string.contact_type_help,
        sectionRes = R.string.contact_section_help,
        sectionTaglineRes = R.string.contact_section_alert_tagline,
        descriptionRes = R.string.contact_type_help_description,
        icon = Icons.Filled.VolunteerActivism,
        accent = PukaarOrange,
        hasPreSavedNumbers = false,
        apiRole = "HELP_MONITOR"
    ),
    INACTIVITY(
        labelRes = R.string.contact_type_inactivity,
        sectionRes = R.string.contact_section_inactivity,
        sectionTaglineRes = R.string.contact_section_alert_tagline,
        descriptionRes = R.string.contact_type_inactivity_description,
        icon = Icons.Filled.Timer,
        accent = AccentBlue,
        hasPreSavedNumbers = true,
        apiRole = "HELP_BACKUP"
    ),
    DOCTOR(
        labelRes = R.string.contact_type_doctor,
        sectionRes = R.string.contact_section_doctor,
        sectionTaglineRes = R.string.contact_section_alert_tagline,
        descriptionRes = R.string.contact_type_doctor_description,
        icon = Icons.Filled.LocalHospital,
        accent = SuccessGreen,
        hasPreSavedNumbers = false,
        apiRole = "DOCTOR"
    ),
    NEIGHBOUR(
        labelRes = R.string.contact_type_neighbour,
        sectionRes = R.string.contact_section_neighbour,
        sectionTaglineRes = R.string.contact_section_alert_tagline,
        descriptionRes = R.string.contact_type_neighbour_description,
        icon = Icons.Filled.Home,
        accent = AccentBlue,
        hasPreSavedNumbers = false,
        apiRole = "NEIGHBOUR"
    )
}

enum class ContactRelation(@StringRes val labelRes: Int) {
    SPOUSE(R.string.relation_spouse),
    PARENT(R.string.relation_parent),
    CHILD(R.string.relation_child),
    SIBLING(R.string.relation_sibling),
    RELATIVE(R.string.relation_relative),
    FRIEND(R.string.relation_friend),
    NEIGHBOUR(R.string.relation_neighbour),
    COLLEAGUE(R.string.relation_colleague),
    OTHER(R.string.relation_other)
}

enum class ContactPriority(@StringRes val labelRes: Int) {
    PRIMARY(R.string.contact_tag_primary),
    SECONDARY(R.string.contact_tag_secondary)
}

data class ContactUiModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: ContactType = ContactType.SOS,
    val relation: ContactRelation? = null,
    val priority: ContactPriority? = null,
    /** Free-text relationship for the production add/edit form. */
    val relationship: String = "",
    val notes: String = "",
    val priorityOrder: Int = 1,
    val verified: Boolean = false
)

data class PreSavedNumberUiModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: ContactType,
    val relation: ContactRelation? = null
)

fun List<PreSavedNumberUiModel>.filterByAlert(type: ContactType): List<PreSavedNumberUiModel> =
    filter { it.type == type }

/** Production add/edit form payload. */
data class ContactDraft(
    val id: String? = null,
    val name: String,
    val mobile: String,
    val dialCode: String = "+91",
    val relationship: String,
    val notes: String = "",
    val type: ContactType,
    val priorityOrder: Int = 1
)

fun List<ContactUiModel>.filterByType(type: ContactType?): List<ContactUiModel> =
    if (type == null) this else filter { it.type == type }

fun List<ContactUiModel>.orderedByPriority(): List<ContactUiModel> =
    sortedBy { it.priority?.ordinal ?: ContactPriority.entries.size }

/** Backend help roles that Ritik surfaces as pre-saved under SOS / Inactivity. */
private val PreSavedRoles = setOf(ContactType.HELP, ContactType.DOCTOR, ContactType.NEIGHBOUR)

fun List<ContactUiModel>.alertContactsOnly(): List<ContactUiModel> =
    filter { it.type == ContactType.SOS || it.type == ContactType.INACTIVITY }

fun List<ContactUiModel>.asPreSavedNumbers(): List<PreSavedNumberUiModel> =
    filter { it.type in PreSavedRoles }.map { c ->
        val alertType = when {
            c.notes.contains("presaved:INACTIVITY", ignoreCase = true) -> ContactType.INACTIVITY
            else -> ContactType.SOS
        }
        PreSavedNumberUiModel(
            id = c.id,
            name = c.name,
            phoneNumber = c.phoneNumber,
            type = alertType,
            relation = c.relation
        )
    }

fun PreSavedNumberUiModel.toDraft(): ContactDraft {
    val (dial, national) = PhoneNumbers.splitE164(phoneNumber)
    val helpType = when (relation) {
        ContactRelation.NEIGHBOUR -> ContactType.NEIGHBOUR
        ContactRelation.OTHER -> ContactType.DOCTOR
        else -> ContactType.HELP
    }
    return ContactDraft(
        id = id.takeIf { it.isNotBlank() },
        name = name,
        mobile = national,
        dialCode = dial,
        relationship = relation?.name?.lowercase()?.replaceFirstChar { it.titlecase() }.orEmpty(),
        notes = "presaved:${type.name}",
        type = helpType,
        priorityOrder = 1
    )
}

fun ContactUiModel.toDraft(): ContactDraft {
    val raw = phoneNumber.trim()
    val (dial, national) = if (raw.startsWith("+") || raw.filter { it.isDigit() }.length > 10) {
        PhoneNumbers.splitE164(raw)
    } else {
        // Edit dialog often keeps national digits only — don't re-split as E.164.
        "+91" to raw.filter { it.isDigit() }.ifBlank { raw }
    }
    val relationLabel = relation?.name?.lowercase()?.replaceFirstChar { it.titlecase() }.orEmpty()
    return ContactDraft(
        id = id.takeIf { it.isNotBlank() },
        name = name,
        mobile = national,
        dialCode = dial,
        // Prefer the relation picked in the edit dialog over a stale free-text label.
        relationship = relationLabel.ifBlank { relationship },
        notes = notes,
        type = type,
        priorityOrder = priorityOrder
    )
}
