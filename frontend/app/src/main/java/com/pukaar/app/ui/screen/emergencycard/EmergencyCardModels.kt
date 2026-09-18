package com.pukaar.app.ui.screen.emergencycard

import androidx.annotation.StringRes
import com.pukaar.app.R
import com.pukaar.app.ui.screen.contacts.ContactRelation

/**
 * The five steps of the Emergency Card flow, in order.
 *
 * The stepper at the top of every page is drawn from this, and so is the Review
 * page's list of sections — which means a step cannot appear in one and be
 * forgotten in the other.
 */
enum class EmergencyCardStep(@StringRes val labelRes: Int) {
    PERSONAL(R.string.card_step_personal),
    IDENTIFICATION(R.string.card_step_id),
    MEDICAL(R.string.card_step_medical),
    TRAVEL(R.string.card_step_travel),
    PREVIEW(R.string.card_step_preview);

    val isFirst: Boolean get() = ordinal == 0
    val isLast: Boolean get() = ordinal == entries.lastIndex

    fun next(): EmergencyCardStep = entries[(ordinal + 1).coerceAtMost(entries.lastIndex)]
    fun previous(): EmergencyCardStep = entries[(ordinal - 1).coerceAtLeast(0)]
}

/**
 * The two faces a finished card can wear.
 *
 * The same QR code, the same payload — what differs is how much of it is legible
 * without scanning. [WITH_DETAILS] puts the photo, the blood group and the number
 * to ring in plain sight, which is what actually helps when the person who picks
 * the card up has no phone to hand. [PLAIN] shows nothing but the code, for a
 * sticker on a laptop or a bag tag where the card will be seen by people who are
 * not responding to anything.
 *
 * Both are always generated; this is only which one is being looked at, printed
 * or set as a wallpaper.
 */
enum class CardQrStyle(
    @StringRes val labelRes: Int,
    @StringRes val captionRes: Int,
    /** The line inviting a swipe to the *other* style. */
    @StringRes val swipeToOtherRes: Int
) {
    WITH_DETAILS(
        R.string.card_qr_style_details,
        R.string.card_qr_style_details_caption,
        R.string.card_swipe_to_plain
    ),
    PLAIN(
        R.string.card_qr_style_plain,
        R.string.card_qr_style_plain_caption,
        R.string.card_swipe_to_details
    )
}

/**
 * Which set of identity documents step 2 asks for.
 *
 * An Aadhaar number means nothing for a visitor and a visa expiry means nothing
 * for a resident, so the step swaps its fields wholesale rather than showing one
 * long form with half of it struck through.
 */
enum class Citizenship(@StringRes val labelRes: Int) {
    INDIAN(R.string.card_citizenship_indian),
    FOREIGN(R.string.card_citizenship_foreign)
}

enum class Gender(@StringRes val labelRes: Int) {
    MALE(R.string.gender_male),
    FEMALE(R.string.gender_female),
    OTHER(R.string.gender_other),
    UNDISCLOSED(R.string.gender_undisclosed)
}

/** The eight groups, with the label that goes on the card face. */
enum class BloodGroup(val label: String) {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-")
}

/**
 * One person a responder should ring, as the card carries them.
 *
 * Deliberately not [com.pukaar.app.ui.screen.contacts.ContactUiModel]: those
 * are the people PUKAAR alerts, and they are verified, typed and prioritised.
 * This is a name and a number printed on a card, and it needs none of that.
 */
data class CardContact(
    val name: String = "",
    val relation: ContactRelation? = null,
    val phone: String = ""
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && relation != null && phone.isNotBlank()
}

/**
 * Step 1. The only step with required fields — everything past it is optional,
 * because a half-filled card still helps and an empty one never gets made.
 */
data class PersonalDetails(
    val fullName: String = "",
    val nationality: String = "",
    val dateOfBirth: Long? = null,
    val gender: Gender? = null,
    val bloodGroup: BloodGroup? = null,
    /** A content:// uri from the picker; null until a photo is chosen. */
    val photoUri: String? = null,
    val primaryContact: CardContact = CardContact(),
    val secondaryContact: CardContact? = null
) {
    val isValid: Boolean
        get() = fullName.isNotBlank() && nationality.isNotBlank() && primaryContact.isComplete
}

