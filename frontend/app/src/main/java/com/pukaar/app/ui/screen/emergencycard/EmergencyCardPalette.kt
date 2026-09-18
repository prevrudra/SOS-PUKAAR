package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.ui.graphics.Color

/**
 * The one light corner of a black app.
 *
 * Every other screen is an alert, read in the dark and in a hurry. The Emergency
 * Card is a document: filled in calmly, printed onto white paper, and held up for
 * a stranger to read. It is designed as paper for the same reason a boarding pass
 * is, and the palette lives here rather than in the app theme so nothing else can
 * pick it up by accident.
 */
object CardPalette {
    /** The page behind everything. */
    val Background = Color(0xFFF6F7F9)

    /** Cards, fields and the card face itself. */
    val Surface = Color(0xFFFFFFFF)

    /** Field fill, one step down from [Surface] so an input reads as an input. */
    val Field = Color(0xFFFFFFFF)

    val Border = Color(0xFFE3E6EB)
    val BorderStrong = Color(0xFFCBD2DB)

    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val TextTertiary = Color(0xFF9CA3AF)

    /** The brand red, unchanged from the dark screens — it is the through-line. */
    val Accent = Color(0xFFE01E26)

    /** A pale red wash, for the emergency-contact block and the required marks. */
    val AccentWash = Color(0xFFFEF2F3)

    /** The muted panel the privacy and printing notes sit in. */
    val Note = Color(0xFFF1F4F8)

    val Success = Color(0xFF16A34A)
}
