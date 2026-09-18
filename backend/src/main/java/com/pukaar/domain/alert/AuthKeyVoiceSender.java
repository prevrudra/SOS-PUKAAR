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
                        .queryParam("name", extractFirstName(voiceMessage));
            } else {
                builder.queryParam("voice", voiceMessage);
            }
            String url = builder.build().toUriString();
            String resp = restTemplate.getForObject(url, String.class);
            log.info("AuthKey voice to {} response: {}", toPhoneE164, resp);
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

    private static MobileParts splitMobile(String phoneE164, String defaultCountryCode) {
        String digits = PhoneNumbers.digitsOnly(phoneE164);
        String country = defaultCountryCode != null && !defaultCountryCode.isBlank()
                ? defaultCountryCode.replace("+", "")
                : "91";
        if (digits.startsWith("91") && digits.length() == 12) {
            return new MobileParts("91", digits.substring(2));
        }
        if (digits.length() > 10) {
            return new MobileParts(digits.substring(0, digits.length() - 10), digits.substring(digits.length() - 10));
        }
        return new MobileParts(country, digits);
    }

    private static String extractFirstName(String message) {
        // Best-effort for template APIs that expect a name parameter.
        int from = message.indexOf("from PUKAAR.");
        if (from >= 0) {
            int start = from + "from PUKAAR.".length();
            int end = message.indexOf(" may be", start);
            if (end > start) {
                return message.substring(start, end).trim();
            }
        }
        return "PUKAAR user";
    }

    private record MobileParts(String countryCode, String mobile) {}
}
