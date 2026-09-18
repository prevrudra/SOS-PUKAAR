package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * What the card's QR code carries.
 *
 * Plain text rather than a link: a responder scanning this may be somewhere with
 * no signal, and a URL that will not load is worse than no code at all. Everything
 * a stranger needs is in the code itself, and only what the user actually filled
 * in is included — an empty line reads as missing information rather than as
 * information withheld.
 */
fun emergencyCardPayload(draft: EmergencyCardDraft, relationLabel: (CardContact) -> String?): String {
    val lines = mutableListOf<String>()

    lines += "PUKAAR EMERGENCY CARD"
    lines += "Name: ${draft.personal.fullName}"
    if (draft.personal.nationality.isNotBlank()) {
        lines += "Nationality: ${draft.personal.nationality}"
    }
    draft.bloodGroup?.let { lines += "Blood Group: ${it.label}" }
    draft.personal.dateOfBirth?.let { lines += "DOB: ${formatCardDate(it)}" }

    listOfNotNull(draft.personal.primaryContact, draft.personal.secondaryContact)
        .filter { it.isComplete }
        .forEachIndexed { index, contact ->
            val relation = relationLabel(contact)?.let { " ($it)" } ?: ""
            lines += "Contact ${index + 1}: ${contact.name}$relation ${contact.phone}"
        }

    with(draft.medical) {
        if (allergies.isNotBlank()) lines += "Allergies: $allergies"
        if (conditions.isNotBlank()) lines += "Conditions: $conditions"
        if (medications.isNotBlank()) lines += "Medications: $medications"
        if (instructions.isNotBlank()) lines += "Instructions: $instructions"
    }

    with(draft.identification) {
        if (aadhaarNumber.isNotBlank()) lines += "Aadhaar: $aadhaarNumber"
        if (passportNumber.isNotBlank()) lines += "Passport: $passportNumber"
        if (countryOfCitizenship.isNotBlank()) lines += "Citizen of: $countryOfCitizenship"
        if (visaNumber.isNotBlank()) lines += "Visa: $visaNumber"
        visaExpiry?.let { lines += "Visa expiry: ${formatCardDate(it)}" }
    }

    with(draft.travel) {
        if (destination.isNotBlank()) lines += "Destination: $destination"
        if (accommodation.isNotBlank()) lines += "Staying at: $accommodation"
        if (localAddress.isNotBlank()) lines += "Local address: $localAddress"
        if (startDate != null && endDate != null) {
            lines += "Travelling: ${formatCardDate(startDate)} - ${formatCardDate(endDate)}"
        }
    }

    return lines.joinToString("\n")
}

/**
 * The encoded matrix: true where a module is dark.
 *
 * Encoding is cheap but not free, and the card is drawn on four different
 * screens, so callers hold this in a `remember` keyed on the payload.
 */
fun encodeQr(payload: String): Array<BooleanArray>? = try {
    // Q rather than the default: a card gets creased, wet and photographed at an
    // angle, and a quarter of the code can be lost before it stops scanning.
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q,
        EncodeHintType.MARGIN to 0,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)

    Array(matrix.height) { row ->
        BooleanArray(matrix.width) { column -> matrix.get(column, row) }
    }
} catch (error: Exception) {
    // A payload too long for any version, or an empty one. The card still prints;
    // it just carries no code.
    null
}

/**
 * The QR code, drawn as squares rather than rasterised to a bitmap.
 *
 * A canvas keeps it sharp at any size, which matters when the same code appears
 * thumbnail-sized on the review page and at A5 on the printable one.
 *
 * [logo] punches the PUKAAR mark through the middle. It is safe because the code
 * is encoded at error-correction level Q — a quarter of it can be lost — and the
 * mark covers under a tenth of the area. It is worth doing because a stranger has
 * to decide, in a second, whether an unfamiliar square is worth pointing a camera
 * at; a branded one answers that.
 */
@Composable
fun QrCode(
    payload: String,
    modifier: Modifier = Modifier,
    foreground: Color = Color.Black,
    background: Color = Color.White,
    logo: Boolean = false
) {
    val matrix = remember(payload) { encodeQr(payload) }

    BoxWithConstraints(
        modifier = modifier
            .background(background, RoundedCornerShape(4.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (matrix != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val modules = matrix.size
                // Floored so the code sits on whole pixels: a fractional module
                // width leaves seams between squares that a scanner reads as noise.
                val module = minOf(size.width, size.height) / modules
                val originX = (size.width - module * modules) / 2f
                val originY = (size.height - module * modules) / 2f

                matrix.forEachIndexed { row, columns ->
                    columns.forEachIndexed { column, dark ->
                        if (dark) {
                            drawRect(
                                color = foreground,
                                topLeft = Offset(
                                    originX + column * module,
                                    originY + row * module
                                ),
                                // A hair over one module, so neighbouring squares
                                // meet instead of leaving a light hairline.
                                size = Size(module + 0.5f, module + 0.5f)
                            )
                        }
                    }
                }
            }

            if (logo) {
                // Sized off the box rather than in fixed dp: the same code is drawn
                // at 52dp on a lock screen and at 150dp on the plain card, and the
                // mark has to stay the same proportion of it in both.
                val badge = minOf(maxWidth, maxHeight) * 0.26f
                Box(
                    modifier = Modifier
                        .size(badge)
                        .background(background, RoundedCornerShape(badge * 0.22f))
                        .padding(badge * 0.08f),
                    contentAlignment = Alignment.Center
                ) {
                    CardShieldMark(size = (badge.value * 0.84f).toInt())
                }
            }
        }
    }
}
