package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.domain.elderly.ElderlySettingsEntity;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/elderly")
@RequiredArgsConstructor
public class ElderlyController {
    private final ElderlySettingsRepository settingsRepo;
    private final UserRepository userRepo;

    @GetMapping("/settings")
    public Map<String, Object> get() {
        return toDto(settings());
    }

    @PutMapping("/settings")
    public Map<String, Object> update(@RequestBody SettingsRequest req) {
        ElderlySettingsEntity s = settings();
        if (req.getSoftHours() != null) s.setSoftHours(req.getSoftHours());
        if (req.getMediumHours() != null) s.setMediumHours(req.getMediumHours());
        if (req.getUrgentHours() != null) s.setUrgentHours(req.getUrgentHours());
        if (req.getEscalationMinutes() != null) s.setEscalationMinutes(req.getEscalationMinutes());
        if (req.getInactivityMonitoringEnabled() != null) s.setInactivityMonitoringEnabled(req.getInactivityMonitoringEnabled());
        if (req.getAmbulanceNumber() != null) s.setAmbulanceNumber(req.getAmbulanceNumber());
        if (req.getDoctorName() != null) s.setDoctorName(req.getDoctorName());
        if (req.getDoctorPhone() != null) s.setDoctorPhone(req.getDoctorPhone());
        return toDto(settingsRepo.save(s));
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat() {
        UserEntity user = userRepo.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        user.setLastActivityAt(Instant.now());
        userRepo.save(user);
        return Map.of("lastActivityAt", user.getLastActivityAt());
    }

    private ElderlySettingsEntity settings() {
        return settingsRepo.findById(SecurityUtils.currentUserId())
                .orElseGet(() -> settingsRepo.save(ElderlySettingsEntity.builder().userId(SecurityUtils.currentUserId()).build()));
    }

    private Map<String, Object> toDto(ElderlySettingsEntity s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("softHours", s.getSoftHours());
        m.put("mediumHours", s.getMediumHours());
        m.put("urgentHours", s.getUrgentHours());
        m.put("escalationMinutes", s.getEscalationMinutes());
        m.put("inactivityMonitoringEnabled", s.isInactivityMonitoringEnabled());
        m.put("ambulanceNumber", s.getAmbulanceNumber());
        m.put("doctorName", s.getDoctorName());
        m.put("doctorPhone", s.getDoctorPhone());
        return m;
    }

    @Data
    public static class SettingsRequest {
        private Integer softHours;
        private Integer mediumHours;
        private Integer urgentHours;
        private Integer escalationMinutes;
        private Boolean inactivityMonitoringEnabled;
        private String ambulanceNumber;
        private String doctorName;
        private String doctorPhone;
    }
}
