package com.pukaar.domain.payment;

import com.pukaar.common.ApiException;
import com.pukaar.common.PaymentOrderStatus;
import com.pukaar.common.SubscriptionPlan;
import com.pukaar.domain.referral.ReferralRepository;
import com.pukaar.domain.subscription.SubscriptionEntity;
import com.pukaar.domain.subscription.SubscriptionService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.razorpay.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentOrderRepository paymentRepo;
    private final RazorpayService razorpay;
    private final SubscriptionService subscriptionService;
    private final UserRepository userRepo;
    private final ReferralRepository referralRepo;

    public Map<String, Object> config() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabled", razorpay.isConfigured());
        m.put("keyId", razorpay.isConfigured() ? razorpay.keyId() : null);
        return m;
    }

    @Transactional
    public Map<String, Object> createOrder(UUID userId, SubscriptionPlan plan) {
        if (!razorpay.isConfigured()) {
            throw new ApiException("RAZORPAY_NOT_CONFIGURED", "Payment gateway is not configured");
        }
        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        if (!user.isMockDrillPassed()) {
            throw new ApiException("MOCK_DRILL_REQUIRED", "Complete mock drill before payment");
        }
        long referrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
        int amountInr = subscriptionService.priceFor(plan, referrals);
        int amountPaise = amountInr * 100;
        String receipt = "pukaar-" + userId.toString().substring(0, 8) + "-" + System.currentTimeMillis();
        Order order = razorpay.createOrder(amountPaise, receipt);

        PaymentOrderEntity entity = PaymentOrderEntity.builder()
                .userId(userId)
                .plan(plan)
                .amountInr(amountInr)
                .amountPaise(amountPaise)
                .razorpayOrderId(order.get("id").toString())
                .status(PaymentOrderStatus.PENDING)
                .build();
        entity = paymentRepo.save(entity);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", entity.getRazorpayOrderId());
        m.put("amount", amountPaise);
        m.put("amountInr", amountInr);
        m.put("currency", "INR");
        m.put("keyId", razorpay.keyId());
        m.put("plan", plan);
        m.put("userName", user.getFullName());
        m.put("userPhone", user.getPhoneE164());
        m.put("description", "PUKAAR " + plan.name() + " plan — 1 year");
        return m;
    }

    @Transactional
    public Map<String, Object> verifyPayment(UUID userId, String orderId, String paymentId, String signature) {
        PaymentOrderEntity order = paymentRepo.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ApiException("ORDER_NOT_FOUND", "Payment order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ApiException("ORDER_FORBIDDEN", "Order does not belong to user");
        }
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return paidResponse(order);
        }
        if (!razorpay.verifySignature(orderId, paymentId, signature)) {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentRepo.save(order);
            throw new ApiException("PAYMENT_INVALID", "Payment signature verification failed");
        }
        return completeOrder(order, paymentId);
    }

    @Transactional
    public void handleWebhookPaymentCaptured(String orderId, String paymentId) {
        paymentRepo.findByRazorpayOrderId(orderId).ifPresent(order -> {
            if (order.getStatus() != PaymentOrderStatus.PAID) {
                completeOrder(order, paymentId);
            }
        });
    }

    private Map<String, Object> completeOrder(PaymentOrderEntity order, String paymentId) {
        SubscriptionEntity sub = subscriptionService.activateFromPayment(
                order.getUserId(), order.getPlan(), paymentId, "RAZORPAY");
        order.setRazorpayPaymentId(paymentId);
        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(Instant.now());
        order.setSubscriptionId(sub.getId());
        paymentRepo.save(order);
        return paidResponse(order);
    }

    private Map<String, Object> paidResponse(PaymentOrderEntity order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "PAID");
        m.put("orderId", order.getRazorpayOrderId());
        m.put("paymentId", order.getRazorpayPaymentId());
        m.put("plan", order.getPlan());
        m.put("amountInr", order.getAmountInr());
        m.put("subscriptionId", order.getSubscriptionId());
        m.put("message", "Payment successful — protection activated");
        return m;
    }
}
