package com.pukaar.web;

import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Meta WhatsApp Cloud API webhook — configure in Meta Business Manager:
 * Callback URL: https://pukaaralert.com/pukaar/api/v1/whatsapp/webhook
 * Verify token: WHATSAPP_WEBHOOK_VERIFY_TOKEN from deploy/.env
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {
    private final PukaarProperties props;

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        String expected = props.getAlerts().getWhatsapp().getWebhookVerifyToken();
        if ("subscribe".equals(mode) && expected != null && !expected.isBlank()
                && expected.equals(token)) {
            log.info("WhatsApp webhook verified");
            return ResponseEntity.ok(challenge == null ? "" : challenge);
        }
        log.warn("WhatsApp webhook verify failed mode={} tokenMatch={}", mode,
                expected != null && expected.equals(token));
        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> receive(@RequestBody Map<String, Object> body) {
        log.info("WhatsApp webhook payload: {}", body);
        // Delivery/read receipts can be parsed here when Meta template statuses are needed.
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
