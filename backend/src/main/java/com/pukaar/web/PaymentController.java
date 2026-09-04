package com.pukaar.web;

import com.pukaar.common.SubscriptionPlan;
import com.pukaar.domain.payment.PaymentService;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/config")
    public Map<String, Object> config() {
        return paymentService.config();
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody CreateOrderRequest req) {
        SubscriptionPlan plan = req.getPlan() == null ? SubscriptionPlan.INDIVIDUAL : req.getPlan();
        return paymentService.createOrder(SecurityUtils.currentUserId(), plan);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody VerifyPaymentRequest req) {
        return paymentService.verifyPayment(
                SecurityUtils.currentUserId(),
                req.getOrderId(),
                req.getPaymentId(),
                req.getSignature()
        );
    }

    @Data
    public static class CreateOrderRequest {
        private SubscriptionPlan plan;
    }

    @Data
    public static class VerifyPaymentRequest {
        private String orderId;
        private String paymentId;
        private String signature;
    }
}
