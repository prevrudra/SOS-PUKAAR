package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pukaar.app.ui.screen.contacts.ContactRelation

/**
 * Resolves a contact's relation to its translated name, outside a composable.
 *
 * The QR payload is built from plain Kotlin — it has to be, since it is also what
 * a saved or shared code would be built from — but the relation names live in
 * resources. This reads them once, in composition, and hands back a lookup the
 * payload builder can call.
 */
@Composable
fun rememberRelationLabel(): (CardContact) -> String? {
    val labels = ContactRelation.entries.associateWith { stringResource(it.labelRes) }
    return { contact -> contact.relation?.let(labels::get) }
}

/**
 * A filled-in card for the previews.
 *
 * The same person as the design, so a preview can be held against the mock-up
 * without translating between two sets of names.
 */
val SampleCardDraft = EmergencyCardDraft(
    personal = PersonalDetails(
        fullName = "Rohan Mehta",
        nationality = "India",
        gender = Gender.MALE,
        bloodGroup = BloodGroup.B_POSITIVE,
        primaryContact = CardContact(
            name = "Anita Mehta",
            relation = ContactRelation.PARENT,
            phone = "+91 98765 43210"
        )
    ),
    identification = IdentificationDetails(
        citizenship = Citizenship.INDIAN,
        aadhaarNumber = "1234 5678 9012",
        passportNumber = "U1234567"
    ),
    medical = MedicalDetails(bloodGroup = BloodGroup.B_POSITIVE),
    travel = TravelDetails(destination = "Singapore")
)
