package com.pukaar.domain.alert;

import com.pukaar.common.ApiException;
import com.pukaar.common.DeliveryStatus;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStatusService {
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;
    private final ContactAlertDeviceRepository alertDeviceRepo;
    private final FcmPushSender fcm;
    private final YourBulkSmsSender smsSender;

    @Transactional
    public Map<String, Object> updateStatuses(UUID ownerUserId, UUID eventId, List<StatusUpdate> updates) {
        EmergencyEventEntity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ApiException("EVENT_NOT_FOUND", "Emergency not found"));
        if (!event.getUserId().equals(ownerUserId)) {
            throw new ApiException("FORBIDDEN", "Not your emergency");
        }
        int updated = 0;
        for (StatusUpdate u : updates) {
            if (u.phone() == null || u.status() == null) continue;
            String phone = PhoneNumbers.toE164(u.phone());
            DeliveryStatus status = parse(u.status());
            for (ContactDeliveryEntity d : deliveryRepo.findByEventId(eventId)) {
                if (!phone.equals(d.getContactPhone())) continue;
                applyStatus(d, status);
                deliveryRepo.save(d);
                updated++;
            }
        }
        return Map.of("updated", updated);
    }

    @Transactional
    public Map<String, Object> acknowledgeByContact(UUID contactUserId, UUID eventId, String statusRaw) {
        UserEntity contact = userRepo.findById(contactUserId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        String phone = contact.getPhoneE164();
        DeliveryStatus status = parse(statusRaw == null ? "READ" : statusRaw);
        if (status != DeliveryStatus.DELIVERED && status != DeliveryStatus.READ) {
            status = DeliveryStatus.READ;
        }
        int updated = 0;
        for (ContactDeliveryEntity d : deliveryRepo.findByEventId(eventId)) {
            if (!phone.equals(d.getContactPhone())) continue;
            applyStatus(d, status);
            if (status == DeliveryStatus.READ && d.getAcknowledgedAt() == null) {
                d.setAcknowledgedAt(Instant.now());
            }
            deliveryRepo.save(d);
            updated++;
        }
        if (updated == 0) {
            throw new ApiException("DELIVERY_NOT_FOUND", "No alert delivery for this contact");
        }
        return Map.of("updated", updated, "status", status.name());
    }

    @Transactional
    public void notifyContactsUserSafe(UUID eventId) {
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        if (event == null) return;
        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        if (user == null) return;
        String who = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : user.getPhoneE164();
        String message = who + " is safe now. Please call and check on them now. — PUKAAR";
        Map<String, String> pushData = new LinkedHashMap<>();
        pushData.put("type", "USER_SAFE");
        pushData.put("eventId", eventId.toString());
        pushData.put("victimName", who);
        pushData.put("victimPhone", user.getPhoneE164());

        for (ContactDeliveryEntity d : deliveryRepo.findByEventId(eventId)) {
            var device = alertDeviceRepo.findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(d.getContactPhone());
            if (device.isPresent() && device.get().getFcmToken() != null) {
                fcm.sendHighPriority(device.get().getFcmToken(), "PUKAAR — User Safe", message, pushData);
            }
            // Best-effort SMS when gateway supports the destination
            smsSender.send(d.getContactPhone(), message);
        }
        log.info("Safe notifications enqueued for event {}", eventId);
    }

    private void applyStatus(ContactDeliveryEntity d, DeliveryStatus next) {
        DeliveryStatus current = d.getStatus() == null ? DeliveryStatus.PENDING : d.getStatus();
        if (rank(next) >= rank(current)) {
            d.setStatus(next);
            if (next == DeliveryStatus.READ && d.getAcknowledgedAt() == null) {
                d.setAcknowledgedAt(Instant.now());
            }
        }
    }

    private static int rank(DeliveryStatus s) {
        return switch (s) {
            case PENDING -> 0;
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
            case FAILED, UNKNOWN -> -1;
        };
    }

    private static DeliveryStatus parse(String raw) {
        try {
            return DeliveryStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return DeliveryStatus.SENT;
        }
    }

    public record StatusUpdate(String phone, String status) {}
}
