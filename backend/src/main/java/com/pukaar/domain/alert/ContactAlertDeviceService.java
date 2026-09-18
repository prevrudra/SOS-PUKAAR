package com.pukaar.domain.alert;

import com.pukaar.common.ContactRole;
import com.pukaar.common.DeliveryStatus;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.emergency.EmergencyLocationRepository;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactAlertDeviceService {
    private final ContactAlertDeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final EmergencyEventRepository eventRepo;
    private final EmergencyLocationRepository locationRepo;
    private final NearbyPlacesService nearbyPlacesService;
    private final TrustedContactRepository contactRepo;
    private final ContactDeliveryRepository deliveryRepo;

    @Transactional
    public Map<String, Object> register(String phoneE164, String fcmToken, String deviceId, String platform) {
        String phone = normalize(phoneE164);
        ContactAlertDeviceEntity device = deviceRepo.findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(phone)
                .orElse(ContactAlertDeviceEntity.builder().phoneE164(phone).build());
        if (fcmToken != null && !fcmToken.isBlank()) device.setFcmToken(fcmToken);
        if (deviceId != null) device.setDeviceId(deviceId);
        if (platform != null) device.setPlatform(platform);
        device.setActive(true);
        device = deviceRepo.save(device);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("registered", true);
        m.put("phone", phone);
        m.put("deviceId", device.getId());
        return m;
    }

    public Map<String, Object> pendingAlertForContact(String phoneE164) {
        String phone = normalize(phoneE164);
        return eventRepo.findOpenAlertsForContactPhone(phone)
                .map(this::toAlertPayload)
                .orElse(Map.of("active", false));
    }

    /** Fresh snapshot for an already-open High Alert screen (location may arrive after first fire). */
    public Map<String, Object> alertSnapshotForContact(String phoneE164, UUID eventId) {
        String phone = normalize(phoneE164);
        if (contactDismissedAlert(eventId, phone)) {
            return Map.of("active", false);
        }
        return eventRepo.findById(eventId)
                .filter(e -> e.getClosedAt() == null)
                .filter(e -> deliveryBelongsToPhone(e.getId(), phone))
                .map(this::toAlertPayload)
                .orElse(Map.of("active", false));
    }

    private boolean contactDismissedAlert(UUID eventId, String phoneE164) {
        String want = last10(phoneE164);
        return deliveryRepo.findByEventId(eventId).stream()
                .anyMatch(d -> last10(d.getContactPhone()).equals(want)
                        && (d.getAcknowledgedAt() != null
                        || d.getStatus() == DeliveryStatus.READ));
    }

    private boolean deliveryBelongsToPhone(UUID eventId, String phoneE164) {
        String want = last10(phoneE164);
        if (want.length() < 10) return false;
        return deliveryRepo.findByEventId(eventId).stream()
                .anyMatch(d -> last10(d.getContactPhone()).equals(want));
    }

    private Map<String, Object> toAlertPayload(EmergencyEventEntity event) {
        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        Double lat = event.getLatitude();
        Double lng = event.getLongitude();
        if (lat == null || lng == null) {
            var latest = locationRepo.findByEventIdOrderByRecordedAtDesc(event.getId());
            if (!latest.isEmpty()) {
                lat = latest.get(0).getLatitude();
                lng = latest.get(0).getLongitude();
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", true);
        m.put("eventId", event.getId());
        m.put("mockDrill", event.isMockDrill());
        m.put("triggerType", event.getTriggerType());
        m.put("latitude", lat);
        m.put("longitude", lng);
        m.put("batteryPct", event.getBatteryPct());
        m.put("networkType", event.getNetworkType());
        m.put("startedAt", event.getStartedAt());
        if (user != null) {
            String name = user.getFullName();
            if (name == null || name.isBlank()) name = user.getPhoneE164();
            m.put("victimName", name);
            m.put("victimPhone", user.getPhoneE164());
            m.put("victimSubtitle", subtitleFor(user));
        }
        if (lat != null && lng != null) {
            m.put("locationLabel", String.format(Locale.ROOT, "%.5f, %.5f", lat, lng));
            try {
                Map<String, Object> nearby = nearbyPlacesService.nearby(lat, lng, 3);
                firstPlace(nearby.get("police")).ifPresent(p -> {
                    m.put("policeName", p.get("name"));
                    m.put("policePhone", p.get("phone"));
                    if (p.get("address") != null) m.put("policeAddress", p.get("address"));
                });
                firstPlace(nearby.get("hospitals")).ifPresent(h -> {
                    m.put("hospitalName", h.get("name"));
                    m.put("hospitalPhone", h.get("phone"));
                    if (h.get("address") != null) m.put("hospitalAddress", h.get("address"));
                });
                firstPlace(nearby.get("ambulance")).ifPresent(a -> {
                    m.put("ambulanceName", a.get("name"));
                    m.put("ambulancePhone", a.get("phone"));
                    if (a.get("address") != null) m.put("ambulanceAddress", a.get("address"));
                });
            } catch (Exception e) {
                log.warn("Nearby enrich for High Alert failed: {}", e.getMessage());
            }
        }
        if (!m.containsKey("policeName")) {
            m.put("policeName", "Police Emergency");
            m.put("policePhone", "100");
        }
        if (!m.containsKey("hospitalName")) {
            m.put("hospitalName", "Nearest Hospital");
            m.put("hospitalPhone", "112");
        }
        if (!m.containsKey("ambulanceName")) {
            m.put("ambulanceName", "National Ambulance");
            m.put("ambulancePhone", "108");
        }

        List<TrustedContactEntity> allContacts = contactRepo
                .findByOwnerUserIdAndContactRoleInAndActiveTrue(
                        event.getUserId(),
                        List.of(
                                ContactRole.SOS_TRUSTED,
                                ContactRole.HELP_MONITOR,
                                ContactRole.HELP_BACKUP,
                                ContactRole.DOCTOR,
                                ContactRole.NEIGHBOUR
                        )
                );
        Map<String, ContactDeliveryEntity> deliveryByPhone = new LinkedHashMap<>();
        for (ContactDeliveryEntity d : deliveryRepo.findByEventId(event.getId())) {
            deliveryByPhone.put(last10(d.getContactPhone()), d);
        }

        List<Map<String, Object>> trusted = new ArrayList<>();
        List<Map<String, Object>> help = new ArrayList<>();
        for (TrustedContactEntity c : allContacts) {
            Map<String, Object> row = contactDto(c, deliveryByPhone.get(last10(c.getPhoneE164())));
            if (c.getContactRole() == ContactRole.SOS_TRUSTED) {
                trusted.add(row);
            } else {
                help.add(row);
            }
        }
        m.put("trustedContacts", trusted);
        m.put("helpNumbers", help);
        return m;
    }

    private static String subtitleFor(UserEntity user) {
        if (user.getHomeMode() != null) {
            return switch (user.getHomeMode()) {
                case HELP -> "Help mode user";
                case SOS -> "SOS protected user";
            };
        }
        return "PUKAAR user";
    }

    private Map<String, Object> contactDto(TrustedContactEntity c, ContactDeliveryEntity delivery) {
        Map<String, Object> row = new LinkedHashMap<>();
        String label = c.getName();
        if (c.getRelationship() != null && !c.getRelationship().isBlank()) {
            label = c.getName() + " (" + c.getRelationship() + ")";
        } else if (c.getContactRole() != ContactRole.SOS_TRUSTED) {
            label = c.getName() + " (" + prettyRole(c.getContactRole()) + ")";
        }
        row.put("name", label);
        row.put("phone", c.getPhoneE164());
        row.put("role", c.getContactRole().name());
        row.put("relationship", c.getRelationship());
        String status = "PENDING";
        if (delivery != null && delivery.getStatus() != null) {
            status = delivery.getStatus().name();
        }
        row.put("status", status);
        return row;
    }

    private static String prettyRole(ContactRole role) {
        return switch (role) {
            case DOCTOR -> "Doctor";
            case NEIGHBOUR -> "Neighbor";
            case HELP_MONITOR -> "Monitor";
            case HELP_BACKUP -> "Backup";
            case SOS_TRUSTED -> "Trusted";
        };
    }

    private java.util.Optional<Map<String, Object>> firstPlace(Object listObj) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return java.util.Optional.empty();
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> raw)) return java.util.Optional.empty();
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw.get("name") != null) m.put("name", String.valueOf(raw.get("name")));
        if (raw.get("phone") != null) m.put("phone", String.valueOf(raw.get("phone")));
        if (raw.get("address") != null) m.put("address", String.valueOf(raw.get("address")));
        return m.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(m);
    }

    private String normalize(String phone) {
        return com.pukaar.common.PhoneNumbers.toE164(phone);
    }

    private static String last10(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() <= 10 ? digits : digits.substring(digits.length() - 10);
    }
}
