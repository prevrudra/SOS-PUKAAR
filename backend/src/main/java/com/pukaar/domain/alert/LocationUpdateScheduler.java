package com.pukaar.domain.alert;

import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Periodic WhatsApp/SMS live-location pings while SOS is open.
 * Continues with last known GPS until I'm Safe or max hours — does not require
 * a brand-new fix every cycle (OEM devices often stall GPS uploads).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateScheduler {
    private final EmergencyEventRepository eventRepo;
    private final LocationUpdateNotifier locationUpdateNotifier;
    private final PukaarProperties props;

    @Scheduled(fixedDelayString = "${pukaar.alerts.location-scan-ms:30000}")
    public void scanActiveEvents() {
        Instant since = Instant.now().minusSeconds(24 * 3600L);
        int maxHours = Math.max(1, props.getAlerts().getWhatsapp().getLocationUpdateMaxHours());
        Instant maxAge = Instant.now().minusSeconds(maxHours * 3600L);

        for (EmergencyEventEntity event : eventRepo.findOpenSince(since)) {
            if (event.getLatitude() == null || event.getLongitude() == null) continue;

            Instant started = event.getStartedAt() != null ? event.getStartedAt() : event.getUpdatedAt();
            if (started != null && started.isBefore(maxAge)) {
                log.info("Stop location updates for event {} — past {}h max", event.getId(), maxHours);
                locationUpdateNotifier.clear(event.getId());
                continue;
            }

            locationUpdateNotifier.maybeNotify(event.getId(), event);
        }
    }
}
