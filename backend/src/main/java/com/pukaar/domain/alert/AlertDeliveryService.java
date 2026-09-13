package com.pukaar.domain.alert;

import com.pukaar.common.ContactRole;
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
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDeliveryService {
    private static final DateTimeFormatter SOS_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;
    private final ContactAlertDeviceRepository alertDeviceRepo;
    private final TrustedContactRepository contactRepo;
    private final RichAlertMessageBuilder messageBuilder;
    private final FcmPushSender fcm;
    private final WhatsAppAlertSender whatsApp;
    private final NearbyPlacesService nearbyPlacesService;
    private final PukaarProperties props;

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

        if (whatsApp.isConfigured()) {
            List<String> params = buildEmergencyTemplateParams(user, event);
            if (whatsApp.sendEmergencyTemplate(phone, params)) {
                delivery.setChannel("WHATSAPP");
                delivery.setChannelUsed("WHATSAPP");
                delivery.setStatus(DeliveryStatus.SENT);
                delivery.setLastError(null);
                deliveryRepo.save(delivery);
                log.info("WhatsApp emergency alert sent to {}", phone);
                tryPush(user, event, eventId, phone);
                return;
            }
            log.warn("WhatsApp send failed for {}, trying FCM fallback", phone);
        }

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
        NearbySnapshot nearby = resolveNearby(event);
        pushData.put("policeName", nearby.policeName);
        pushData.put("policePhone", nearby.policePhone);
        pushData.put("hospitalName", nearby.hospitalName);
        pushData.put("hospitalPhone", nearby.hospitalPhone);
        return fcm.sendHighPriority(
                device.get().getFcmToken(),
                messageBuilder.buildPushTitle(event),
                messageBuilder.buildPushBody(user, event),
                pushData
        );
    }

    /**
     * Builds body variables for the configured WhatsApp template.
     * <ul>
     *   <li>{@code pukaar_sos} — Sourabh layout (18 vars: time, trusted + emergency lines, services)</li>
     *   <li>{@code emergency} — legacy 19-var template (relations packed into contact names)</li>
     * </ul>
     */
    public List<String> buildEmergencyTemplateParams(UserEntity user, EmergencyEventEntity event) {
        String template = props.getAlerts().getWhatsapp().getTemplateName();
        if (template != null && template.equalsIgnoreCase("pukaar_sos")) {
            return buildPukaarSosParams(user, event);
        }
        return buildLegacyEmergencyParams(user, event);
    }

    /** New Meta template "pukaar_sos" (18 body variables). */
    private List<String> buildPukaarSosParams(UserEntity user, EmergencyEventEntity event) {
        String who = displayName(user);
        String userPhone = user.getPhoneE164() != null ? user.getPhoneE164() : "-";
        String when = event.getStartedAt() != null
                ? SOS_TIME.format(event.getStartedAt().atZone(IST))
                : SOS_TIME.format(java.time.Instant.now().atZone(IST));
        String maps = mapsLink(event);
        String battery = event.getBatteryPct() != null ? event.getBatteryPct() + "%" : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? event.getNetworkType() : "-";

        List<TrustedContactEntity> all = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        List<TrustedContactEntity> trusted = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.SOS_TRUSTED)
                .toList();
        List<TrustedContactEntity> emergency = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.DOCTOR
                        || c.getContactRole() == ContactRole.NEIGHBOUR
                        || c.getContactRole() == ContactRole.HELP_MONITOR
                        || c.getContactRole() == ContactRole.HELP_BACKUP)
                .toList();

        NearbySnapshot nearby = resolveNearby(event);

        List<String> params = new ArrayList<>();
        params.add(who);                 // 1
        params.add(when);                // 2
        params.add(userPhone);           // 3
        params.add(maps);                // 4
        params.add(battery);             // 5
        params.add(network);             // 6
        params.add(contactLine(trusted, 0));   // 7
        params.add(contactLine(trusted, 1));   // 8
        params.add(contactLine(trusted, 2));   // 9
        params.add(contactLine(emergency, 0)); // 10
        params.add(contactLine(emergency, 1)); // 11
        params.add(contactLine(emergency, 2)); // 12
        params.add(nearby.policeName);         // 13
        params.add(nearby.policePhone);        // 14
        params.add(nearby.ambName);            // 15
        params.add(nearby.ambPhone);           // 16
        params.add(nearby.hospitalName);       // 17
        params.add(nearby.hospitalPhone);      // 18
        return params;
    }

    /** Legacy approved template "emergency" (19 body variables). */
    private List<String> buildLegacyEmergencyParams(UserEntity user, EmergencyEventEntity event) {
        String who = displayName(user);
        String userPhone = user.getPhoneE164() != null ? user.getPhoneE164() : "-";
        String maps = mapsLink(event);
        String battery = event.getBatteryPct() != null ? event.getBatteryPct() + "%" : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? event.getNetworkType() : "-";

        List<TrustedContactEntity> contacts = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        String c1n = "-", c1p = "-", c2n = "-", c2p = "-", c3n = "-", c3p = "-";
        if (contacts.size() > 0) {
            c1n = contactNameWithRelation(contacts.get(0));
            c1p = contacts.get(0).getPhoneE164();
        }
        if (contacts.size() > 1) {
            c2n = contactNameWithRelation(contacts.get(1));
            c2p = contacts.get(1).getPhoneE164();
        }
        if (contacts.size() > 2) {
            c3n = contactNameWithRelation(contacts.get(2));
            c3p = contacts.get(2).getPhoneE164();
        }

        NearbySnapshot nearby = resolveNearby(event);

        List<String> params = new ArrayList<>();
        params.add(who);
        params.add(userPhone);
        params.add(maps);
        params.add(battery);
        params.add(network);
        params.add(c1n);
        params.add(c1p);
        params.add(c2n);
        params.add(c2p);
        params.add(c3n);
        params.add(c3p);
        params.add(nearby.policeName);
        params.add(nearby.policePhone);
        params.add(nearby.ambName);
        params.add(nearby.ambPhone);
        params.add(nearby.hospitalName);
        params.add(nearby.hospitalPhone);
        params.add("112");
        params.add(who);
        return params;
    }

    private NearbySnapshot resolveNearby(EmergencyEventEntity event) {
        NearbySnapshot n = new NearbySnapshot(
                "Police Emergency", "100",
                "National Ambulance", "108",
                "Nearest Hospital", "112"
        );
        if (event.getLatitude() == null || event.getLongitude() == null) return n;
        try {
            Map<String, Object> nearby = nearbyPlacesService.nearby(
                    event.getLatitude(), event.getLongitude(), 3);
            n = new NearbySnapshot(
                    firstName(nearby.get("police"), n.policeName),
                    firstPhone(nearby.get("police"), n.policePhone),
                    firstNameLiveAmbulance(nearby.get("ambulance"), n.ambName),
                    firstPhoneLiveAmbulance(nearby.get("ambulance"), n.ambPhone),
                    firstName(nearby.get("hospitals"), n.hospitalName),
                    firstPhone(nearby.get("hospitals"), n.hospitalPhone)
            );
        } catch (Exception e) {
            log.warn("Nearby lookup for WhatsApp template failed: {}", e.getMessage());
        }
        return n;
    }

    private static String displayName(UserEntity user) {
        return user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName() : "PUKAAR user";
    }

    private static String mapsLink(EmergencyEventEntity event) {
        if (event.getLatitude() != null && event.getLongitude() != null) {
            return "https://maps.google.com/?q=" + event.getLatitude() + "," + event.getLongitude();
        }
        return "Location pending";
    }

    private static String contactLine(List<TrustedContactEntity> list, int index) {
        if (index >= list.size()) return "-";
        TrustedContactEntity c = list.get(index);
        String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                ? c.getRelationship() : roleLabel(c.getContactRole());
        return c.getName() + " — " + rel + " — " + c.getPhoneE164();
    }

    private static String contactNameWithRelation(TrustedContactEntity c) {
        if (c.getRelationship() != null && !c.getRelationship().isBlank()) {
            return c.getName() + " (" + c.getRelationship() + ")";
        }
        return c.getName();
    }

    private static String roleLabel(ContactRole role) {
        if (role == null) return "Contact";
        return switch (role) {
            case SOS_TRUSTED -> "Trusted";
            case HELP_MONITOR -> "Help";
            case HELP_BACKUP -> "Backup";
            case DOCTOR -> "Doctor";
            case NEIGHBOUR -> "Neighbour";
        };
    }

    private String firstName(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("name") != null) return String.valueOf(m.get("name"));
        return fallback;
    }

    private String firstPhone(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("phone") != null) {
            String phone = String.valueOf(m.get("phone"));
            if (!phone.isBlank() && !"null".equals(phone)) return phone;
        }
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

    private record NearbySnapshot(
            String policeName,
            String policePhone,
            String ambName,
            String ambPhone,
            String hospitalName,
            String hospitalPhone
    ) {}
}
