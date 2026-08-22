package com.pukaar.domain.notification;

import com.pukaar.common.DeliveryStatus;
import com.pukaar.common.TriggerType;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;

    @Async
    @Transactional
    public void enqueueEmergencyAlert(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery {} missing; skip alert", deliveryId);
            return;
        }
        try {
            EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
            UserEntity user = userRepo.findById(userId).orElse(null);
            boolean mock = event != null && event.isMockDrill();
            boolean help = event != null && event.getTriggerType() == TriggerType.HELP;
            String title = mock ? "PUKAAR TEST ALERT" : "PUKAAR EMERGENCY ALERT";
            String name = user == null || user.getFullName() == null ? "A PUKAAR user" : user.getFullName();
            String body = help
                    ? name + " pressed HELP and needs assistance. Call them first."
                    : name + " may be in danger. Open PUKAAR for live location and actions.";
            log.info("ALERT -> {} | {} | event={} | {}", delivery.getContactPhone(), title, eventId, body);
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setAttempts(delivery.getAttempts() + 1);
            delivery.setLastError(null);
            deliveryRepo.save(delivery);
        } catch (Exception ex) {
            log.error("Failed to send alert for delivery {}", deliveryId, ex);
            deliveryRepo.findById(deliveryId).ifPresent(d -> {
                d.setStatus(DeliveryStatus.FAILED);
                d.setLastError(ex.getMessage());
                d.setAttempts(d.getAttempts() + 1);
                deliveryRepo.save(d);
            });
        }
    }

    @Async
    public void notifyEmergencyClosed(UUID eventId) {
        log.info("Emergency {} closed", eventId);
    }
}
