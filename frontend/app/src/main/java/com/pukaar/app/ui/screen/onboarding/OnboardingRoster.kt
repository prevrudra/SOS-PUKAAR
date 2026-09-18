package com.pukaar.app.ui.screen.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * A fixed run of numbered positions — "Contact 2" stays "Contact 2" whether or
 * not "Contact 1" is filled, which is what the numbered layouts expect.
 */
class ContactSlots(private val items: SnapshotStateList<OnboardingContact?>) {

    val size: Int get() = items.size

    operator fun get(index: Int): OnboardingContact? = items[index]

    val filled: List<OnboardingContact> get() = items.filterNotNull()

    fun set(index: Int, contact: OnboardingContact?) {
        items[index] = contact
    }

    fun verify(id: String) = update(id) { it.copy(verified = true) }

    fun resend(id: String) = update(id) {
        it.copy(verified = false, resendCount = it.resendCount + 1)
    }

    private fun update(id: String, block: (OnboardingContact) -> OnboardingContact) {
        val index = items.indexOfFirst { it?.id == id }
        if (index >= 0) items[index] = items[index]?.let(block)
    }
}

@Composable
fun rememberContactSlots(count: Int): ContactSlots {
    val items = remember {
        mutableStateListOf<OnboardingContact?>().apply { repeat(count) { add(null) } }
    }
    return remember(items) { ContactSlots(items) }
}

/** A growing list of contacts, capped at [max]. */
class ContactRoster(
    private val items: SnapshotStateList<OnboardingContact>,
    val max: Int
) {
    val contacts: List<OnboardingContact> get() = items

    val isFull: Boolean get() = items.size >= max

    val allVerified: Boolean get() = items.isNotEmpty() && items.all { it.verified }

    fun add(contact: OnboardingContact) {
        if (!isFull) items.add(contact)
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
    }

    fun verify(id: String) = update(id) { it.copy(verified = true) }

    fun resend(id: String) = update(id) {
        it.copy(verified = false, resendCount = it.resendCount + 1)
    }

    private fun update(id: String, block: (OnboardingContact) -> OnboardingContact) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = block(items[index])
    }
}

@Composable
fun rememberContactRoster(max: Int): ContactRoster {
    val items = remember { mutableStateListOf<OnboardingContact>() }
    return remember(items, max) { ContactRoster(items, max) }
}

/** A growing list of help numbers, capped at [max]. */
class HelpNumberList(
    private val items: SnapshotStateList<HelpNumber>,
    val max: Int
) {
    val numbers: List<HelpNumber> get() = items

    val isFull: Boolean get() = items.size >= max

    fun add(number: HelpNumber) {
        if (!isFull) items.add(number)
    }

    fun remove(id: String) {
        items.removeAll { it.id == id }
    }

    fun replace(id: String, number: HelpNumber) {
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = number
    }
}

@Composable
fun rememberHelpNumbers(max: Int): HelpNumberList {
    val items = remember { mutableStateListOf<HelpNumber>() }
    return remember(items, max) { HelpNumberList(items, max) }
}
