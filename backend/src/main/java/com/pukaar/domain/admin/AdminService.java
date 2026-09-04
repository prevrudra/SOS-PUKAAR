package com.pukaar.domain.admin;

import com.pukaar.common.ApiException;
import com.pukaar.common.PaymentOrderStatus;
import com.pukaar.common.SubscriptionStatus;
import com.pukaar.common.UploadStatus;
import com.pukaar.common.UserRole;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.evidence.AudioSegmentEntity;
import com.pukaar.domain.evidence.AudioSegmentRepository;
import com.pukaar.domain.evidence.EvidenceStorageService;
import com.pukaar.domain.payment.PaymentOrderEntity;
import com.pukaar.domain.payment.PaymentOrderRepository;
import com.pukaar.domain.subscription.SubscriptionEntity;
import com.pukaar.domain.subscription.SubscriptionRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentOrderRepository paymentRepo;
    private final EmergencyEventRepository emergencyRepo;
    private final AudioSegmentRepository audioRepo;
    private final EvidenceStorageService evidenceStorage;

    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", userRepo.count());
        m.put("activeSubscriptions", subscriptionRepo.countByStatus(SubscriptionStatus.ACTIVE));
        m.put("paidOrders", paymentRepo.countByStatus(PaymentOrderStatus.PAID));
        m.put("totalEmergencies", emergencyRepo.count());
        m.put("uploadedRecordings", audioRepo.findByUploadStatusOrderByUploadedAtDesc(UploadStatus.UPLOADED).size());
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

    public Map<String, Object> recordings(UUID userIdFilter, int page, int size) {
        List<AudioSegmentEntity> uploaded = audioRepo.findByUploadStatusOrderByUploadedAtDesc(UploadStatus.UPLOADED);
        Map<UUID, EmergencyEventEntity> events = emergencyRepo.findAllById(
                uploaded.stream().map(AudioSegmentEntity::getEventId).distinct().toList()
        ).stream().collect(Collectors.toMap(EmergencyEventEntity::getId, e -> e));

        List<Map<String, Object>> rows = uploaded.stream()
                .map(seg -> {
                    EmergencyEventEntity event = events.get(seg.getEventId());
                    if (event == null) return null;
                    if (userIdFilter != null && !event.getUserId().equals(userIdFilter)) return null;
                    UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("segmentId", seg.getId());
                    row.put("eventId", event.getId());
                    row.put("userId", event.getUserId());
                    row.put("userPhone", user != null ? user.getPhoneE164() : null);
                    row.put("userName", user != null ? user.getFullName() : null);
                    row.put("triggerType", event.getTriggerType());
                    row.put("mockDrill", event.isMockDrill());
                    row.put("index", seg.getSegmentIndex());
                    row.put("durationSec", seg.getDurationSec());
                    row.put("byteSize", seg.getByteSize());
                    row.put("uploadedAt", seg.getUploadedAt());
                    row.put("startedAt", event.getStartedAt());
                    row.put("playUrl", "/api/v1/admin/recordings/" + seg.getId() + "/content");
                    return row;
                })
                .filter(r -> r != null)
                .toList();

        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("page", page);
        m.put("size", size);
        m.put("total", rows.size());
        m.put("items", rows.subList(from, to));
        return m;
    }

    public Resource streamRecording(UUID segmentId) {
        AudioSegmentEntity segment = audioRepo.findById(segmentId)
                .orElseThrow(() -> new ApiException("SEGMENT_NOT_FOUND", "Audio segment not found"));
        if (segment.getUploadStatus() != UploadStatus.UPLOADED || segment.getStorageKey() == null) {
            throw new ApiException("NOT_UPLOADED", "Recording is not available");
        }
        return evidenceStorage.asResource(segment.getStorageKey());
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
