package com.pukaar.app.util

/**
 * Phone helpers that preserve arbitrary country codes in E.164 form.
 * Bare 10-digit numbers still default to India (+91) for backward compatibility.
 */
object PhoneNumbers {

    data class Country(
        val iso: String,
        val name: String,
        val dialCode: String,
        val nationalLength: IntRange = 6..12
    )

    val countries: List<Country> = listOf(
        Country("IN", "India", "+91", 10..10),
        Country("AE", "UAE", "+971", 8..9),
        Country("US", "United States", "+1", 10..10),
        Country("GB", "United Kingdom", "+44", 9..10),
        Country("SA", "Saudi Arabia", "+966", 8..9),
        Country("BD", "Bangladesh", "+880", 10..10),
        Country("NP", "Nepal", "+977", 10..10),
        Country("LK", "Sri Lanka", "+94", 9..9),
        Country("PK", "Pakistan", "+92", 10..10),
        Country("SG", "Singapore", "+65", 8..8),
        Country("MY", "Malaysia", "+60", 9..10),
        Country("AU", "Australia", "+61", 9..9),
        Country("CA", "Canada", "+1", 10..10),
        Country("QA", "Qatar", "+974", 8..8),
        Country("KW", "Kuwait", "+965", 8..8),
        Country("OM", "Oman", "+968", 8..8),
        Country("BH", "Bahrain", "+973", 8..8)
    )

    fun defaultCountry(): Country = countries.first { it.iso == "IN" }

    fun countryForDialCode(dialCode: String): Country? =
        countries.firstOrNull { it.dialCode == dialCode }

    fun toE164(raw: String): String {
        var p = raw.trim()
            .replace('\u00A0', ' ')
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
        if (p.startsWith("00")) p = "+" + p.substring(2)
        if (p.startsWith("+")) {
            val digits = p.drop(1).filter { it.isDigit() }
            require(digits.length in 8..15) { "Invalid phone number" }
            return "+$digits"
        }
        val digits = p.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "+91$digits"
            digits.length in 8..15 -> "+$digits"
            else -> error("Invalid phone number")
        }
    }

    fun fromParts(dialCode: String, nationalNumber: String): String {
        val code = dialCode.trim().let { if (it.startsWith("+")) it else "+$it" }.filter { it == '+' || it.isDigit() }
        val national = nationalNumber.filter { it.isDigit() }
        require(national.isNotEmpty()) { "Invalid phone number" }
        return toE164("$code$national")
    }

    fun splitE164(e164: String): Pair<String, String> {
        val normalized = runCatching { toE164(e164) }.getOrElse { e164.trim() }
        if (!normalized.startsWith("+")) return "+91" to normalized.filter { it.isDigit() }
        val match = countries
            .sortedByDescending { it.dialCode.length }
            .firstOrNull { normalized.startsWith(it.dialCode) }
        return if (match != null) {
            match.dialCode to normalized.removePrefix(match.dialCode)
        } else {
            // Unknown country: keep first 1-3 digits as dial code heuristic
            val digits = normalized.drop(1)
            when {
                digits.length > 10 -> "+${digits.take(digits.length - 10)}" to digits.takeLast(10)
                else -> "+${digits.take(1)}" to digits.drop(1)
            }
        }
    }

    fun isValidNational(country: Country, national: String): Boolean {
        val len = national.filter { it.isDigit() }.length
        return len in country.nationalLength
    }
}
