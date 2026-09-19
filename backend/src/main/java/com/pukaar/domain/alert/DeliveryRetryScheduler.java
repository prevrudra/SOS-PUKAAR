package com.pukaar.domain.alert;

import com.pukaar.common.DeliveryStatus;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Never leave an SOS delivery as a silent failure.
 * Retries FAILED/PENDING every 15s; forces SMS if still unacked after 30s.
 * Voice IVR fires instantly on SOS (see AlertDeliveryService).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryRetryScheduler {
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final NotificationService notificationService;
    private final AlertDeliveryService alertDeliveryService;
    private final PukaarProperties props;

    @Scheduled(fixedDelayString = "${pukaar.notification.retry-scan-ms:15000}")
    public void retryFailed() {
        int max = Math.max(1, props.getNotification().getRetryMax());
        Instant since = Instant.now().minusSeconds(6 * 3600L);
        List<ContactDeliveryEntity> rows = deliveryRepo.findRetryable(max, since);
        for (ContactDeliveryEntity d : rows) {
            EmergencyEventEntity event = eventRepo.findById(d.getEventId()).orElse(null);
            if (event == null || event.getClosedAt() != null) continue;
            log.info("Retrying delivery {} attempt={} phone={}", d.getId(), d.getAttempts(), d.getContactPhone());
            notificationService.enqueueEmergencyAlert(event.getUserId(), event.getId(), d.getId());
        }

        // Do NOT retry WhatsApp here — it caused duplicate SOS messages when FCM
        // succeeded first. FCM + server SMS fallback are enough; WA is send-once only.
    }

    @Scheduled(fixedDelayString = "${pukaar.notification.sms-escalate-ms:15000}")
    public void escalateUnackedToSms() {
        Instant olderThan = Instant.now().minusSeconds(30);
        Instant since = Instant.now().minusSeconds(6 * 3600L);
        List<ContactDeliveryEntity> rows = deliveryRepo.findUnackedNeedingSms(olderThan, since);
        for (ContactDeliveryEntity d : rows) {
            EmergencyEventEntity event = eventRepo.findById(d.getEventId()).orElse(null);
            if (event == null || event.getClosedAt() != null) continue;
            if (d.getStatus() == DeliveryStatus.DELIVERED || d.getStatus() == DeliveryStatus.READ) continue;
            log.warn("No ack in 30s — forcing SMS for delivery {} phone={}", d.getId(), d.getContactPhone());
            alertDeliveryService.forceSmsFallback(event.getUserId(), event.getId(), d.getId());
        }
    }

}
