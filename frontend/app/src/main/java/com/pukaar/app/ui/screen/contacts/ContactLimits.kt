package com.pukaar.app.ui.screen.contacts

/** Product limits for trusted contacts that receive alerts. */
const val MaxTrustedContactsPerCategory = 3

/** Inactivity trusted contacts (HELP_BACKUP) — max 2. */
const val MaxInactivityTrustedContacts = 2

fun maxTrustedFor(type: ContactType): Int = when (type) {
    ContactType.INACTIVITY -> MaxInactivityTrustedContacts
    else -> MaxTrustedContactsPerCategory
}
