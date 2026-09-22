package com.pukaar.domain.alert;

import com.pukaar.common.ContactRole;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.common.DeliveryStatus;
import com.pukaar.common.InactivityLevel;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.elderly.InactivityEpisodeRepository;
import com.pukaar.domain.elderly.InactivityService;
import com.pukaar.domain.elderly.InactivityViewMoreService;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.common.TriggerType;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.config.PukaarProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDeliveryService {
    private static final DateTimeFormatter SOS_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final ContactDeliveryRepository deliveryRepo;
    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;
    private final ContactAlertDeviceRepository alertDeviceRepo;
    private final TrustedContactRepository contactRepo;
    private final RichAlertMessageBuilder messageBuilder;
    private final FcmPushSender fcm;
    private final WhatsAppAlertSender whatsApp;
    private final WhatsAppSosDedupService waDedup;
    private final VoiceSosDedupService voiceDedup;
    private final YourBulkSmsSender smsSender;
    private final AuthKeyVoiceSender voiceSender;
    private final NearbyPlacesService nearbyPlacesService;
    private final PukaarProperties props;
    private final InactivityEpisodeRepository inactivityEpisodeRepo;
    private final InactivityViewMoreService viewMoreService;
    private final ElderlySettingsRepository elderlySettingsRepo;

    @Transactional
    public void deliverInactivityAlert(UUID userId, UUID eventId, UUID deliveryId, InactivityLevel level) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Inactivity delivery {} missing", deliveryId);
            return;
        }
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null) {
            markFailed(delivery, "Event or user missing");
            return;
        }

        String phone = normalizeContactPhone(delivery.getContactPhone());
        delivery.setContactPhone(phone);
        if (!PhoneNumbers.isDeliverable(phone)) {
            markFailed(delivery, "Invalid phone number — cannot deliver alert");
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.SENT) {
            log.info("Inactivity delivery {} already SENT for {} — skip", deliveryId, phone);
            return;
        }
        delivery.setAttempts(delivery.getAttempts() + 1);
        boolean highPriority = level == InactivityLevel.URGENT;
        boolean alreadyWa = channelHasWhatsApp(delivery);
        // FCM first — do not wait on Places/WhatsApp. Save once at end (avoid WA retry race).
        boolean fcmSent = tryPush(user, event, eventId, phone, highPriority, level);
        boolean waSent = alreadyWa || tryWhatsApp(user, event, phone, delivery);
        boolean smsSent = false;

        if (!waSent && !fcmSent) {
            smsSent = trySmsFallback(user, event, phone, delivery);
        }

        if (waSent || fcmSent || smsSent) {
            String channel = channelLabel(fcmSent, waSent, smsSent, false);
            delivery.setChannel(channel);
            delivery.setChannelUsed(waSent ? "WHATSAPP" : (smsSent ? "SMS" : "FCM"));
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setLastError(waSent ? null : "WhatsApp failed — used " + delivery.getChannelUsed());
            deliveryRepo.save(delivery);
            log.info("Inactivity alert to {} via {}", phone, channel);
            return;
        }

        markFailed(delivery, "Inactivity alert delivery failed (FCM+WhatsApp+SMS)");
    }

    @Transactional
    public void deliverEmergencyAlert(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery {} missing", deliveryId);
            return;
        }
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null) {
            markFailed(delivery, "Event or user missing");
            return;
        }

        String phone = normalizeContactPhone(delivery.getContactPhone());
        delivery.setContactPhone(phone);
        if (!PhoneNumbers.isDeliverable(phone)) {
            markFailed(delivery, "Invalid phone number — cannot deliver alert");
            log.warn("Skipping delivery {} — invalid phone {}", deliveryId, phone);
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.SENT) {
            log.info("Delivery {} already SENT for {} — skip duplicate send", deliveryId, phone);
            return;
        }
        delivery.setAttempts(delivery.getAttempts() + 1);
        boolean alreadyWa = channelHasWhatsApp(delivery);

        // FCM FIRST — never block on WhatsApp / Places.
        // Do NOT mid-save FCM-only: that made the retry scheduler re-fire WhatsApp
        // while this call was still sending (duplicate SOS WhatsApps).
        boolean fcmSent = tryPush(user, event, eventId, phone, true, null);
        boolean waSent = alreadyWa || tryWhatsApp(user, event, phone, delivery);
        boolean smsSent = false;
        if (!waSent && !fcmSent) {
            smsSent = trySmsFallback(user, event, phone, delivery);
        }

        if (waSent || fcmSent || smsSent) {
            String channel = channelLabel(fcmSent, waSent, smsSent, false);
            delivery.setChannel(channel);
            delivery.setChannelUsed(waSent ? "WHATSAPP" : (smsSent ? "SMS" : "FCM"));
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setLastError(waSent ? null : "WhatsApp failed — used " + delivery.getChannelUsed());
            deliveryRepo.save(delivery);
            log.info("Emergency alert to {} via {} (fcm={} wa={} sms={}; voice scheduled ~{}s)",
                    phone, channel, fcmSent, waSent, smsSent,
                    props.getNotification().getVoiceEscalateDelaySeconds());
            return;
        }

        delivery.setChannel("WHATSAPP");
        delivery.setChannelUsed("WHATSAPP");
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setLastError("WhatsApp/FCM/SMS delivery failed");
        deliveryRepo.save(delivery);
        log.warn("All alert channels failed for {}", phone);
    }

    /** One WhatsApp template per event+phone — no follow-up text, no send retries. */
    private boolean tryWhatsApp(
            UserEntity user,
            EmergencyEventEntity event,
            String phone,
            ContactDeliveryEntity delivery
    ) {
        if (!whatsApp.isConfigured()) {
            log.warn("WhatsApp not configured — skipping for {}", phone);
            return false;
        }
        if (channelHasWhatsApp(delivery) || waDedup.alreadySent(event.getId(), phone)) {
            return true;
        }
        if (!waDedup.tryClaim(event.getId(), phone)) {
            log.info("WhatsApp already claimed for event {} phone {}", event.getId(), phone);
            return true;
        }

        if (event.getTriggerType() == TriggerType.INACTIVITY) {
            if (sendInactivityWelfareWhatsApp(user, event, phone)) {
                return true;
            }
            waDedup.releaseClaim(event.getId(), phone);
            return false;
        }

        List<String> params = buildEmergencyTemplateParams(user, event);
        if (whatsApp.sendEmergencyTemplate(phone, params)) {
            sendFullAddressFollowUp(phone, user, event);
            return true;
        }
        String template = props.getAlerts().getWhatsapp().getTemplateName();
        if (template != null && (template.equalsIgnoreCase("pukaar_sos") || template.equalsIgnoreCase("alert"))) {
            log.warn("{} failed for {} — trying compact legacy emergency template", template, phone);
            if (whatsApp.sendLegacyEmergencyTemplate(phone, buildCompactLegacyParams(user, event))) {
                sendFullAddressFollowUp(phone, user, event);
                return true;
            }
        }
        String who = displayName(user);
        String maps = compactMapsLink(event);
        String fallback = "PUKAAR SOS: " + who + " needs help now. Call "
                + formatPhoneParam(user.getPhoneE164()) + ". Map: " + maps;
        if (whatsApp.sendText(phone, fallback)) {
            log.warn("WhatsApp templates failed for {} — sent short SOS text fallback", phone);
            sendFullAddressFollowUp(phone, user, event);
            return true;
        }
        waDedup.releaseClaim(event.getId(), phone);
        log.warn("WhatsApp template failed for {}", phone);
        return false;
    }

    private boolean sendInactivityWelfareWhatsApp(UserEntity user, EmergencyEventEntity event, String phone) {
        String who = displayName(user);
        String lastActive = user.getLastActivityAt() != null
                ? SOS_TIME.format(user.getLastActivityAt().atZone(IST)) + " IST"
                : "unknown";
        int hours = elderlySettingsRepo.findById(user.getId())
                .map(s -> InactivityService.normalizeDuration(s.getDurationHours()))
                .orElse(12);
        String viewUrl = inactivityEpisodeRepo.findFirstByEventIdOrderByCreatedAtDesc(event.getId())
                .or(() -> inactivityEpisodeRepo.findFirstByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(user.getId()))
                .map(ep -> ep.getViewToken() == null ? null : viewMoreService.publicViewUrl(ep.getViewToken()))
                .orElse(null);

        String maps = compactMapsLink(event);
        boolean hasLoc = event.getLatitude() != null && event.getLongitude() != null;
        StringBuilder body = new StringBuilder();
        body.append("PUKAAR INACTIVITY ALERT\n\n");
        body.append(who).append(" has had no activity or response for ").append(hours).append(" hours.\n");
        body.append("Last active: ").append(lastActive).append("\n");
        body.append("Please check on them.\n\n");
        if (hasLoc) {
            body.append("Last available location: ").append(maps).append("\n");
            body.append("Current location: ").append(maps).append("\n\n");
        } else {
            body.append("Location: not available yet\n\n");
        }
        body.append("Call ").append(who);
        if (user.getPhoneE164() != null) {
            body.append(" (").append(formatPhoneParam(user.getPhoneE164())).append(")");
        }
        body.append("\n");
        if (viewUrl != null) {
            body.append("\nIf you cannot reach them and need additional information:\n").append(viewUrl);
        }
        return whatsApp.sendText(phone, body.toString());
    }

    private static boolean channelHasWhatsApp(ContactDeliveryEntity delivery) {
        String ch = delivery.getChannel();
        String used = delivery.getChannelUsed();
        return (ch != null && ch.toUpperCase(Locale.ROOT).contains("WHATSAPP"))
                || (used != null && used.toUpperCase(Locale.ROOT).contains("WHATSAPP"));
    }

    private static String channelLabel(boolean fcm, boolean wa, boolean sms, boolean voice) {
        StringBuilder sb = new StringBuilder();
        if (fcm) sb.append("FCM");
        if (wa) {
            if (!sb.isEmpty()) sb.append('+');
            sb.append("WHATSAPP");
        }
        if (voice) {
            if (!sb.isEmpty()) sb.append('+');
            sb.append("VOICE");
        }
        if (sms) {
            if (!sb.isEmpty()) sb.append('+');
            sb.append("SMS");
        }
        return sb.isEmpty() ? "NONE" : sb.toString();
    }

    /** AuthKey voice IVR ~25s after SOS — one call per event+phone even if FCM/WhatsApp already read. */
    @Transactional
    public boolean forceVoiceEscalation(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) return false;
        if (channelHasVoice(delivery) || voiceDedup.alreadySent(eventId, delivery.getContactPhone())) {
            return false;
        }
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null || event.getClosedAt() != null) return false;
        return tryVoiceEscalation(user, event, delivery.getContactPhone(), delivery);
    }

    /** Called by DeliveryRetryScheduler when contact has not acked within 30s. */
    @Transactional
    public void forceSmsFallback(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) return;
        if (delivery.getAcknowledgedAt() != null) return;
        if ("SMS".equalsIgnoreCase(delivery.getChannelUsed())) return;
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null) return;
        trySmsFallback(user, event, delivery.getContactPhone(), delivery);
    }

    /** SMS when WhatsApp and FCM both miss — Meta API can return 200 before handset delivery. */
    private boolean trySmsFallback(
            UserEntity user,
            EmergencyEventEntity event,
            String phone,
            ContactDeliveryEntity delivery
    ) {
        if (!props.getNotification().isSmsFallbackEnabled() || !smsSender.isConfigured()) {
            return false;
        }
        String who = displayName(user);
        String maps = mapsLink(event);
        String body = "PUKAAR SOS ALERT: " + who + " needs help NOW. "
                + maps + " Open PUKAAR High Alert app. Call them immediately.";
        if (smsSender.send(phone, body)) {
            delivery.setChannel("SMS");
            delivery.setChannelUsed("SMS");
            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setLastError(null);
            deliveryRepo.save(delivery);
            log.info("SMS fallback alert sent to {}", phone);
            return true;
        }
        return false;
    }

    private boolean tryVoiceEscalation(
            UserEntity user,
            EmergencyEventEntity event,
            String phone,
            ContactDeliveryEntity delivery
    ) {
        if (!props.getNotification().isVoiceEscalationEnabled() || !voiceSender.isConfigured()) {
            return false;
        }
        if (channelHasVoice(delivery) || voiceDedup.alreadySent(event.getId(), phone)) {
            return true;
        }
        if (!voiceDedup.tryClaim(event.getId(), phone)) {
            log.info("Voice already claimed for event {} phone {}", event.getId(), phone);
            return true;
        }
        String who = displayName(user);
        if (voiceSender.sendEmergency(phone, who)) {
            markVoiceOnAllDeliveries(event.getId(), phone);
            log.info("Voice escalation alert placed to {} for {}", phone, who);
            return true;
        }
        voiceDedup.release(event.getId(), phone);
        log.warn("AuthKey voice failed for {} — claim released for retry", phone);
        return false;
    }

    private void markVoiceOnAllDeliveries(UUID eventId, String phone) {
        String normalized = normalizeContactPhone(phone);
        for (ContactDeliveryEntity row : deliveryRepo.findByEventIdAndContactPhone(eventId, normalized)) {
            String existing = row.getChannel();
            if (channelHasVoice(row)) continue;
            row.setChannel(existing == null || existing.isBlank() ? "VOICE" : existing + "+VOICE");
            row.setLastError(null);
            deliveryRepo.save(row);
        }
    }

    private static boolean channelHasVoice(ContactDeliveryEntity delivery) {
        String ch = delivery.getChannel();
        return ch != null && ch.toUpperCase(Locale.ROOT).contains("VOICE");
    }

    private String buildAlertFollowUpMessage(UserEntity user, EmergencyEventEntity event) {
        if (user == null || event == null) return null;
        String who = displayName(user);
        String when = event.getStartedAt() != null
                ? SOS_TIME.format(event.getStartedAt().atZone(IST)) + " IST"
                : SOS_TIME.format(java.time.Instant.now().atZone(IST)) + " IST";
        NearbySnapshot n = resolveNearby(event);
        List<TrustedContactEntity> all = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        List<TrustedContactEntity> trusted = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.SOS_TRUSTED)
                .limit(3)
                .toList();
        List<TrustedContactEntity> helpContacts = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.DOCTOR
                        || c.getContactRole() == ContactRole.NEIGHBOUR
                        || c.getContactRole() == ContactRole.HELP_MONITOR)
                .limit(3)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("🟣 *PUKAAR — ADDITIONAL SAFETY INFORMATION*\n");
        sb.append(who).append("\n");
        sb.append("Last active: ").append(when).append("\n");
        if (event.getLatitude() != null && event.getLongitude() != null) {
            sb.append("Current / Last available location:\n");
            sb.append(mapsLink(event)).append("\n\n");
        } else {
            sb.append("Current / Last available location: pending\n\n");
        }

        sb.append("👥 *TRUSTED CONTACTS*\n");
        if (trusted.isEmpty()) {
            sb.append("- (none added)\n");
        } else {
            for (TrustedContactEntity c : trusted) {
                String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                        ? c.getRelationship() : roleLabel(c.getContactRole());
                sb.append(c.getName()).append(" — ").append(rel).append(" — ")
                        .append(formatPhoneParam(c.getPhoneE164())).append("\n");
            }
        }
        sb.append("You may coordinate with the other trusted contact to check on ")
                .append(who).append(".\n\n");

        sb.append("📞 *PRE-SAVED HELP NUMBERS*\n");
        if (helpContacts.isEmpty()) {
            sb.append("- (none added)\n");
        } else {
            for (TrustedContactEntity c : helpContacts) {
                String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                        ? c.getRelationship() : roleLabel(c.getContactRole());
                sb.append(c.getName()).append(" — ").append(rel).append(" — ")
                        .append(formatPhoneParam(c.getPhoneE164())).append("\n");
            }
        }
        sb.append("If required, you may call these pre-saved help numbers if they can provide immediate help.\n\n");

        sb.append("🚔 *NEAREST POLICE*\n");
        sb.append(blankToDash(n.policeName())).append("\n");
        sb.append(blankToDash(n.policeAddress())).append("\n");
        sb.append("📞 ").append(blankToDash(n.policePhone())).append("\n\n");

        sb.append("🚑 *NEAREST AMBULANCE*\n");
        sb.append(blankToDash(n.ambName())).append("\n");
        sb.append(blankToDash(n.ambAddress())).append("\n");
        sb.append("📞 ").append(blankToDash(n.ambPhone())).append("\n\n");

        sb.append("🏥 *NEAREST HOSPITAL*\n");
        sb.append(blankToDash(n.hospitalName())).append("\n");
        sb.append(blankToDash(n.hospitalAddress())).append("\n");
        sb.append("📞 ").append(blankToDash(n.hospitalPhone())).append("\n\n");

        sb.append("🆘 *EMERGENCY*\n");
        sb.append("112 — National Emergency\n");
        sb.append("PUKAAR — Information that can help when you need it most.");
        return sb.toString();
    }

    private String buildNearbyAddressFollowUp(EmergencyEventEntity event) {
        UserEntity u = userRepo.findById(event.getUserId()).orElse(null);
        return buildAlertFollowUpMessage(u, event);
    }

    private static String blankToDash(String s) {
        return s == null || s.isBlank() || "null".equals(s) ? "-" : s;
    }

    private boolean tryPush(
            UserEntity user,
            EmergencyEventEntity event,
            UUID eventId,
            String phone,
            boolean fullScreen,
            InactivityLevel inactivityLevel
    ) {
        var device = findAlertDevice(phone);
        if (device.isEmpty() || device.get().getFcmToken() == null) return false;
        Map<String, String> pushData = new java.util.LinkedHashMap<>();
        boolean inactivity = event.getTriggerType() == com.pukaar.common.TriggerType.INACTIVITY;
        pushData.put("type", inactivity && !fullScreen ? "INACTIVITY_ALERT" : "EMERGENCY_ALERT");
        pushData.put("eventId", eventId.toString());
        pushData.put("victimName", user.getFullName() != null ? user.getFullName() : "");
        pushData.put("victimPhone", user.getPhoneE164());
        if (event.getLatitude() != null) pushData.put("latitude", event.getLatitude().toString());
        if (event.getLongitude() != null) pushData.put("longitude", event.getLongitude().toString());
        if (event.getBatteryPct() != null) pushData.put("batteryPct", event.getBatteryPct().toString());
        if (event.getNetworkType() != null) pushData.put("networkType", event.getNetworkType());
        pushData.put("mockDrill", Boolean.toString(event.isMockDrill()));
        pushData.put("triggerType", event.getTriggerType().name());
        if (inactivityLevel != null) {
            pushData.put("inactivityLevel", inactivityLevel.name());
            pushData.put("alertStyle", fullScreen ? "high" : "soft");
        }
        // Do NOT call Places here — Google Places can hang ~30–60s and delay the entire SOS.
        // National defaults are enough for the push wake; WhatsApp enriches nearby separately.
        pushData.put("policeName", "Police");
        pushData.put("policePhone", "100");
        pushData.put("hospitalName", "Hospital");
        pushData.put("hospitalPhone", "112");
        return fcm.sendHighPriority(
                device.get().getFcmToken(),
                messageBuilder.buildPushTitle(event, inactivityLevel),
                messageBuilder.buildPushBody(user, event, inactivityLevel),
                pushData
        );
    }

    /**
     * Builds body variables for the configured WhatsApp template.
     * <ul>
     *   <li>{@code pukaar_sos} — Sourabh layout (18 vars: time, trusted + emergency lines, services)</li>
     *   <li>{@code emergency} — legacy 19-var template (relations packed into contact names)</li>
     * </ul>
     */
    public List<String> buildEmergencyTemplateParams(UserEntity user, EmergencyEventEntity event) {
        String template = props.getAlerts().getWhatsapp().getTemplateName();
        if (template != null && template.equalsIgnoreCase("alert")) {
            return buildAlertTemplateParams(user, event);
        }
        if (template != null && template.equalsIgnoreCase("pukaar_sos")) {
            return buildPukaarSosParams(user, event);
        }
        return buildLegacyEmergencyParams(user, event);
    }

    /**
     * Meta template "alert" (32 body variables) — Sourabh layout Sep 2026.
     * Meta caps rendered body at 1024 chars (#132005), so every var is clipped.
     */
    private List<String> buildAlertTemplateParams(UserEntity user, EmergencyEventEntity event) {
        String who = clip(displayName(user), 24);
        String userPhone = clip(formatPhoneParam(user.getPhoneE164()), 16);
        String when = event.getStartedAt() != null
                ? SOS_TIME.format(event.getStartedAt().atZone(IST))
                : SOS_TIME.format(java.time.Instant.now().atZone(IST));
        when = clip(when, 22);
        String maps = compactMapsLink(event);
        String battery = event.getBatteryPct() != null
                ? String.valueOf(event.getBatteryPct()) : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? clip(event.getNetworkType(), 12) : "-";

        List<TrustedContactEntity> all = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        List<TrustedContactEntity> trusted = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.SOS_TRUSTED)
                .toList();
        List<TrustedContactEntity> emergency = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.DOCTOR
                        || c.getContactRole() == ContactRole.NEIGHBOUR
                        || c.getContactRole() == ContactRole.HELP_MONITOR)
                .toList();
        NearbySnapshot nearby = resolveNearby(event);

        List<String> params = new ArrayList<>();
        params.add(who);       // 1
        params.add(when);      // 2
        params.add(userPhone); // 3
        params.add(maps);      // 4
        params.add(battery);   // 5
        params.add(network);   // 6
        params.add(who);       // 7
        addContactTriple(params, trusted, 0);    // 8-10
        addContactTriple(params, trusted, 1);    // 11-13
        addContactTriple(params, trusted, 2);    // 14-16
        addContactTriple(params, emergency, 0);  // 17-19
        addContactTriple(params, emergency, 1);  // 20-22
        addContactTriple(params, emergency, 2);  // 23-25
        // Short names ONLY in template (Meta 1024 body cap). Full addresses in follow-up text.
        params.add(clip(blankToDash(nearby.policeName()), 28));   // 26
        params.add(clip(blankToDash(nearby.policePhone()), 16));  // 27
        params.add(clip(blankToDash(nearby.ambName()), 28));      // 28
        params.add(clip(blankToDash(nearby.ambPhone()), 16));     // 29
        params.add(clip(blankToDash(nearby.hospitalName()), 28)); // 30
        params.add(clip(blankToDash(nearby.hospitalPhone()), 16));// 31
        params.add(who);       // 32
        return params;
    }

    /** After template opens the 24h window, send full street addresses as free-form text. */
    private void sendFullAddressFollowUp(String phone, UserEntity user, EmergencyEventEntity event) {
        try {
            String body = buildAlertFollowUpMessage(user, event);
            if (body == null || body.isBlank()) return;
            if (whatsApp.sendText(phone, body)) {
                log.info("WhatsApp full-address follow-up sent to {}", phone);
            } else {
                log.warn("WhatsApp full-address follow-up failed for {}", phone);
            }
        } catch (Exception e) {
            log.warn("WhatsApp full-address follow-up error for {}: {}", phone, e.getMessage());
        }
    }

    private static void addContactTriple(List<String> params, List<TrustedContactEntity> list, int index) {
        if (index >= list.size()) {
            params.add("-");
            params.add("-");
            params.add("-");
            return;
        }
        TrustedContactEntity c = list.get(index);
        String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                ? c.getRelationship() : roleLabel(c.getContactRole());
        params.add(clip(c.getName() != null && !c.getName().isBlank() ? c.getName().trim() : "-", 18));
        params.add(clip(rel, 12));
        params.add(clip(formatPhoneParam(c.getPhoneE164()), 16));
    }

    /** Legacy emergency with short fields — avoids #132005 on address-heavy Places names. */
    private List<String> buildCompactLegacyParams(UserEntity user, EmergencyEventEntity event) {
        List<String> full = buildLegacyEmergencyParams(user, event);
        List<String> out = new ArrayList<>(full.size());
        for (int i = 0; i < full.size(); i++) {
            // Location / address-ish slots get a tighter cap.
            int max = (i == 3 || i >= 12) ? 40 : 28;
            out.add(clip(full.get(i), max));
        }
        return out;
    }

    private static String compactMapsLink(EmergencyEventEntity event) {
        if (event.getLatitude() != null && event.getLongitude() != null) {
            return String.format(java.util.Locale.US, "https://maps.google.com/?q=%.4f,%.4f",
                    event.getLatitude(), event.getLongitude());
        }
        return "pending";
    }

    private static String clip(String raw, int max) {
        if (raw == null || raw.isBlank()) return "-";
        String s = raw.trim();
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(1, max - 1)) + "…";
    }

    /** New Meta template "pukaar_sos" (18 body variables). */
    private List<String> buildPukaarSosParams(UserEntity user, EmergencyEventEntity event) {
        String who = displayName(user);
        String userPhone = formatPhoneParam(user.getPhoneE164());
        String when = event.getStartedAt() != null
                ? SOS_TIME.format(event.getStartedAt().atZone(IST))
                : SOS_TIME.format(java.time.Instant.now().atZone(IST));
        String maps = mapsLink(event);
        String battery = event.getBatteryPct() != null
                ? String.valueOf(event.getBatteryPct()) : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? event.getNetworkType() : "-";

        List<TrustedContactEntity> all = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        List<TrustedContactEntity> trusted = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.SOS_TRUSTED)
                .toList();
        // Help numbers are display-only on the alert — never the alerted party.
        List<TrustedContactEntity> emergency = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.DOCTOR
                        || c.getContactRole() == ContactRole.NEIGHBOUR
                        || c.getContactRole() == ContactRole.HELP_MONITOR)
                .toList();

        NearbySnapshot nearby = resolveNearby(event);

        List<String> params = new ArrayList<>();
        params.add(who);                 // 1
        params.add(when);                // 2
        params.add(userPhone);           // 3
        params.add(maps);                // 4
        params.add(battery);             // 5 (template already has % suffix — do not send "12%")
        params.add(network);             // 6
        params.add(contactLine(trusted, 0));   // 7
        params.add(contactLine(trusted, 1));   // 8
        params.add(contactLine(trusted, 2));   // 9
        params.add(contactLine(emergency, 0)); // 10
        params.add(contactLine(emergency, 1)); // 11
        params.add(contactLine(emergency, 2)); // 12
        // Short names in template — full street addresses go in follow-up text (no "…").
        params.add(clip(blankToDash(nearby.policeName()), 40)); // 13
        params.add(clip(blankToDash(nearby.policePhone()), 18)); // 14
        params.add(clip(blankToDash(nearby.ambName()), 40));     // 15
        params.add(clip(blankToDash(nearby.ambPhone()), 18));    // 16
        params.add(clip(blankToDash(nearby.hospitalName()), 40)); // 17
        params.add(clip(blankToDash(nearby.hospitalPhone()), 18)); // 18
        return params;
    }

    /** Legacy approved template "emergency" (19 body variables). */
    private List<String> buildLegacyEmergencyParams(UserEntity user, EmergencyEventEntity event) {
        String who = displayName(user);
        String userPhone = formatPhoneParam(user.getPhoneE164());
        String maps = mapsLink(event);
        // Template body already includes "%" after the battery variable.
        String battery = event.getBatteryPct() != null
                ? String.valueOf(event.getBatteryPct()) : "-";
        String network = event.getNetworkType() != null && !event.getNetworkType().isBlank()
                ? event.getNetworkType() : "-";

        List<TrustedContactEntity> contacts = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId())
                .stream()
                .filter(c -> c.getContactRole() == ContactRole.SOS_TRUSTED)
                .toList();
        String c1n = "-", c1p = "-", c2n = "-", c2p = "-", c3n = "-", c3p = "-";
        if (contacts.size() > 0) {
            c1n = contactNameWithRelation(contacts.get(0));
            c1p = formatPhoneParam(contacts.get(0).getPhoneE164());
        }
        if (contacts.size() > 1) {
            c2n = contactNameWithRelation(contacts.get(1));
            c2p = formatPhoneParam(contacts.get(1).getPhoneE164());
        }
        if (contacts.size() > 2) {
            c3n = contactNameWithRelation(contacts.get(2));
            c3p = formatPhoneParam(contacts.get(2).getPhoneE164());
        }

        NearbySnapshot nearby = resolveNearby(event);

        List<String> params = new ArrayList<>();
        params.add(who);
        params.add(userPhone);
        params.add(maps);
        params.add(battery);
        params.add(network);
        params.add(c1n);
        params.add(c1p);
        params.add(c2n);
        params.add(c2p);
        params.add(c3n);
        params.add(c3p);
        // Short names only — full address is in the free-form follow-up (no truncation).
        params.add(clip(blankToDash(nearby.policeName()), 40));
        params.add(clip(blankToDash(nearby.policePhone()), 18));
        params.add(clip(blankToDash(nearby.ambName()), 40));
        params.add(clip(blankToDash(nearby.ambPhone()), 18));
        params.add(clip(blankToDash(nearby.hospitalName()), 40));
        params.add(clip(blankToDash(nearby.hospitalPhone()), 18));
        params.add("112");
        params.add(who);
        return params;
    }

    private NearbySnapshot resolveNearby(EmergencyEventEntity event) {
        NearbySnapshot n = new NearbySnapshot(
                "Police Emergency", "100", "-",
                "National Ambulance", "108", "-",
                "Nearest Hospital", "112", "-"
        );
        if (event.getLatitude() == null || event.getLongitude() == null) return n;
        try {
            // Hard timeout so WhatsApp never waits ~60s on Places.
            java.util.concurrent.Future<Map<String, Object>> future =
                    java.util.concurrent.Executors.newSingleThreadExecutor().submit(() ->
                            nearbyPlacesService.nearby(event.getLatitude(), event.getLongitude(), 3));
            Map<String, Object> nearby = future.get(2, java.util.concurrent.TimeUnit.SECONDS);
            n = new NearbySnapshot(
                    firstName(nearby.get("police"), n.policeName),
                    firstPhone(nearby.get("police"), n.policePhone),
                    firstAddressOrMaps(nearby.get("police"), n.policeAddress),
                    firstNameLiveAmbulance(nearby.get("ambulance"), n.ambName),
                    firstPhoneLiveAmbulance(nearby.get("ambulance"), n.ambPhone),
                    firstAddressOrMapsLiveAmbulance(nearby.get("ambulance"), n.ambAddress),
                    firstName(nearby.get("hospitals"), n.hospitalName),
                    firstPhone(nearby.get("hospitals"), n.hospitalPhone),
                    firstAddressOrMaps(nearby.get("hospitals"), n.hospitalAddress)
            );
        } catch (Exception e) {
            log.warn("Nearby lookup for WhatsApp template failed/timed out: {}", e.getMessage());
        }
        return n;
    }

    /** Name + full street address for WhatsApp "Name - Phone" template lines. */
    private static String serviceNameWithAddress(String name, String address) {
        String n = name != null && !name.isBlank() ? name.trim() : "-";
        if (address == null || address.isBlank() || "-".equals(address) || "null".equals(address)) {
            return n;
        }
        return n + " | Addr: " + address.trim();
    }

    /** @deprecated use {@link #serviceNameWithAddress} */
    private static String nameWithAddress(String name, String address) {
        return serviceNameWithAddress(name, address);
    }

    private static String displayName(UserEntity user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        if (user.getPhoneE164() != null && !user.getPhoneE164().isBlank()) {
            return user.getPhoneE164();
        }
        return "PUKAAR user";
    }

    private static String mapsLink(EmergencyEventEntity event) {
        if (event.getLatitude() != null && event.getLongitude() != null) {
            return "https://maps.google.com/?q=" + event.getLatitude() + "," + event.getLongitude();
        }
        return "Location pending";
    }

    private static String contactLine(List<TrustedContactEntity> list, int index) {
        if (index >= list.size()) return "-";
        TrustedContactEntity c = list.get(index);
        String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                ? c.getRelationship() : roleLabel(c.getContactRole());
        return c.getName() + " — " + rel + " — " + formatPhoneParam(c.getPhoneE164());
    }

    private static String formatPhoneParam(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        try {
            return PhoneNumbers.toE164(raw);
        } catch (Exception e) {
            return raw.trim();
        }
    }

    private static String normalizeContactPhone(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        try {
            return PhoneNumbers.toE164(raw);
        } catch (Exception e) {
            return raw.trim();
        }
    }

    private java.util.Optional<ContactAlertDeviceEntity> findAlertDevice(String phone) {
        String normalized = normalizeContactPhone(phone);
        return alertDeviceRepo.findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(normalized)
                .or(() -> alertDeviceRepo.findActiveByPhoneLast10(normalized));
    }

    private static String contactNameWithRelation(TrustedContactEntity c) {
        if (c.getRelationship() != null && !c.getRelationship().isBlank()) {
            return c.getName() + " (" + c.getRelationship() + ")";
        }
        return c.getName();
    }

    private static String roleLabel(ContactRole role) {
        if (role == null) return "Contact";
        return switch (role) {
            case SOS_TRUSTED -> "Trusted";
            case HELP_MONITOR -> "Help";
            case HELP_BACKUP -> "Backup";
            case DOCTOR -> "Doctor";
            case NEIGHBOUR -> "Neighbour";
        };
    }

    private String firstName(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("name") != null) return String.valueOf(m.get("name"));
        return fallback;
    }

    private String firstPhone(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("phone") != null) {
            String phone = String.valueOf(m.get("phone"));
            if (!phone.isBlank() && !"null".equals(phone)) return phone;
        }
        return fallback;
    }

    private String firstAddress(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m && m.get("address") != null) {
            String address = String.valueOf(m.get("address"));
            if (!address.isBlank() && !"null".equals(address)) return address;
        }
        return fallback;
    }

    /** Prefer street address; if missing, use the place's Google Maps pin. */
    private String firstAddressOrMaps(Object listObj, String fallback) {
        String address = firstAddress(listObj, null);
        if (address != null) return address;
        if (!(listObj instanceof List<?> list) || list.isEmpty()) return fallback;
        Object first = list.get(0);
        if (first instanceof Map<?, ?> m) {
            Object lat = m.get("latitude");
            Object lng = m.get("longitude");
            if (lat instanceof Number && lng instanceof Number) {
                return "https://maps.google.com/?q="
                        + ((Number) lat).doubleValue() + "," + ((Number) lng).doubleValue();
            }
        }
        return fallback;
    }

    private String firstNameLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return fallback;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && !"NATIONAL".equals(String.valueOf(m.get("source")))
                    && m.get("name") != null) {
                return String.valueOf(m.get("name"));
            }
        }
        return firstName(listObj, fallback);
    }

    private String firstPhoneLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return fallback;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && !"NATIONAL".equals(String.valueOf(m.get("source")))
                    && m.get("phone") != null) {
                return String.valueOf(m.get("phone"));
            }
        }
        return firstPhone(listObj, fallback);
    }

    private String firstAddressLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return fallback;
        for (Object item : list) {
            if (item instanceof Map<?, ?> m && !"NATIONAL".equals(String.valueOf(m.get("source")))
                    && m.get("address") != null) {
                String address = String.valueOf(m.get("address"));
                if (!address.isBlank() && !"null".equals(address)) return address;
            }
        }
        return firstAddress(listObj, fallback);
    }

    private String firstAddressOrMapsLiveAmbulance(Object listObj, String fallback) {
        if (!(listObj instanceof List<?> list)) return firstAddressOrMaps(listObj, fallback);
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m) || "NATIONAL".equals(String.valueOf(m.get("source")))) {
                continue;
            }
            if (m.get("address") != null) {
                String address = String.valueOf(m.get("address"));
                if (!address.isBlank() && !"null".equals(address)) return address;
            }
            Object lat = m.get("latitude");
            Object lng = m.get("longitude");
            if (lat instanceof Number && lng instanceof Number) {
                return "https://maps.google.com/?q="
                        + ((Number) lat).doubleValue() + "," + ((Number) lng).doubleValue();
            }
        }
        return firstAddressOrMaps(listObj, fallback);
    }

    private void markFailed(ContactDeliveryEntity delivery, String error) {
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setLastError(error);
        delivery.setAttempts(delivery.getAttempts() + 1);
        deliveryRepo.save(delivery);
        log.warn("Alert delivery failed for {}: {}", delivery.getContactPhone(), error);
    }

    private record NearbySnapshot(
            String policeName,
            String policePhone,
            String policeAddress,
            String ambName,
            String ambPhone,
            String ambAddress,
            String hospitalName,
            String hospitalPhone,
            String hospitalAddress
    ) {}
}
