package com.pukaar.domain.alert;

import com.pukaar.common.PhoneNumbers;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppAlertSender {
    private final PukaarProperties props;
    private final RestTemplate restTemplate = createClient();

    private static RestTemplate createClient() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8_000);
        factory.setReadTimeout(15_000);
        return new RestTemplate(factory);
    }

    public boolean isConfigured() {
        var wa = props.getAlerts().getWhatsapp();
        return wa.getToken() != null && !wa.getToken().isBlank()
                && wa.getPhoneNumberId() != null && !wa.getPhoneNumberId().isBlank();
    }

    /**
     * Sends the configured WhatsApp SOS template once.
     * Do not retry here — Meta may accept the message while returning a transient
     * error, and retries produced 3–4 duplicate SOS WhatsApps per contact.
     */
    public boolean sendEmergencyTemplate(String toPhoneE164, List<String> bodyParams) {
        var wa = props.getAlerts().getWhatsapp();
        String templateName = wa.getTemplateName() == null || wa.getTemplateName().isBlank()
                ? "emergency" : wa.getTemplateName();
        int expected = switch (templateName.toLowerCase(java.util.Locale.ROOT)) {
            case "alert" -> 32;
            case "pukaar_sos" -> 18;
            default -> 19; // legacy "emergency"
        };
        return sendNamedTemplate(toPhoneE164, templateName, wa.getTemplateLanguage(), bodyParams, expected);
    }

    /** Legacy approved template "emergency" (19 body variables). */
    public boolean sendLegacyEmergencyTemplate(String toPhoneE164, List<String> bodyParams) {
        var wa = props.getAlerts().getWhatsapp();
        return sendNamedTemplate(
                toPhoneE164,
                "emergency",
                wa.getTemplateLanguage(),
                bodyParams,
                19
        );
    }

    public boolean hasSafeTemplate() {
        String name = props.getAlerts().getWhatsapp().getSafeTemplateName();
        return name != null && !name.isBlank();
    }

    /** I'm Safe — WHATSAPP_SAFE_TEMPLATE_NAME (e.g. safe = 4 vars, pukaar_safe = 2 vars). */
    public boolean sendSafeTemplate(String toPhoneE164, String userName, String mapsLink, String closedAtIst) {
        var wa = props.getAlerts().getWhatsapp();
        String templateName = wa.getSafeTemplateName();
        if (templateName == null || templateName.isBlank()) return false;
        String lang = wa.getSafeTemplateLanguage() == null || wa.getSafeTemplateLanguage().isBlank()
                ? "en" : wa.getSafeTemplateLanguage();
        List<String> params;
        int expected;
        if ("safe".equalsIgnoreCase(templateName)) {
            params = List.of(userName, userName, mapsLink == null || mapsLink.isBlank() ? "-" : mapsLink, closedAtIst);
            expected = 4;
        } else {
            params = List.of(userName, closedAtIst);
            expected = 2;
        }
        return sendNamedTemplate(toPhoneE164, templateName, lang, params, expected);
    }

    /** @deprecated use {@link #sendSafeTemplate(String, String, String, String)} */
    public boolean sendSafeTemplate(String toPhoneE164, String userName, String closedAtIst) {
        return sendSafeTemplate(toPhoneE164, userName, "-", closedAtIst);
    }

    private boolean sendNamedTemplate(
            String toPhoneE164,
            String templateName,
            String languageCode,
            List<String> bodyParams,
            int expectedParams
    ) {
        if (!isConfigured()) return false;
        try {
            String phone = PhoneNumbers.forWhatsApp(toPhoneE164);
            var wa = props.getAlerts().getWhatsapp();
            String url = "https://graph.facebook.com/v26.0/" + wa.getPhoneNumberId() + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(wa.getToken());

            List<Map<String, Object>> parameters = new ArrayList<>();
            for (String p : padParams(bodyParams, expectedParams)) {
                Map<String, Object> param = new LinkedHashMap<>();
                param.put("type", "text");
                param.put("text", sanitize(p));
                parameters.add(param);
            }

            Map<String, Object> bodyComponent = new LinkedHashMap<>();
            bodyComponent.put("type", "body");
            bodyComponent.put("parameters", parameters);

            Map<String, Object> template = new LinkedHashMap<>();
            template.put("name", templateName);
            template.put("language", Map.of("code", languageCode == null || languageCode.isBlank()
                    ? "en" : languageCode));
            template.put("components", List.of(bodyComponent));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", phone);
            payload.put("type", "template");
            payload.put("template", template);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("WhatsApp emergency template sent to {} — meta: {}", phone, resp.getBody());
            } else {
                log.warn("WhatsApp template failed {} -> {}", phone, resp.getBody());
            }
            return ok;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("WhatsApp template HTTP {} for {}: {}",
                    e.getStatusCode().value(), toPhoneE164, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("WhatsApp template send failed for {}", toPhoneE164, e);
            return false;
        }
    }

    /** Native WhatsApp location pin — works in the post-SOS 24h session window. */
    public boolean sendLocation(String toPhoneE164, double lat, double lng, String name, String address) {
        if (!isConfigured()) return false;
        try {
            String phone = PhoneNumbers.forWhatsApp(toPhoneE164);
            var wa = props.getAlerts().getWhatsapp();
            String url = "https://graph.facebook.com/v26.0/" + wa.getPhoneNumberId() + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(wa.getToken());

            Map<String, Object> location = new LinkedHashMap<>();
            location.put("latitude", lat);
            location.put("longitude", lng);
            location.put("name", sanitize(name));
            location.put("address", sanitize(address));

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", phone);
            payload.put("type", "location");
            payload.put("location", location);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("WhatsApp location sent to {} — meta: {}", phone, resp.getBody());
            } else {
                log.warn("WhatsApp location failed {} -> {}", phone, resp.getBody());
            }
            return ok;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("WhatsApp location HTTP {} for {}: {}",
                    e.getStatusCode().value(), toPhoneE164, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("WhatsApp location send failed for {}", toPhoneE164, e);
            return false;
        }
    }

    public boolean sendText(String toPhoneE164, String body) {
        if (!isConfigured()) return false;
        try {
            String phone = PhoneNumbers.forWhatsApp(toPhoneE164);
            var wa = props.getAlerts().getWhatsapp();
            String url = "https://graph.facebook.com/v26.0/" + wa.getPhoneNumberId() + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(wa.getToken());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", phone);
            payload.put("type", "text");
            payload.put("text", Map.of("preview_url", true, "body", body));

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) {
                log.info("WhatsApp text sent to {} — meta: {}", phone, resp.getBody());
            } else {
                log.warn("WhatsApp text failed {} -> {}", phone, resp.getBody());
            }
            return ok;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("WhatsApp text HTTP {} for {}: {}",
                    e.getStatusCode().value(), toPhoneE164, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("WhatsApp text send failed for {}", toPhoneE164, e);
            return false;
        }
    }

    private static List<String> padParams(List<String> in, int n) {
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            out.add(i < in.size() && in.get(i) != null && !in.get(i).isBlank() ? in.get(i) : "-");
        }
        return out;
    }

    /** WhatsApp template vars cannot be empty and have length limits. */
    private static String sanitize(String raw) {
        String s = raw == null ? "-" : raw.trim().replace('\n', ' ').replace('\r', ' ');
        if (s.isBlank()) s = "-";
        // Keep template vars short so Meta body stays ≤ 1024 (#132005). Full text is in follow-up.
        if (s.length() > 60) s = s.substring(0, 59) + "…";
        return s;
    }
}