/** Step 2. Which half is filled depends on [citizenship]; all of it is optional. */
data class IdentificationDetails(
    val citizenship: Citizenship = Citizenship.INDIAN,
    val aadhaarNumber: String = "",
    val passportNumber: String = "",
    val drivingLicenceNumber: String = "",
    val countryOfCitizenship: String = "",
    val visaNumber: String = "",
    val visaExpiry: Long? = null
) {
    /** What the review page lists under this section — only what was filled in. */
    val filledLabels: List<String>
        get() = when (citizenship) {
            Citizenship.INDIAN -> listOf(
                "Aadhaar".takeIf { aadhaarNumber.isNotBlank() },
                "Passport".takeIf { passportNumber.isNotBlank() },
                "Driving Licence".takeIf { drivingLicenceNumber.isNotBlank() }
            )

            Citizenship.FOREIGN -> listOf(
                "Passport".takeIf { passportNumber.isNotBlank() },
                countryOfCitizenship.takeIf { it.isNotBlank() },
                "Visa".takeIf { visaNumber.isNotBlank() }
            )
        }.filterNotNull()
}

/** Step 3. Free text throughout: a responder reads it, nothing parses it. */
data class MedicalDetails(
    val bloodGroup: BloodGroup? = null,
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val instructions: String = ""
) {
    val isEmpty: Boolean
        get() = bloodGroup == null && allergies.isBlank() && conditions.isBlank() &&
            medications.isBlank() && instructions.isBlank()
}

/** Step 4. For a trip; left empty by anyone making a card to keep at home. */
data class TravelDetails(
    val destination: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val accommodation: String = "",
    val localAddress: String = ""
) {
    val isEmpty: Boolean
        get() = destination.isBlank() && startDate == null && endDate == null &&
            accommodation.isBlank() && localAddress.isBlank()
}

/**
 * Everything the five steps collect, and the single thing the card is made from.
 *
 * Held as one value so a step can be revisited from the review page without the
 * others being rebuilt, and so "Edit Card" after generating reopens exactly what
 * was submitted rather than a blank form.
 */
data class EmergencyCardDraft(
    val personal: PersonalDetails = PersonalDetails(),
    val identification: IdentificationDetails = IdentificationDetails(),
    val medical: MedicalDetails = MedicalDetails(),
    val travel: TravelDetails = TravelDetails()
) {
    /** Step 1 is the only gate: the card cannot be generated without it. */
    val canGenerate: Boolean get() = personal.isValid

    /**
     * The group shown on the card face.
     *
     * Asked twice — once with the personal details, once with the medical ones —
     * because people fill in whichever step they think of it on. The medical
     * answer wins, being the more deliberate of the two.
     */
    val bloodGroup: BloodGroup? get() = medical.bloodGroup ?: personal.bloodGroup
}

/** What a printed or saved card comes out as. */
enum class PrintFormat(@StringRes val labelRes: Int) {
    PDF(R.string.card_format_pdf),
    PNG(R.string.card_format_png)
}

/**
 * The paper sizes offered for printing.
 *
 * [MULTIPLE] is not a size but a sheet of all of them, for someone who wants one
 * for the wallet and one for a bag tag off a single print.
 */
enum class CardSize(
    @StringRes val labelRes: Int,
    @StringRes val dimensionsRes: Int,
    val isSheet: Boolean = false
) {
    WALLET(R.string.card_size_wallet, R.string.card_size_wallet_mm),
    STANDARD(R.string.card_size_standard, R.string.card_size_standard_mm),
    A5(R.string.card_size_a5, R.string.card_size_a5_mm),
    MULTIPLE(R.string.card_size_multiple, R.string.card_size_multiple_note, isSheet = true)
}

/** What the user picked on the printing screen. */
data class PrintOptions(
    val format: PrintFormat = PrintFormat.PDF,
    val size: CardSize = CardSize.STANDARD
)
