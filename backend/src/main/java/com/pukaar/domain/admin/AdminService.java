package com.pukaar.domain.admin;

import com.pukaar.common.PaymentOrderStatus;
import com.pukaar.common.SubscriptionStatus;
import com.pukaar.common.UserRole;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.payment.PaymentOrderEntity;
import com.pukaar.domain.payment.PaymentOrderRepository;
import com.pukaar.domain.subscription.SubscriptionEntity;
import com.pukaar.domain.subscription.SubscriptionRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentOrderRepository paymentRepo;
    private final EmergencyEventRepository emergencyRepo;

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("activeSubscriptions", subscriptionRepo.countByStatus(SubscriptionStatus.ACTIVE));
        m.put("paidOrders", paymentRepo.countByStatus(PaymentOrderStatus.PAID));
        m.put("totalEmergencies", emergencyRepo.count());
        m.put("revenueInr", paymentRepo.findAll().stream()
                .filter(p -> p.getStatus() == PaymentOrderStatus.PAID)
                .mapToInt(PaymentOrderEntity::getAmountInr)
                .sum());
        return m;
    }

    public Map<String, Object> users(int page, int size) {
        Page<UserEntity> users = userRepo.findAll(PageRequest.of(page, size));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("page", page);
        m.put("size", size);
        m.put("total", users.getTotalElements());
        m.put("items", users.getContent().stream().map(this::userRow).toList());
        return m;
    }

    public Map<String, Object> subscriptions(int page, int size) {
        Page<SubscriptionEntity> subs = subscriptionRepo.findAll(PageRequest.of(page, size));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("page", page);
        m.put("size", size);
        m.put("total", subs.getTotalElements());
        m.put("items", subs.getContent().stream().map(this::subRow).toList());
        return m;
    }

    public Map<String, Object> payments(int page, int size) {
        Page<PaymentOrderEntity> orders = paymentRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("page", page);
        m.put("size", size);
        m.put("total", orders.getTotalElements());
        m.put("items", orders.getContent().stream().map(this::paymentRow).toList());
        return m;
    }

    public Map<String, Object> setUserRole(UUID userId, UserRole role) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        user.setRole(role);
        userRepo.save(user);
        return userRow(user);
    }

    private Map<String, Object> userRow(UserEntity u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("phone", u.getPhoneE164());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole());
        m.put("protectionReady", u.isProtectionReady());
        m.put("mockDrillPassed", u.isMockDrillPassed());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }

    private Map<String, Object> subRow(SubscriptionEntity s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("userId", s.getUserId());
        m.put("plan", s.getPlan());
        m.put("status", s.getStatus());
        m.put("priceInr", s.getPriceInr());
        m.put("endsAt", s.getEndsAt());
        m.put("storePlatform", s.getStorePlatform());
        return m;
    }

    private Map<String, Object> paymentRow(PaymentOrderEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        m.put("plan", p.getPlan());
        m.put("amountInr", p.getAmountInr());
        m.put("status", p.getStatus());
        m.put("razorpayOrderId", p.getRazorpayOrderId());
        m.put("razorpayPaymentId", p.getRazorpayPaymentId());
        m.put("createdAt", p.getCreatedAt());
        m.put("paidAt", p.getPaidAt());
        return m;
    }
}
