package com.pukaar.common;

/**
 * Normalizes phone numbers to E.164 without forcing a single country.
 * Bare 10-digit numbers default to India (+91) for backward compatibility.
 */
public final class PhoneNumbers {
    private PhoneNumbers() {}

    public static String toE164(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ApiException("INVALID_PHONE", "Phone number is required");
        }
        String p = raw.trim()
                .replace('\u00A0', ' ')
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "");
        if (p.startsWith("00")) {
            p = "+" + p.substring(2);
        }
        if (p.startsWith("+")) {
            String digits = p.substring(1).replaceAll("\\D", "");
            if (digits.length() < 8 || digits.length() > 15) {
                throw new ApiException("INVALID_PHONE", "Invalid phone number");
            }
            return "+" + digits;
        }
        String digits = p.replaceAll("\\D", "");
        if (digits.length() == 10) {
            // Legacy India local numbers without country code
            return "+91" + digits;
        }
        if (digits.length() >= 8 && digits.length() <= 15) {
            // Already includes country calling code (e.g. 97150…, 447…)
            return "+" + digits;
        }
        throw new ApiException("INVALID_PHONE", "Invalid phone number");
    }

    public static String digitsOnly(String e164) {
        return toE164(e164).substring(1);
    }
}
