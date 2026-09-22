package com.pukaar.domain.admin;

import com.pukaar.common.ApiException;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.common.PaymentOrderStatus;
import com.pukaar.domain.alert.AuthKeyVoiceSender;
import com.pukaar.domain.alert.WhatsAppAlertSender;
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
import com.pukaar.domain.elderly.ElderlySettingsEntity;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.elderly.InactivityService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter IST_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final PaymentOrderRepository paymentRepo;
    private final EmergencyEventRepository emergencyRepo;
    private final AudioSegmentRepository audioRepo;
    private final EvidenceStorageService evidenceStorage;
    private final AuthKeyVoiceSender voiceSender;
    private final WhatsAppAlertSender whatsAppSender;
    private final ElderlySettingsRepository elderlySettingsRepo;
    private final InactivityService inactivityService;

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
                    row.put("uploadedAt", formatIst(seg.getUploadedAt()));
                    row.put("startedAt", formatIst(event.getStartedAt()));
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

    public Map<String, Object> testVoiceCall(String phone, String userName) {
        if (!voiceSender.isConfigured()) {
            throw new ApiException("VOICE_NOT_CONFIGURED", "AuthKey voice is not enabled on this server");
        }
        String normalized = PhoneNumbers.toE164(phone);
        String who = userName != null && !userName.isBlank() ? userName.trim() : "Test User";
        boolean ok = voiceSender.sendEmergency(normalized, who);
        if (!ok) {
            throw new ApiException("VOICE_FAILED", "AuthKey voice request failed");
        }
        return Map.of("phone", normalized, "userName", who, "status", "SUBMITTED");
    }

    /**
     * Send all types of WhatsApp messages for testing.
     * @param phone Phone number to send to
     * @param userName Test user name for templates
     * @param types Comma-separated list of message types: alert, safe, location, text (or "all")
     */
    public Map<String, Object> testWhatsApp(String phone, String userName, String types) {
        if (!whatsAppSender.isConfigured()) {
            throw new ApiException("WHATSAPP_NOT_CONFIGURED", "WhatsApp is not enabled on this server");
        }
        String normalized = PhoneNumbers.toE164(phone);
        String who = userName != null && !userName.isBlank() ? userName.trim() : "Test User";
        String now = formatIst(Instant.now());
        double testLat = 12.9716;  // Bangalore
        double testLng = 77.5946;
        String mapsLink = String.format(java.util.Locale.US, "https://maps.google.com/?q=%.6f,%.6f", testLat, testLng);

        Map<String, Object> results = new LinkedHashMap<>();
        results.put("phone", normalized);
        results.put("userName", who);

        String typeList = types == null || types.isBlank() ? "all" : types.toLowerCase();
        boolean all = typeList.equals("all");

        // 1. Alert template (32 params)
        if (all || typeList.contains("alert")) {
            List<String> alertParams = List.of(
                    who, now, // 1-2: name, time
                    "Test Address, Mumbai, MH", mapsLink, // 3-4: address, maps
                    "75%", "Good (4G)", // 5-6: battery, network
                    "TEST_CONTACT_1", "Friend", "+919999999001", // 7-9: contact1
                    "TEST_CONTACT_2", "Family", "+919999999002", // 10-12: contact2
                    "TEST_CONTACT_3", "Friend", "+919999999003", // 13-15: contact3
                    "-", "-", "-", // 16-18: help1
                    "-", "-", "-", // 19-21: help2
                    "-", "-", "-", // 22-24: help3
                    "Test Police", "100", // 25-26: police
                    "Test Ambulance", "108", // 27-28: ambulance
                    "Test Hospital", "+91-22-12345678", // 29-30: hospital
                    "100", // 31: emergency number
                    who  // 32: name again
            );
            boolean alertOk = whatsAppSender.sendEmergencyTemplate(normalized, alertParams);
            results.put("alert_template", alertOk ? "SENT" : "FAILED");
        }

        // 2. Safe template (4 params)
        if (all || typeList.contains("safe")) {
            boolean safeOk = whatsAppSender.sendSafeTemplate(normalized, who, mapsLink, now);
            results.put("safe_template", safeOk ? "SENT" : "FAILED");
        }

        // 3. Location pin
        if (all || typeList.contains("location")) {
            boolean locOk = whatsAppSender.sendLocation(normalized, testLat, testLng, who + " live location", mapsLink);
            results.put("location_pin", locOk ? "SENT" : "FAILED");
        }

        // 4. Plain text
        if (all || typeList.contains("text")) {
            String textMsg = "🚨 PUKAAR TEST MESSAGE\n\n" + who + " is testing WhatsApp alerts.\n\n" +
                    "📍 Location: " + mapsLink + "\n" +
                    "🕐 Time: " + now + "\n\n" +
                    "This is a test message from PUKAAR.";
            boolean textOk = whatsAppSender.sendText(normalized, textMsg);
            results.put("text_message", textOk ? "SENT" : "FAILED");
        }

        return results;
    }

    private static String formatIst(Instant instant) {
        if (instant == null) return null;
        return IST_FMT.format(instant.atZone(IST)) + " IST";
    }

    // ────────────────────────────────────────────────────────────────────────────
    // INACTIVITY TESTING
    // ────────────────────────────────────────────────────────────────────────────

    /**
     * Admin: set inactivity duration to ANY value for testing (bypasses 12/18/24/30/36 restriction).
     * Accepts minutes (converted to hours stored in DB). Min 1 minute.
     */
    public Map<String, Object> setInactivityDuration(UUID userId, int minutes, boolean enable) {
        if (minutes < 1) throw new ApiException("INVALID_DURATION", "Duration must be at least 1 minute");
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));

        ElderlySettingsEntity settings = elderlySettingsRepo.findById(userId)
                .orElseGet(() -> elderlySettingsRepo.save(ElderlySettingsEntity.builder().userId(userId).build()));

        // Store as fractional hours for sub-hour testing (DB column is int, so multiply by 60 and divide)
        // Actually the DB stores hours — for testing, we'll use a special column or convert.
        // Simplest: add durationMinutes column OR just allow very small hours (treat 1 hour = 60 min).
        // For now, store actual hours rounded, but also set lastActivityAt to trigger sooner.
        int hours = Math.max(1, (int) Math.ceil(minutes / 60.0));
        settings.setDurationHours(hours);
        settings.setSoftHours(hours);
        settings.setMediumHours(hours);
        settings.setUrgentHours(hours);
        settings.setInactivityMonitoringEnabled(enable);
        elderlySettingsRepo.save(settings);

        // If testing with < 1 hour, backdate lastActivityAt to trigger alert soon
        if (minutes < 60) {
            Instant backdatedActivity = Instant.now().minusSeconds((long) hours * 3600 - minutes * 60);
            user.setLastActivityAt(backdatedActivity);
            userRepo.save(user);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("phone", user.getPhoneE164());
        result.put("fullName", user.getFullName());
        result.put("durationHours", hours);
        result.put("durationMinutesRequested", minutes);
        result.put("inactivityMonitoringEnabled", enable);
        result.put("lastActivityAt", formatIst(user.getLastActivityAt()));
        result.put("note", minutes < 60
                ? "lastActivityAt backdated so alert triggers in ~" + minutes + " minutes"
                : "Duration set to " + hours + " hours");
        return result;
    }

    /**
     * Admin: force trigger inactivity check for a user (for testing).
     */
    public Map<String, Object> forceInactivityCheck(UUID userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        inactivityService.processUser(user);
        return Map.of(
                "userId", userId,
                "phone", user.getPhoneE164(),
                "fullName", user.getFullName(),
                "lastActivityAt", formatIst(user.getLastActivityAt()),
                "processed", true
        );
    }

    /**
     * Admin: get inactivity status for a user.
     */
    public Map<String, Object> getInactivityStatus(UUID userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        ElderlySettingsEntity settings = elderlySettingsRepo.findById(userId).orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("phone", user.getPhoneE164());
        result.put("fullName", user.getFullName());
        result.put("lastActivityAt", formatIst(user.getLastActivityAt()));

        if (settings != null) {
            result.put("durationHours", settings.getDurationHours());
            result.put("inactivityMonitoringEnabled", settings.isInactivityMonitoringEnabled());
            if (user.getLastActivityAt() != null && settings.getDurationHours() > 0) {
                Instant threshold = user.getLastActivityAt().plusSeconds((long) settings.getDurationHours() * 3600);
                result.put("alertThreshold", formatIst(threshold));
                result.put("willAlertIn", java.time.Duration.between(Instant.now(), threshold).toMinutes() + " minutes");
            }
        } else {
            result.put("durationHours", null);
            result.put("inactivityMonitoringEnabled", false);
        }
        return result;
    }
}
