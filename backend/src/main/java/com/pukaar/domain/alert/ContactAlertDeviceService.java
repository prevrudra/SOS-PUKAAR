package com.pukaar.domain.alert;

import com.pukaar.common.ApiException;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactAlertDeviceService {
    private final ContactAlertDeviceRepository deviceRepo;
    private final UserRepository userRepo;
    private final EmergencyEventRepository eventRepo;

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
        // Find any open emergency where this phone is a trusted contact delivery target
        return eventRepo.findOpenAlertsForContactPhone(phone)
                .map(this::toAlertPayload)
                .orElse(Map.of("active", false));
    }

    private Map<String, Object> toAlertPayload(EmergencyEventEntity event) {
        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", true);
        m.put("eventId", event.getId());
        m.put("mockDrill", event.isMockDrill());
        m.put("triggerType", event.getTriggerType());
        m.put("latitude", event.getLatitude());
        m.put("longitude", event.getLongitude());
        m.put("batteryPct", event.getBatteryPct());
        m.put("networkType", event.getNetworkType());
        m.put("startedAt", event.getStartedAt());
        if (user != null) {
            m.put("victimName", user.getFullName());
            m.put("victimPhone", user.getPhoneE164());
        }
        return m;
    }

    private String normalize(String phone) {
        String p = phone == null ? "" : phone.trim().replace(" ", "");
        if (!p.startsWith("+")) {
            if (p.length() == 10) p = "+91" + p;
            else p = "+" + p;
        }
        if (p.length() < 10) throw new ApiException("INVALID_PHONE", "Invalid phone number");
        return p;
    }
}
