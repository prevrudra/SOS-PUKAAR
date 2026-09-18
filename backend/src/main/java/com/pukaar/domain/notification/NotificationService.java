package com.pukaar.domain.notification;

import com.pukaar.common.InactivityLevel;
import com.pukaar.domain.alert.AlertDeliveryService;
import com.pukaar.domain.alert.DeliveryStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final AlertDeliveryService alertDeliveryService;
    private final DeliveryStatusService deliveryStatusService;

    @Async
    public void enqueueEmergencyAlert(UUID userId, UUID eventId, UUID deliveryId) {
        alertDeliveryService.deliverEmergencyAlert(userId, eventId, deliveryId);
    }

    @Async
    public void enqueueInactivityAlert(UUID userId, UUID eventId, UUID deliveryId, InactivityLevel level) {
        alertDeliveryService.deliverInactivityAlert(userId, eventId, deliveryId, level);
    }

    @Async
    public void notifyEmergencyClosed(UUID eventId) {
        log.info("Emergency {} closed — notifying contacts user is safe", eventId);
        deliveryStatusService.notifyContactsUserSafe(eventId);
    }
}
