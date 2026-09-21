package com.pukaar.domain.alert;

import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Backup for live-location WhatsApp pings when the victim app stops posting GPS
 * (OEM battery kill, weak signal, etc.). Uses the last known coordinates on the event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateScheduler {
    private final EmergencyEventRepository eventRepo;
    private final LocationUpdateNotifier locationUpdateNotifier;

    @Scheduled(fixedDelayString = "${pukaar.alerts.location-scan-ms:60000}")
    public void scanActiveEvents() {
        Instant since = Instant.now().minusSeconds(24 * 3600L);
        for (EmergencyEventEntity event : eventRepo.findOpenSince(since)) {
            if (event.getLatitude() == null || event.getLongitude() == null) continue;
            locationUpdateNotifier.maybeNotify(event.getId(), event);
        }
    }
}
