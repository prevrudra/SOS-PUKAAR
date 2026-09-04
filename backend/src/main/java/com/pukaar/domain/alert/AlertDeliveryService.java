package com.pukaar.domain.alert;

import com.pukaar.common.DeliveryStatus;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDeliveryService {
    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;
    private final ContactAlertDeviceRepository alertDeviceRepo;
    private final RichAlertMessageBuilder messageBuilder;
    private final FcmPushSender fcm;

    @Transactional
    public void deliverEmergencyAlert(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery {} missing", deliveryId);
            return;
        }
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null) {
            markFailed(delivery, "Event or user missing");
            return;
        }

        String pushTitle = messageBuilder.buildPushTitle(event);
        String pushBody = messageBuilder.buildPushBody(user, event);

        Map<String, String> pushData = new LinkedHashMap<>();
        pushData.put("type", "EMERGENCY_ALERT");
        pushData.put("eventId", eventId.toString());
        pushData.put("victimName", user.getFullName() != null ? user.getFullName() : "");
        pushData.put("victimPhone", user.getPhoneE164());
        if (event.getLatitude() != null) pushData.put("latitude", event.getLatitude().toString());
        if (event.getLongitude() != null) pushData.put("longitude", event.getLongitude().toString());
        if (event.getBatteryPct() != null) pushData.put("batteryPct", event.getBatteryPct().toString());
        if (event.getNetworkType() != null) pushData.put("networkType", event.getNetworkType());
        pushData.put("mockDrill", Boolean.toString(event.isMockDrill()));
        pushData.put("triggerType", event.getTriggerType().name());

        String phone = delivery.getContactPhone();

        // FCM push to PUKAAR High Alert app (optional supplement)
        var device = alertDeviceRepo.findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(phone);
        if (device.isPresent() && device.get().getFcmToken() != null) {
            if (fcm.sendHighPriority(device.get().getFcmToken(), pushTitle, pushBody, pushData)) {
                delivery.setChannel("FCM");
                delivery.setChannelUsed("FCM");
                delivery.setStatus(DeliveryStatus.SENT);
                delivery.setAttempts(delivery.getAttempts() + 1);
                delivery.setLastError(null);
                deliveryRepo.save(delivery);
                log.info("High Alert push sent to {}", phone);
                return;
            }
        }

        // Primary emergency path: victim device sends SMS via built-in SMS
        delivery.setChannel("DEVICE_SMS");
        delivery.setChannelUsed("DEVICE_SMS");
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastError(null);
        deliveryRepo.save(delivery);
        log.info("Emergency alert for {} will be sent via device SMS from victim phone", phone);
    }

    private void markFailed(ContactDeliveryEntity delivery, String error) {
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setLastError(error);
        delivery.setAttempts(delivery.getAttempts() + 1);
        deliveryRepo.save(delivery);
        log.warn("Alert delivery failed for {}: {}", delivery.getContactPhone(), error);
    }
}
