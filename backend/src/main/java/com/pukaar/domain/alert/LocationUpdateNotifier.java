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

/**
 * Live location to every alerted contact every ~2 minutes until I'm Safe or max hours.
 * Sends text+map (works on all phones) plus location pin when possible, with SMS fallback.
 */
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
    /** Prevents double-send when GPS callback + scheduler race. */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public void maybeNotify(UUID eventId, EmergencyEventEntity event) {
        var wa = props.getAlerts().getWhatsapp();
        if (!wa.isLocationUpdatesEnabled() || !whatsApp.isConfigured()) return;
        if (event.getLatitude() == null || event.getLongitude() == null) return;

        int maxHours = Math.max(1, wa.getLocationUpdateMaxHours());
        Instant started = event.getStartedAt() != null ? event.getStartedAt() : event.getUpdatedAt();
        if (started != null && Instant.now().isAfter(started.plusSeconds(maxHours * 3600L))) {
            clear(eventId);
            return;
        }

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
        // Strict 2-minute cadence (default 120) — always send, even if pin unchanged.
        int interval = Math.max(60, intervalSeconds);
        Instant now = Instant.now();
        Instant prev = lastSent.get(eventId);
        if (prev != null && now.isBefore(prev.plusSeconds(interval))) return;

        lastSent.put(eventId, now);

        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        if (user == null) {
            lastSent.remove(eventId);
            return;
        }

        String who = displayName(user);
        String when = TIME_FMT.format(now.atZone(IST));
        String maps = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                event.getLatitude(), event.getLongitude());
        String coords = String.format(Locale.US, "%.6f, %.6f",
                event.getLatitude(), event.getLongitude());
        // Text+link works on every WhatsApp client; location pin is extra when session allows.
        String body = "*PUKAAR — UPDATED LOCATION*\n"
                + who + " — " + when + " IST\n"
                + "📍 " + maps + "\n"
                + "Coords: " + coords;
        String smsBody = "PUKAAR LIVE LOCATION: " + who + " @ " + when + " IST " + maps;

        Set<String> phonesSeen = new LinkedHashSet<>();
        int sent = 0;
        int failed = 0;
        for (ContactDeliveryEntity d : deliveryRepo.findByEventId(eventId)) {
            String phone = normalizePhone(d.getContactPhone());
            if (phone == null) {
                log.warn("Skip location — invalid phone {}", d.getContactPhone());
                failed++;
                continue;
            }
            String key = PhoneNumbers.digitsOnly(phone);
            if (!phonesSeen.add(key)) continue; // one message per number

            boolean textOk = whatsApp.sendText(phone, body);
            boolean pinOk = false;
            if (!textOk) {
                // Free-form text failed (no session) — try native location pin as alternate.
                pinOk = whatsApp.sendLocation(
                        phone,
                        event.getLatitude(),
                        event.getLongitude(),
                        who + " live location",
                        maps
                );
            }
            boolean smsOk = smsSender.isConfigured() && smsSender.send(phone, smsBody);
            if (textOk || pinOk || smsOk) {
                sent++;
                log.info("Location update to {} text={} pin={} sms={}", phone, textOk, pinOk, smsOk);
            } else {
                failed++;
                log.warn("Location update FAILED for {} — WA+SMS all failed", phone);
            }
        }
        if (sent > 0) {
            log.info("Live location sent for event {} to {} contact(s) ({} failed)", eventId, sent, failed);
        } else {
            lastSent.remove(eventId); // allow retry soon if nothing delivered
            log.warn("Live location skipped for event {} — no deliverable contacts", eventId);
        }
    }

    /** Normalize to E.164; return null if undeliverable. */
    private static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String e164 = PhoneNumbers.toE164(raw.trim());
            return PhoneNumbers.isDeliverable(e164) ? e164 : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void clear(UUID eventId) {
        lastSent.remove(eventId);
        inFlight.remove(eventId);
    }

    private static String displayName(UserEntity user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getPhoneE164() != null ? user.getPhoneE164() : "PUKAAR user";
    }
}
