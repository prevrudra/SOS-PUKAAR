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
    private final RestTemplate restTemplate = new RestTemplate();

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
        int expected = "pukaar_sos".equalsIgnoreCase(templateName) ? 18 : 19;
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

    /** I'm Safe closure — uses WHATSAPP_SAFE_TEMPLATE_NAME when set (e.g. pukaar_safe, 2 body vars). */
    public boolean sendSafeTemplate(String toPhoneE164, String userName, String closedAtIst) {
        var wa = props.getAlerts().getWhatsapp();
        String templateName = wa.getSafeTemplateName();
        if (templateName == null || templateName.isBlank()) return false;
        String lang = wa.getSafeTemplateLanguage() == null || wa.getSafeTemplateLanguage().isBlank()
                ? "en" : wa.getSafeTemplateLanguage();
        return sendNamedTemplate(toPhoneE164, templateName, lang,
                List.of(userName, closedAtIst), 2);
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
        String s = raw == null ? "-" : raw.trim();
        if (s.isBlank()) s = "-";
        if (s.length() > 1024) s = s.substring(0, 1024);
        return s;
    }
}
