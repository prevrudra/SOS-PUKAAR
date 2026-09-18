package com.pukaar.app.ui.screen.success

import androidx.annotation.StringRes
import com.pukaar.app.R

/**
 * Every confirmation the app can show.
 *
 * The mock-ups repeat the same green-check panel after several different actions,
 * so there is one screen and this enum picks the wording.
 */
enum class SuccessType(@StringRes val messageRes: Int) {
    CONTACT_ADDED(R.string.add_contact_success),
    DRILL_SENT(R.string.mock_drill_success),
    PAYMENT_COMPLETED(R.string.payment_success),
    GENERAL_SETTINGS_SAVED(R.string.general_success),
    LANGUAGE_SAVED(R.string.language_success),
    INACTIVITY_TIMING_SAVED(R.string.inactivity_timing_success);

    companion object {
        fun fromName(value: String?): SuccessType =
            entries.firstOrNull { it.name == value } ?: CONTACT_ADDED
    }
}
