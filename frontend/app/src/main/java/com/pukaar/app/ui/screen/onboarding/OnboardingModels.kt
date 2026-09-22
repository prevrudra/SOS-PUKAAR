package com.pukaar.app.ui.screen.onboarding

import androidx.compose.ui.graphics.Color
import com.pukaar.app.ui.screen.contacts.ContactRelation
import com.pukaar.app.ui.screen.protection.ProtectionType

/**
 * One person being set up as a contact.
 *
 * [verified] is what the whole flow turns on: a contact is only counted once the
 * code sent to them has come back, and every "Continue" that matters waits for it.
 */
data class OnboardingContact(
    val id: String,
    val name: String,
    val phone: String,
    val verified: Boolean = false,
    /** Bumped by "Resend code", which restarts the wait for this contact. */
    val resendCount: Int = 0,
    /**
     * How this person is related to the user. Required wherever a contact is
     * named, so it is set on anything the picker produces; nullable only because
     * a half-filled draft has not answered it yet.
     */
    val relation: ContactRelation? = null
)

/**
 * A number the user's contacts can ring for help. Not alerted automatically.
 *
 * [relation] is asked for as the number is saved: a trusted contact ringing on
 * the user's behalf needs to know who they are calling.
 */
data class HelpNumber(
    val id: String,
    val label: String,
    val phone: String,
    val relation: ContactRelation? = null
)

/** How long a quiet phone stays quiet before an inactivity alert fires. */
data class InactivityTiming(
    val durationHours: Int = 12
)

/** The hour choices offered in the timing dropdowns. */
val InactivityHourOptions = listOf(12, 18, 24, 30, 36)

/** Stable-ish colours for the initial avatars, picked from the name. */
private val AvatarPalette = listOf(
    Color(0xFFD93A3A),
    Color(0xFF7C4DBE),
    Color(0xFF2E8B84),
    Color(0xFFC2762A),
    Color(0xFF3A6FD9),
    Color(0xFF9B3A6E)
)

fun avatarColorFor(name: String): Color =
    AvatarPalette[(name.hashCode().mod(AvatarPalette.size))]

/**
 * "Rahul Sharma" becomes "RS"; a single word gives a single letter.
 *
 * First and last rather than the first two, so a middle name does not push the
 * family name out of somebody's avatar.
 */
fun initialsFor(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotEmpty() }

    return when (words.size) {
        0 -> "?"
        1 -> words.first().take(1).uppercase()
        else -> "${words.first().first()}${words.last().first()}".uppercase()
    }
}

/**
 * What a finished flow hands back.
 *
 * One shape for all three so the nav graph has a single completion callback; the
 * fields a given flow does not collect are simply left empty.
 */
data class OnboardingResult(
    val type: ProtectionType,
    /** Elder's name — shown on SOS/inactivity alerts to trusted contacts. */
    val userName: String = "",
    val primaryContacts: List<OnboardingContact> = emptyList(),
    val secondaryContacts: List<OnboardingContact> = emptyList(),
    val helpNumbers: List<HelpNumber> = emptyList(),
    val timing: InactivityTiming? = null
)
