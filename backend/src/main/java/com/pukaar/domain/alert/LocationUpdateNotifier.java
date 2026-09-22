package com.pukaar.domain.alert;

import com.pukaar.common.PhoneNumbers;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Throttled WhatsApp/SMS live-location pings to contacts during an active SOS. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationUpdateNotifier {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final PukaarProperties props;
    private final WhatsAppAlertSender whatsApp;
    private final YourBulkSmsSender smsSender;
    private final ContactDeliveryRepository deliveryRepo;
    private final UserRepository userRepo;

    private final Map<UUID, Instant> lastSent = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentCoords = new ConcurrentHashMap<>();
    /** Prevents double-send when GPS callback + scheduler race. */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public void maybeNotify(UUID eventId, EmergencyEventEntity event) {
        var wa = props.getAlerts().getWhatsapp();
        if (!wa.isLocationUpdatesEnabled() || !whatsApp.isConfigured()) return;
        if (event.getLatitude() == null || event.getLongitude() == null) return;
        if (!inFlight.add(eventId)) {
            log.debug("Skip location — already sending for event {}", eventId);
            return;
        }
        try {
            notifyLocked(eventId, event, wa.getLocationUpdateIntervalSeconds());
        } finally {
            inFlight.remove(eventId);
        }
    }

    private void notifyLocked(UUID eventId, EmergencyEventEntity event, int intervalSeconds) {
        int interval = Math.max(60, intervalSeconds);
        Instant now = Instant.now();
        Instant prev = lastSent.get(eventId);
        String coordKey = String.format(Locale.US, "%.5f,%.5f", event.getLatitude(), event.getLongitude());
        boolean moved = !coordKey.equals(lastSentCoords.get(eventId));
        if (prev != null && now.isBefore(prev.plusSeconds(interval))) return;
        // After 2 intervals, allow a heartbeat even if pin is unchanged (Motorola stall UX).
        boolean heartbeat = prev != null && !moved
                && now.isAfter(prev.plusSeconds(interval * 2L));
        if (prev != null && !moved && !heartbeat) {
            log.debug("Skip location event {} — coordinates unchanged", eventId);
            return;
        }

        lastSent.put(eventId, now);
        lastSentCoords.put(eventId, coordKey);

        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        if (user == null) {
            lastSent.remove(eventId);
            return;
        }

        String who = displayName(user);
        String when = TIME_FMT.format(now.atZone(IST));
        String maps = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                event.getLatitude(), event.getLongitude());
        String body = "*PUKAAR LIVE LOCATION*\n"
                + who + " — updated " + when + " IST\n"
                + "Open map: " + maps;
        String smsBody = "PUKAAR LIVE LOCATION: " + who + " @ " + when + " IST " + maps;

        Set<String> phonesSeen = new LinkedHashSet<>();
        int sent = 0;
        for (ContactDeliveryEntity d : deliveryRepo.findByEventId(eventId)) {
            String phone = d.getContactPhone();
            if (!PhoneNumbers.isDeliverable(phone)) {
                log.warn("Skip location — invalid phone {}", phone);
                continue;
            }
            String key = PhoneNumbers.digitsOnly(phone);
            if (!phonesSeen.add(key)) continue; // one message per number

            boolean waOk = whatsApp.sendLocation(
                    phone,
                    event.getLatitude(),
                    event.getLongitude(),
                    who + " live location",
                    maps
            );
            if (!waOk) {
                waOk = whatsApp.sendText(phone, body);
            }
            boolean smsOk = smsSender.isConfigured() && smsSender.send(phone, smsBody);
            if (waOk || smsOk) {
                sent++;
                log.info("Location update to {} wa={} sms={}", phone, waOk, smsOk);
            }
        }
        if (sent > 0) {
            log.info("Live location sent for event {} to {} contact(s)", eventId, sent);
        } else {
            lastSent.remove(eventId); // allow retry soon if nothing delivered
            log.warn("Live location skipped for event {} — no deliverable contacts", eventId);
        }
    }

    public void clear(UUID eventId) {
        lastSent.remove(eventId);
        lastSentCoords.remove(eventId);
        inFlight.remove(eventId);
    }

    private static String displayName(UserEntity user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getPhoneE164() != null ? user.getPhoneE164() : "PUKAAR user";
    }
}
