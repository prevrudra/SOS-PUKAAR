package com.pukaar.domain.alert;

import com.google.auth.oauth2.GoogleCredentials;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data-only high-priority FCM so High Alert's onMessageReceived always runs
 * (notification+data payloads are swallowed by the system tray when app is backgrounded).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushSender {
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final PukaarProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private volatile GoogleCredentials v1Credentials;

    public boolean isConfigured() {
        return hasLegacyKey() || hasServiceAccount();
    }

    public boolean sendHighPriority(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) return false;
        Map<String, String> dataPayload = data == null ? new LinkedHashMap<>() : new LinkedHashMap<>(data);
        dataPayload.putIfAbsent("type", "EMERGENCY_ALERT");
        dataPayload.putIfAbsent("title", title);
        dataPayload.putIfAbsent("body", body);

        if (hasServiceAccount() && sendV1(fcmToken, dataPayload)) {
            return true;
        }
        if (hasLegacyKey()) {
            return sendLegacy(fcmToken, dataPayload);
        }
        return false;
    }

    private boolean sendV1(String fcmToken, Map<String, String> dataPayload) {
        try {
            String token = accessToken();
            if (token == null) return false;

            String projectId = props.getAlerts().getFcm().getProjectId();
            String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

            Map<String, Object> android = new LinkedHashMap<>();
            android.put("priority", "HIGH");
            android.put("ttl", "86400s");
            android.put("direct_boot_ok", true);

            Map<String, Object> message = new LinkedHashMap<>();
            message.put("token", fcmToken);
            message.put("data", dataPayload);
            message.put("android", android);

            Map<String, Object> payload = Map.of("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) log.info("FCM v1 data-only high-priority alert sent");
            else log.warn("FCM v1 failed: {}", resp.getBody());
            return ok;
        } catch (Exception e) {
            log.error("FCM v1 send failed", e);
            return false;
        }
    }

    private boolean sendLegacy(String fcmToken, Map<String, String> dataPayload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + props.getAlerts().getFcm().getServerKey());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to", fcmToken);
            payload.put("priority", "high");
            payload.put("content_available", true);
            payload.put("data", dataPayload);
            if (dataPayload.containsKey("eventId")) {
                payload.put("collapse_key", dataPayload.get("eventId"));
            }

            ResponseEntity<String> resp = restTemplate.exchange(
                    "https://fcm.googleapis.com/fcm/send",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) log.info("FCM legacy data-only high-priority alert sent");
            else log.warn("FCM legacy failed: {}", resp.getBody());
            return ok;
        } catch (Exception e) {
            log.error("FCM legacy send failed", e);
            return false;
        }
    }

    private String accessToken() {
        try {
            GoogleCredentials creds = credentials();
            if (creds == null) return null;
            creds.refreshIfExpired();
            return creds.getAccessToken() != null ? creds.getAccessToken().getTokenValue() : null;
        } catch (Exception e) {
            log.error("FCM access token failed", e);
            return null;
        }
    }

    private GoogleCredentials credentials() throws Exception {
        if (v1Credentials != null) return v1Credentials;
        String path = props.getAlerts().getFcm().getServiceAccountPath();
        if (path == null || path.isBlank() || !Files.isRegularFile(Path.of(path))) {
            return null;
        }
        try (FileInputStream in = new FileInputStream(path)) {
            v1Credentials = GoogleCredentials.fromStream(in).createScoped(FCM_SCOPE);
            return v1Credentials;
        }
    }

    private boolean hasLegacyKey() {
        return props.getAlerts().getFcm().getServerKey() != null
                && !props.getAlerts().getFcm().getServerKey().isBlank();
    }

    private boolean hasServiceAccount() {
        String path = props.getAlerts().getFcm().getServiceAccountPath();
        return path != null && !path.isBlank() && Files.isRegularFile(Path.of(path));
    }
}
