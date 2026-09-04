package com.pukaar.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pukaar.domain.payment.PaymentService;
import com.pukaar.domain.payment.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final RazorpayService razorpay;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        if (signature == null || !razorpay.verifyWebhookSignature(payload, signature)) {
            log.warn("Razorpay webhook signature verification failed");
            return ResponseEntity.badRequest().body("invalid signature");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            if ("payment.captured".equals(event)) {
                JsonNode payment = root.path("payload").path("payment").path("entity");
                String orderId = payment.path("order_id").asText();
                String paymentId = payment.path("id").asText();
                if (!orderId.isBlank() && !paymentId.isBlank()) {
                    paymentService.handleWebhookPaymentCaptured(orderId, paymentId);
                }
            }
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Razorpay webhook processing failed", e);
            return ResponseEntity.internalServerError().body("error");
        }
    }
}
