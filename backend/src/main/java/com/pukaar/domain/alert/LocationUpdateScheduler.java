package com.pukaar.domain.alert;

import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Backup WhatsApp/SMS location pings — only when the PUKAAR app has posted a
 * fresh GPS fix recently. Never rebroadcast a frozen first/cell-tower point.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateScheduler {
    /** Ignore events whose last app GPS fix is older than this. */
    private static final long FRESH_GPS_MAX_AGE_SEC = 420;

    private final EmergencyEventRepository eventRepo;
    private final LocationUpdateNotifier locationUpdateNotifier;

    @Scheduled(fixedDelayString = "${pukaar.alerts.location-scan-ms:60000}")
    public void scanActiveEvents() {
        Instant since = Instant.now().minusSeconds(24 * 3600L);
        Instant freshAfter = Instant.now().minusSeconds(FRESH_GPS_MAX_AGE_SEC);
        for (EmergencyEventEntity event : eventRepo.findOpenSince(since)) {
            if (event.getLatitude() == null || event.getLongitude() == null) continue;
            Instant acquired = event.getLocationAcquiredAt();
            if (acquired == null || acquired.isBefore(freshAfter)) {
                log.debug("Skip location rebroadcast event {} — GPS not fresh from PUKAAR app", event.getId());
                continue;
            }
            locationUpdateNotifier.maybeNotify(event.getId(), event);
        }
    }
}
