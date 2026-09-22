package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.domain.elderly.ElderlySettingsEntity;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.elderly.InactivityService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/elderly")
@RequiredArgsConstructor
public class ElderlyController {
    private static final Set<Integer> ALLOWED = Set.of(12, 18, 24, 30, 36);

    private final ElderlySettingsRepository settingsRepo;
    private final UserRepository userRepo;
    private final InactivityService inactivityService;

    @GetMapping("/settings")
    public Map<String, Object> get() {
        return toDto(settings());
    }

    @PutMapping("/settings")
    public Map<String, Object> update(@RequestBody SettingsRequest req) {
        ElderlySettingsEntity s = settings();
        if (req.getDurationHours() != null) {
            int d = req.getDurationHours();
            if (!ALLOWED.contains(d)) {
                throw new ApiException("INVALID_DURATION", "Duration must be one of 12, 18, 24, 30, 36 hours");
            }
            s.setDurationHours(d);
            s.setDurationMinutes(0); // clear admin/test minute override
            // Keep legacy columns aligned for older clients
            s.setSoftHours(d);
            s.setMediumHours(d);
            s.setUrgentHours(d);
        } else if (req.getUrgentHours() != null) {
            int d = InactivityService.normalizeDuration(req.getUrgentHours());
            s.setDurationHours(d);
            s.setDurationMinutes(0);
            s.setSoftHours(d);
            s.setMediumHours(d);
            s.setUrgentHours(d);
        }
        if (req.getEscalationMinutes() != null) s.setEscalationMinutes(req.getEscalationMinutes());
        if (req.getInactivityMonitoringEnabled() != null) s.setInactivityMonitoringEnabled(req.getInactivityMonitoringEnabled());
        if (req.getAmbulanceNumber() != null) s.setAmbulanceNumber(req.getAmbulanceNumber());
        if (req.getDoctorName() != null) s.setDoctorName(req.getDoctorName());
        if (req.getDoctorPhone() != null) s.setDoctorPhone(req.getDoctorPhone());
        if (req.getBloodGroup() != null) s.setBloodGroup(req.getBloodGroup());
        if (req.getAllergies() != null) s.setAllergies(req.getAllergies());
        if (req.getMedicalConditions() != null) s.setMedicalConditions(req.getMedicalConditions());
        if (req.getMedications() != null) s.setMedications(req.getMedications());
        if (req.getMedicationReminderEnabled() != null) s.setMedicationReminderEnabled(req.getMedicationReminderEnabled());
        return toDto(settingsRepo.save(s));
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat() {
        UserEntity user = userRepo.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        user.setLastActivityAt(Instant.now());
        userRepo.save(user);
        inactivityService.onUserActivity(user.getId());
        return Map.of("lastActivityAt", user.getLastActivityAt());
    }

    @PostMapping("/inactivity/acknowledge")
    public Map<String, Object> acknowledgeInactivity() {
        UserEntity user = userRepo.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        user.setLastActivityAt(Instant.now());
        userRepo.save(user);
        inactivityService.onUserActivity(user.getId());
        return Map.of("ok", true, "lastActivityAt", user.getLastActivityAt());
    }

    private ElderlySettingsEntity settings() {
        return settingsRepo.findById(SecurityUtils.currentUserId())
                .orElseGet(() -> settingsRepo.save(ElderlySettingsEntity.builder().userId(SecurityUtils.currentUserId()).build()));
    }

    private Map<String, Object> toDto(ElderlySettingsEntity s) {
        int duration = InactivityService.normalizeDuration(s.getDurationHours() > 0 ? s.getDurationHours() : s.getUrgentHours());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("durationHours", duration);
        m.put("softHours", duration);
        m.put("mediumHours", duration);
        m.put("urgentHours", duration);
        m.put("escalationMinutes", s.getEscalationMinutes());
        m.put("inactivityMonitoringEnabled", s.isInactivityMonitoringEnabled());
        m.put("ambulanceNumber", s.getAmbulanceNumber());
        m.put("doctorName", s.getDoctorName());
        m.put("doctorPhone", s.getDoctorPhone());
        m.put("bloodGroup", s.getBloodGroup());
        m.put("allergies", s.getAllergies());
        m.put("medicalConditions", s.getMedicalConditions());
        m.put("medications", s.getMedications());
        m.put("medicationReminderEnabled", s.isMedicationReminderEnabled());
        m.put("allowedDurations", ALLOWED);
        return m;
    }

    @Data
    public static class SettingsRequest {
        private Integer durationHours;
        private Integer softHours;
        private Integer mediumHours;
        private Integer urgentHours;
        private Integer escalationMinutes;
        private Boolean inactivityMonitoringEnabled;
        private String ambulanceNumber;
        private String doctorName;
        private String doctorPhone;
        private String bloodGroup;
        private String allergies;
        private String medicalConditions;
        private String medications;
        private Boolean medicationReminderEnabled;
    }
}
