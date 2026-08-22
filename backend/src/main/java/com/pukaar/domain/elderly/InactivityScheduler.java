package com.pukaar.domain.elderly;

import com.pukaar.common.HomeMode;
import com.pukaar.common.InactivityLevel;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityScheduler {
    private final UserRepository userRepo;
    private final ElderlySettingsRepository settingsRepo;

    @Scheduled(fixedDelayString = "${pukaar.elderly.scan-ms:300000}")
    public void scan() {
        List<UserEntity> users = userRepo.findAll().stream()
                .filter(u -> u.getHomeMode() == HomeMode.HELP)
                .filter(u -> u.getLastActivityAt() != null)
                .toList();
        Instant now = Instant.now();
        for (UserEntity user : users) {
            ElderlySettingsEntity settings = settingsRepo.findById(user.getId()).orElse(null);
            if (settings == null || !settings.isInactivityMonitoringEnabled()) continue;
            long hours = Duration.between(user.getLastActivityAt(), now).toHours();
            InactivityLevel level = null;
            if (hours >= settings.getUrgentHours()) level = InactivityLevel.URGENT;
            else if (hours >= settings.getMediumHours()) level = InactivityLevel.MEDIUM;
            else if (hours >= settings.getSoftHours()) level = InactivityLevel.SOFT;
            if (level != null) {
                log.info("Inactivity {} for user {} — no qualifying activity detected for {}h",
                        level, user.getId(), hours);
            }
        }
    }
}
