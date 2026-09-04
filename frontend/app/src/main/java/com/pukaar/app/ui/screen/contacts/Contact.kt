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

enum class ContactType(
    @StringRes val labelRes: Int,
    @StringRes val sectionRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val accent: Color,
    val apiRole: String
) {
    SOS(
        labelRes = R.string.contact_type_sos,
        sectionRes = R.string.contact_section_sos,
        descriptionRes = R.string.contact_type_sos_description,
        icon = Icons.Filled.Sos,
        accent = PukaarRed,
        apiRole = "SOS_TRUSTED"
    ),
    HELP(
        labelRes = R.string.contact_type_help,
        sectionRes = R.string.contact_section_help,
        descriptionRes = R.string.contact_type_help_description,
        icon = Icons.Filled.VolunteerActivism,
        accent = PukaarOrange,
        apiRole = "HELP_MONITOR"
    ),
    INACTIVITY(
        labelRes = R.string.contact_type_inactivity,
        sectionRes = R.string.contact_section_inactivity,
        descriptionRes = R.string.contact_type_inactivity_description,
        icon = Icons.Filled.Timer,
        accent = AccentBlue,
        apiRole = "HELP_BACKUP"
    ),
    DOCTOR(
        labelRes = R.string.contact_type_doctor,
        sectionRes = R.string.contact_section_doctor,
        descriptionRes = R.string.contact_type_doctor_description,
        icon = Icons.Filled.LocalHospital,
        accent = SuccessGreen,
        apiRole = "DOCTOR"
    ),
    NEIGHBOUR(
        labelRes = R.string.contact_type_neighbour,
        sectionRes = R.string.contact_section_neighbour,
        descriptionRes = R.string.contact_type_neighbour_description,
        icon = Icons.Filled.Home,
        accent = AccentBlue,
        apiRole = "NEIGHBOUR"
    )
}

data class ContactUiModel(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: ContactType = ContactType.SOS,
    val relationship: String = "",
    val notes: String = "",
    val priorityOrder: Int = 1,
    val verified: Boolean = false
)

data class ContactDraft(
    val id: String? = null,
    val name: String,
    val mobile: String,
    val relationship: String,
    val notes: String = "",
    val type: ContactType,
    val priorityOrder: Int = 1
)

fun List<ContactUiModel>.filterByType(type: ContactType?): List<ContactUiModel> =
    if (type == null) this else filter { it.type == type }

fun ContactUiModel.toDraft(): ContactDraft = ContactDraft(
    id = id,
    name = name,
    mobile = phoneNumber.removePrefix("+91 ").removePrefix("+91"),
    relationship = relationship,
    notes = notes,
    type = type,
    priorityOrder = priorityOrder
)
