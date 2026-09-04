package com.pukaar.domain.alert;

import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
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

    public boolean sendText(String toPhoneE164, String body) {
        if (!isConfigured()) return false;
        try {
            String phone = toPhoneE164.replace("+", "").trim();
            String url = "https://graph.facebook.com/v19.0/" + props.getAlerts().getWhatsapp().getPhoneNumberId() + "/messages";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getAlerts().getWhatsapp().getToken());

            Map<String, Object> text = Map.of("preview_url", true, "body", body);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", phone);
            payload.put("type", "text");
            payload.put("text", text);

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) log.info("WhatsApp alert sent to {}", toPhoneE164);
            else log.warn("WhatsApp alert failed {} -> {}", toPhoneE164, resp.getBody());
            return ok;
        } catch (Exception e) {
            log.error("WhatsApp send failed for {}", toPhoneE164, e);
            return false;
        }
    }
}
