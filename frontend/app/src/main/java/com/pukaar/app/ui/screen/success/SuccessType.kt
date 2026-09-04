package com.pukaar.app.ui.screen.success

import androidx.annotation.StringRes
import com.pukaar.app.R

/**
 * Every confirmation the app can show.
 *
 * The mock-ups repeat the same green-check panel after eight different actions,
 * so there is one screen and this enum picks the wording.
 */
enum class SuccessType(@StringRes val messageRes: Int) {
    CONTACT_ADDED(R.string.add_contact_success),
    CONTACT_UPDATED(R.string.edit_contact_success),
    CONTACT_DELETED(R.string.contact_deleted_success),
    SOS_SETTINGS_SAVED(R.string.sos_settings_success),
    DRILL_SENT(R.string.mock_drill_success),
    ELDERLY_HELP_SAVED(R.string.elderly_help_success),
    EMERGENCY_INFO_SAVED(R.string.emergency_info_success),
    PAYMENT_COMPLETED(R.string.payment_success),
    LANGUAGE_SAVED(R.string.language_success),
    NOTIFICATIONS_SAVED(R.string.notifications_success);

    companion object {
        fun fromName(value: String?): SuccessType =
            entries.firstOrNull { it.name == value } ?: CONTACT_ADDED
    }
}
