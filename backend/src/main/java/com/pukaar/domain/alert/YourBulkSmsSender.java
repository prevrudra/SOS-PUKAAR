package com.pukaar.domain.alert;

import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class YourBulkSmsSender {
    private final PukaarProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        var sms = props.getAlerts().getSms();
        return sms.getAuthKey() != null && !sms.getAuthKey().isBlank()
                && sms.getDltTeId() != null && !sms.getDltTeId().isBlank();
    }

    public boolean send(String toPhoneE164, String message) {
        if (!isConfigured()) return false;
        try {
            var sms = props.getAlerts().getSms();
            String url = UriComponentsBuilder
                    .fromHttpUrl(sms.getEndpoint())
                    .queryParam("authkey", sms.getAuthKey())
                    .queryParam("mobiles", formatMobile(toPhoneE164))
                    .queryParam("message", message)
                    .queryParam("sender", sms.getSenderId())
                    .queryParam("route", sms.getRoute())
                    .queryParam("country", sms.getCountry())
                    .queryParam("DLT_TE_ID", sms.getDltTeId())
                    .build()
                    .toUriString();
            String resp = restTemplate.getForObject(url, String.class);
            log.info("YourBulkSms to {} response: {}", toPhoneE164, resp);
            return isSuccessResponse(resp);
        } catch (Exception e) {
            log.error("YourBulkSms failed for {}", toPhoneE164, e);
            return false;
        }
    }

    public boolean sendOtp(String toPhoneE164, String otp) {
        String template = props.getAlerts().getSms().getOtpTemplate();
        String message = template.replace("{#var#}", otp);
        return send(toPhoneE164, message);
    }

    private boolean isSuccessResponse(String resp) {
        if (resp == null || resp.isBlank()) return false;
        String lower = resp.toLowerCase();
        if (lower.contains("\"status\":\"error\"") || lower.contains("\"status\": \"error\"")) {
            return false;
        }
        return lower.contains("\"status\":\"success\"")
                || lower.contains("\"status\": \"success\"")
                || lower.contains("\"code\":\"000\"")
                || lower.contains("\"code\": \"000\"")
                || lower.contains("sms submission has been accepted");
    }

    private String formatMobile(String phoneE164) {
        String digits = phoneE164.replace("+", "").replaceAll("\\D", "");
        // Existing India DLT route expects 10-digit local numbers.
        if (digits.startsWith("91") && digits.length() == 12) {
            return digits.substring(2);
        }
        return digits;
    }
}
