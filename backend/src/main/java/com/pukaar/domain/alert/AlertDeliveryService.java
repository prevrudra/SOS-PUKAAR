package com.pukaar.domain.alert;

import com.pukaar.common.DeliveryStatus;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
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
    private final TrustedContactRepository contactRepo;
    private final RichAlertMessageBuilder messageBuilder;
    private final FcmPushSender fcm;
    private final WhatsAppAlertSender whatsApp;
    private final NearbyPlacesService nearbyPlacesService;

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

        String phone = delivery.getContactPhone();
        delivery.setAttempts(delivery.getAttempts() + 1);

        // 1) WhatsApp Cloud API (primary — no device SMS permission)
        if (whatsApp.isConfigured()) {
            List<String> params = buildEmergencyTemplateParams(user, event);
            if (whatsApp.sendEmergencyTemplate(phone, params)) {
                delivery.setChannel("WHATSAPP");
                delivery.setChannelUsed("WHATSAPP");
                delivery.setStatus(DeliveryStatus.SENT);
                delivery.setLastError(null);
                deliveryRepo.save(delivery);
                log.info("WhatsApp emergency alert sent to {}", phone);
                // Still try High Alert push as a supplement (non-blocking best-effort)
                tryPush(user, event, eventId, phone);
                return;
            }
            log.warn("WhatsApp send failed for {}, trying FCM fallback", phone);
        }

        // 2) FCM to High Alert app
        String pushTitle = messageBuilder.buildPushTitle(event);
        String pushBody = messageBuilder.buildPushBody(user, event);
        if (tryPush(user, event, eventId, phone)) {
            delivery.setChannel("FCM");
            delivery.setChannelUsed("FCM");
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setLastError(null);
            deliveryRepo.save(delivery);
            return;
        }

        delivery.setChannel("WHATSAPP");
        delivery.setChannelUsed("WHATSAPP");
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setLastError("WhatsApp/FCM delivery failed");
        deliveryRepo.save(delivery);
        log.warn("All alert channels failed for {}", phone);
    }

    private boolean tryPush(UserEntity user, EmergencyEventEntity event, UUID eventId, String phone) {
        var device = alertDeviceRepo.findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(phone);
        if (device.isEmpty() || device.get().getFcmToken() == null) return false;
        Map<String, String> pushData = new java.util.LinkedHashMap<>();
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
        return fcm.sendHighPriority(
                device.get().getFcmToken(),
                messageBuilder.buildPushTitle(event),
                messageBuilder.buildPushBody(user, event),
                pushData
        );
    }

    /**
     * Matches approved WhatsApp template "emergency" body variables (19).
     */
    public List<String> buildEmergencyTemplateParams(UserEntity user, EmergencyEventEntity event) {
        String who = user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "PUKAAR user";
        String userPhone = user.getPhoneE164() != null ? user.getPhoneE164() : "-";
        String maps = (event.getLatitude() != null && event.getLongitude() != null)
                ? "https://maps.google.com/?q=" + event.getLatitude() + "," + event.getLongitude()
                : "Location pending";
        String battery = event.getBatteryPct() != null ? event.getBatteryPct() + "%" : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? event.getNetworkType() : "-";

        List<TrustedContactEntity> contacts = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        String c1n = "-", c1p = "-", c2n = "-", c2p = "-", c3n = "-", c3p = "-";
        if (contacts.size() > 0) { c1n = contacts.get(0).getName(); c1p = contacts.get(0).getPhoneE164(); }
        if (contacts.size() > 1) { c2n = contacts.get(1).getName(); c2p = contacts.get(1).getPhoneE164(); }
        if (contacts.size() > 2) { c3n = contacts.get(2).getName(); c3p = contacts.get(2).getPhoneE164(); }

        String policeName = "-", policePhone = "-", ambName = "108", ambPhone = "108",
                hospitalName = "-", hospitalPhone = "-";
        if (event.getLatitude() != null && event.getLongitude() != null) {
            try {
                Map<String, Object> nearby = nearbyPlacesService.nearby(
                        event.getLatitude(), event.getLongitude(), 1);
                policeName = firstName(nearby.get("police"), policeName);
                policePhone = firstPhone(nearby.get("police"), policePhone);
                hospitalName = firstName(nearby.get("hospitals"), hospitalName);
                hospitalPhone = firstPhone(nearby.get("hospitals"), hospitalPhone);
                ambName = firstNameLiveAmbulance(nearby.get("ambulance"), ambName);
                ambPhone = firstPhoneLiveAmbulance(nearby.get("ambulance"), ambPhone);
            } catch (Exception e) {
                log.warn("Nearby lookup for WhatsApp template failed: {}", e.getMessage());
            }
        }

        List<String> params = new ArrayList<>();
        params.add(who);           // 1 victim name
        params.add(userPhone);     // 2 victim phone
        params.add(maps);          // 3 maps link
        params.add(battery);       // 4 battery
        params.add(network);       // 5 network
        params.add(c1n);           // 6 contact1 name
        params.add(c1p);           // 7 contact1 phone
        params.add(c2n);           // 8
        params.add(c2p);           // 9
        params.add(c3n);           // 10
        params.add(c3p);           // 11
        params.add(policeName);    // 12
        params.add(policePhone);   // 13
        params.add(ambName);       // 14 ambulance name
        params.add(ambPhone);      // 15 ambulance phone
        params.add(hospitalName);  // 16
        params.add(hospitalPhone); // 17
        params.add("112");         // 18 emergency number
        params.add(who);           // 19 closing name
        return params;
    }

    @SuppressWarnings("unchecked")
    private String firstName(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("name") != null) return String.valueOf(m.get("name"));
        return fallback;
    }

    private String firstPhone(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("phone") != null) return String.valueOf(m.get("phone"));
        return fallback;
    }

    private String firstNameLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return fallback;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && !"NATIONAL".equals(String.valueOf(m.get("source")))
                    && m.get("name") != null) {
                return String.valueOf(m.get("name"));
            }
        }
        return firstName(listObj, fallback);
    }

    private String firstPhoneLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return fallback;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && !"NATIONAL".equals(String.valueOf(m.get("source")))
                    && m.get("phone") != null) {
                return String.valueOf(m.get("phone"));
            }
        }
        return firstPhone(listObj, fallback);
    }

    private void markFailed(ContactDeliveryEntity delivery, String error) {
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setLastError(error);
        delivery.setAttempts(delivery.getAttempts() + 1);
        deliveryRepo.save(delivery);
        log.warn("Alert delivery failed for {}: {}", delivery.getContactPhone(), error);
    }
}
