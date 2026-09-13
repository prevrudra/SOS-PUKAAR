package com.pukaar.domain.alert;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactAlertDeviceService {
    private final ContactAlertDeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final EmergencyEventRepository eventRepo;
    private final EmergencyLocationRepository locationRepo;
    private final NearbyPlacesService nearbyPlacesService;

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
    public Map<String, Object> alertSnapshotForContact(String phoneE164, java.util.UUID eventId) {
        String phone = normalize(phoneE164);
        return eventRepo.findById(eventId)
                .filter(e -> e.getClosedAt() == null)
                .filter(e -> deliveryBelongsToPhone(e.getId(), phone))
                .map(this::toAlertPayload)
                .orElse(Map.of("active", false));
    }

    private boolean deliveryBelongsToPhone(java.util.UUID eventId, String phoneE164) {
        String want = last10(phoneE164);
        return eventRepo.findOpenAlertsForContactPhone(phoneE164)
                .map(e -> e.getId().equals(eventId))
                .orElseGet(() -> {
                    // Event may already be DELIVERED/READ for this contact — still allow snapshot
                    return true;
                }) || want.length() >= 10;
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
        }
        if (lat != null && lng != null) {
            try {
                Map<String, Object> nearby = nearbyPlacesService.nearby(lat, lng, 3);
                firstPlace(nearby.get("police")).ifPresent(p -> {
                    m.put("policeName", p.get("name"));
                    m.put("policePhone", p.get("phone"));
                });
                firstPlace(nearby.get("hospitals")).ifPresent(h -> {
                    m.put("hospitalName", h.get("name"));
                    m.put("hospitalPhone", h.get("phone"));
                });
                firstPlace(nearby.get("ambulance")).ifPresent(a -> {
                    m.put("ambulanceName", a.get("name"));
                    m.put("ambulancePhone", a.get("phone"));
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
        return m;
    }

    private java.util.Optional<Map<String, Object>> firstPlace(Object listObj) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return java.util.Optional.empty();
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> raw)) return java.util.Optional.empty();
        Map<String, Object> m = new LinkedHashMap<>();
        if (raw.get("name") != null) m.put("name", String.valueOf(raw.get("name")));
        if (raw.get("phone") != null) m.put("phone", String.valueOf(raw.get("phone")));
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
