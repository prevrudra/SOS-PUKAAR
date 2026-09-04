package com.pukaar.domain.notification;

import com.pukaar.domain.alert.AlertDeliveryService;
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

    @Async
    public void enqueueEmergencyAlert(UUID userId, UUID eventId, UUID deliveryId) {
        alertDeliveryService.deliverEmergencyAlert(userId, eventId, deliveryId);
    }

    @Async
    public void notifyEmergencyClosed(UUID eventId) {
        log.info("Emergency {} closed", eventId);
    }
}
