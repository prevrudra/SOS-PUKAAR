package com.pukaar.domain.alert;

import com.pukaar.common.DeliveryStatus;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Applies Meta WhatsApp delivery/read receipts to contact_delivery rows. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppDeliveryStatusService {
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;

    @Transactional
    public void handleWebhook(Map<String, Object> body) {
        if (body == null || body.isEmpty()) return;
        Object entryObj = body.get("entry");
        if (!(entryObj instanceof List<?> entries)) return;
        for (Object entry : entries) {
            if (!(entry instanceof Map<?, ?> entryMap)) continue;
            Object changesObj = entryMap.get("changes");
            if (!(changesObj instanceof List<?> changes)) continue;
            for (Object change : changes) {
                if (!(change instanceof Map<?, ?> changeMap)) continue;
                Object valueObj = changeMap.get("value");
                if (!(valueObj instanceof Map<?, ?> value)) continue;
                applyStatuses(value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyStatuses(Map<?, ?> value) {
        Object statusesObj = value.get("statuses");
        if (!(statusesObj instanceof List<?> statuses)) return;
        for (Object statusObj : statuses) {
            if (!(statusObj instanceof Map<?, ?> status)) continue;
            String rawStatus = stringVal(status.get("status"));
            String recipient = stringVal(status.get("recipient_id"));
            if (rawStatus == null || recipient == null) continue;
            DeliveryStatus next = mapMetaStatus(rawStatus);
            if (next == null) continue;
            updateDeliveriesForRecipient(recipient, next);
        }
    }

    private void updateDeliveriesForRecipient(String recipientDigits, DeliveryStatus next) {
        String last10 = PhoneNumbers.last10(recipientDigits);
        if (last10.length() < 10) return;
        Instant since = Instant.now().minusSeconds(6 * 3600L);
        int updated = 0;
        for (ContactDeliveryEntity d : deliveryRepo.findRecentByContactLast10(last10, since)) {
            if (!channelHasWhatsApp(d)) continue;
            if (rank(next) > rank(d.getStatus())) {
                d.setStatus(next);
                if (next == DeliveryStatus.READ && d.getAcknowledgedAt() == null) {
                    d.setAcknowledgedAt(Instant.now());
                }
                deliveryRepo.save(d);
                updated++;
            }
        }
        if (updated > 0) {
            log.info("WhatsApp status {} applied to {} delivery row(s) for {}", next, updated, recipientDigits);
        }
    }

    private static DeliveryStatus mapMetaStatus(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "sent" -> DeliveryStatus.SENT;
            case "delivered" -> DeliveryStatus.DELIVERED;
            case "read" -> DeliveryStatus.READ;
            case "failed" -> DeliveryStatus.FAILED;
            default -> null;
        };
    }

    private static int rank(DeliveryStatus s) {
        if (s == null) return -1;
        return switch (s) {
            case PENDING -> 0;
            case SENT -> 1;
            case FAILED, UNKNOWN -> -1;
            case DELIVERED -> 2;
            case READ -> 3;
        };
    }

    private static boolean channelHasWhatsApp(ContactDeliveryEntity d) {
        String ch = d.getChannel();
        String used = d.getChannelUsed();
        return (ch != null && ch.toUpperCase(Locale.ROOT).contains("WHATSAPP"))
                || (used != null && used.toUpperCase(Locale.ROOT).contains("WHATSAPP"));
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString();
    }
}
