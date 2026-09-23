package com.pukaar.domain.alert;

import com.pukaar.common.PhoneNumbers;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthKeyVoiceSender {
    private final PukaarProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        var voice = props.getAlerts().getVoice();
        return voice.isEnabled()
                && voice.getAuthKey() != null
                && !voice.getAuthKey().isBlank();
    }

    public boolean sendEmergency(String toPhoneE164, String userName) {
        if (!isConfigured()) return false;
        String who = userName != null && !userName.isBlank() ? userName.trim() : "a PUKAAR user";
        String template = props.getAlerts().getVoice().getMessageTemplate();
        String message = template.replace("{userName}", who);
        return send(toPhoneE164, message);
    }

    public boolean send(String toPhoneE164, String voiceMessage) {
        if (!isConfigured() || voiceMessage == null || voiceMessage.isBlank()) return false;
        try {
            var voice = props.getAlerts().getVoice();
            MobileParts parts = splitMobile(toPhoneE164, voice.getCountryCode());
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(voice.getEndpoint())
                    .queryParam("authkey", voice.getAuthKey())
                    .queryParam("mobile", parts.mobile)
                    .queryParam("country_code", parts.countryCode);
            if (voice.getTemplateId() != null && !voice.getTemplateId().isBlank()) {
                builder.queryParam("vid", voice.getTemplateId())
                        .queryParam("name", extractFirstName(voiceMessage, toPhoneE164));
            } else {
                builder.queryParam("voice", voiceMessage);
            }
            String url = builder.build().toUriString();
            String resp = restTemplate.getForObject(url, String.class);
            log.info("AuthKey voice to {} (cc={}, mobile={}) response: {}",
                    toPhoneE164, parts.countryCode, parts.mobile, resp);
            return isSuccessResponse(resp);
        } catch (Exception e) {
            log.error("AuthKey voice failed for {}", toPhoneE164, e);
            return false;
        }
    }

    private static boolean isSuccessResponse(String resp) {
        if (resp == null || resp.isBlank()) return false;
        String lower = resp.toLowerCase();
        return lower.contains("submitted successfully")
                || lower.contains("\"status\":\"success\"")
                || lower.contains("\"status\": \"success\"");
    }

    /**
     * Split E.164 into AuthKey country_code + national mobile.
     * Supports India (+91), NANP (+1), and other international lengths.
     */
    static MobileParts splitMobile(String phoneE164, String defaultCountryCode) {
        String e164;
        try {
            e164 = PhoneNumbers.toE164(phoneE164);
        } catch (Exception e) {
            String digits = phoneE164 == null ? "" : phoneE164.replaceAll("\\D", "");
            String country = defaultCountryCode != null && !defaultCountryCode.isBlank()
                    ? defaultCountryCode.replace("+", "")
                    : "91";
            if (digits.length() > 10) {
                return new MobileParts(digits.substring(0, digits.length() - 10), digits.substring(digits.length() - 10));
            }
            return new MobileParts(country, digits);
        }
        String digits = e164.substring(1);
        String country = defaultCountryCode != null && !defaultCountryCode.isBlank()
                ? defaultCountryCode.replace("+", "")
                : "91";

        // India mobile
        if (digits.startsWith("91") && digits.length() == 12) {
            return new MobileParts("91", digits.substring(2));
        }
        // NANP (+1 + 10 digits)
        if (digits.startsWith("1") && digits.length() == 11) {
            return new MobileParts("1", digits.substring(1));
        }
        // Common 2-digit country codes with remaining national number
        for (String cc : new String[]{"44", "61", "49", "33", "39", "34", "81", "82", "86", "65", "60", "62", "63", "66", "84", "971", "966", "974", "973", "968", "965", "961", "20", "27", "234", "254", "880", "92", "94", "977", "95", "855", "856"}) {
            if (digits.startsWith(cc) && digits.length() > cc.length() + 6) {
                return new MobileParts(cc, digits.substring(cc.length()));
            }
        }
        // Generic: longest CC guess — leave last 8–10 as national when total > 10
        if (digits.length() > 10) {
            int nationalLen = digits.length() >= 12 ? 10 : Math.min(10, digits.length() - 1);
            return new MobileParts(digits.substring(0, digits.length() - nationalLen),
                    digits.substring(digits.length() - nationalLen));
        }
        return new MobileParts(country, digits);
    }

    private static String extractFirstName(String message, String unusedPhone) {
        int from = message.indexOf("from PUKAAR.");
        if (from >= 0) {
            int start = from + "from PUKAAR.".length();
            int end = message.indexOf(" may be", start);
            if (end > start) {
                return message.substring(start, end).trim();
            }
        }
        // Short script: "PUKAAR alert. {userName} needs help..."
        int alert = message.indexOf("PUKAAR alert.");
        if (alert >= 0) {
            int start = alert + "PUKAAR alert.".length();
            int end = message.indexOf(" needs", start);
            if (end > start) {
                return message.substring(start, end).trim();
            }
        }
        return "PUKAAR user";
    }

    record MobileParts(String countryCode, String mobile) {}
}
