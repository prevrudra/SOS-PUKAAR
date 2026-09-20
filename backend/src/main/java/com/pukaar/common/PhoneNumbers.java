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
            if (!isValidIndianMobile10(digits)) {
                throw new ApiException("INVALID_PHONE", "Indian mobile numbers must be 10 digits starting with 6–9");
            }
            return "+91" + digits;
        }
        if (digits.length() == 9) {
            throw new ApiException("INVALID_PHONE", "Phone number must be 10 digits (not 9)");
        }
        if (digits.length() >= 11 && digits.length() <= 15) {
            // Already includes country calling code (e.g. 9198765…, 447…)
            return "+" + digits;
        }
        throw new ApiException("INVALID_PHONE", "Invalid phone number");
    }

    /** True when the number can receive SMS/WhatsApp/voice (valid E.164, Indian mobile if +91). */
    public static boolean isDeliverable(String raw) {
        try {
            String e164 = toE164(raw);
            if (e164.startsWith("+91")) {
                return isValidIndianMobile10(e164.substring(3));
            }
            String digits = e164.substring(1);
            return digits.length() >= 10 && digits.length() <= 15;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isValidIndianMobile10(String digits) {
        if (digits == null || digits.length() != 10) return false;
        char first = digits.charAt(0);
        return first >= '6' && first <= '9';
    }

    public static String digitsOnly(String e164) {
        return toE164(e164).substring(1);
    }

    /** Meta WhatsApp Cloud API `to` field — digits only, always with country code. */
    public static String forWhatsApp(String raw) {
        return digitsOnly(raw);
    }

    /** Last 10 digits for matching phones stored in mixed formats. */
    public static String last10(String raw) {
        if (raw == null) return "";
        String digits = raw.replaceAll("[^0-9]", "");
        return digits.length() <= 10 ? digits : digits.substring(digits.length() - 10);
    }

    public static boolean sameNumber(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        String la = last10(a);
        String lb = last10(b);
        return la.length() >= 10 && la.equals(lb);
    }
}
