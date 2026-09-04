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
public class FcmPushSender {
    private final PukaarProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return props.getAlerts().getFcm().getServerKey() != null
                && !props.getAlerts().getFcm().getServerKey().isBlank();
    }

    public boolean sendHighPriority(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isConfigured() || fcmToken == null || fcmToken.isBlank()) return false;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + props.getAlerts().getFcm().getServerKey());

            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");
            notification.put("priority", "high");

            Map<String, Object> android = Map.of("priority", "high");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", fcmToken);
            payload.put("priority", "high");
            payload.put("notification", notification);
            payload.put("android", android);
            if (data != null && !data.isEmpty()) payload.put("data", data);

            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://fcm.googleapis.com/fcm/send",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) log.info("FCM high-priority alert sent");
            else log.warn("FCM failed: {}", resp.getBody());
            return ok;
        } catch (Exception e) {
            log.error("FCM send failed", e);
            return false;
        }
    }
}
