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
import com.pukaar.domain.nearby.NearbyPlacesService;
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
        // DB claim — multiple delivery rows for the same phone cannot all send.
        if (!waDedup.tryClaim(event.getId(), phone)) {
            log.info("WhatsApp already claimed for event {} phone {}", event.getId(), phone);
            return true;
        }

        List<String> params = buildEmergencyTemplateParams(user, event);
        if (whatsApp.sendEmergencyTemplate(phone, params)) {
            return true;
        }
        String template = props.getAlerts().getWhatsapp().getTemplateName();
        if (template != null && template.equalsIgnoreCase("pukaar_sos")) {
            log.warn("pukaar_sos failed for {} — trying legacy emergency template", phone);
            if (whatsApp.sendLegacyEmergencyTemplate(phone, buildLegacyEmergencyParams(user, event))) {
                return true;
            }
        }
        log.warn("WhatsApp template failed for {}", phone);
        return false;
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

    /** AuthKey voice IVR ~25s after SOS if the contact has not acknowledged yet. */
    @Transactional
    public boolean forceVoiceEscalation(UUID userId, UUID eventId, UUID deliveryId) {
        ContactDeliveryEntity delivery = deliveryRepo.findById(deliveryId).orElse(null);
        if (delivery == null) return false;
        if (delivery.getAcknowledgedAt() != null) return false;
        if (channelHasVoice(delivery) || voiceDedup.alreadySent(eventId, delivery.getContactPhone())) {
            return false;
        }
        EmergencyEventEntity event = eventRepo.findById(eventId).orElse(null);
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (event == null || user == null) return false;
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
        boolean help = event.getTriggerType() == com.pukaar.common.TriggerType.HELP;
        boolean inactivity = event.getTriggerType() == com.pukaar.common.TriggerType.INACTIVITY;
        NearbySnapshot n = resolveNearby(event);
        List<TrustedContactEntity> all = contactRepo
                .findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(user.getId());
        List<TrustedContactEntity> helpContacts = all.stream()
                .filter(c -> c.getContactRole() == ContactRole.DOCTOR
                        || c.getContactRole() == ContactRole.NEIGHBOUR
                        || c.getContactRole() == ContactRole.HELP_MONITOR)
                .toList();

        StringBuilder sb = new StringBuilder();
        if (inactivity) {
            sb.append("*PUKAAR INACTIVITY ALERT*\n");
            sb.append(who).append(" has not used their phone for a long time.\n");
            sb.append("Please call and check on ").append(who).append(" now.\n\n");
        } else if (help) {
            sb.append("*PUKAAR HELP — ASSISTANCE*\n");
            sb.append(who).append(" has activated HELP and may need assistance.\n");
            sb.append("Please check on ").append(who).append(" now.\n\n");
        } else {
            sb.append("*PUKAAR SOS — DETAILS*\n");
            sb.append("Please check on ").append(who).append(" immediately.\n\n");
        }

        sb.append("HELP / DOCTOR / NEIGHBOUR NUMBERS\n");
        if (helpContacts.isEmpty()) {
            sb.append("- (none added)\n");
        } else {
            for (TrustedContactEntity c : helpContacts.stream().limit(5).toList()) {
                String rel = c.getRelationship() != null && !c.getRelationship().isBlank()
                        ? c.getRelationship() : roleLabel(c.getContactRole());
                sb.append("👤 ").append(c.getName()).append(" (").append(rel).append(") - ")
                        .append(c.getPhoneE164()).append("\n");
            }
        }
        sb.append("\n");
        sb.append("NEAREST SERVICES — FULL ADDRESS\n");
        sb.append("🚔 Police: ").append(n.policeName()).append("\n");
        sb.append("Address: ").append(blankToDash(n.policeAddress())).append("\n");
        sb.append("Phone: ").append(blankToDash(n.policePhone())).append("\n\n");
        sb.append("🚑 Ambulance: ").append(n.ambName()).append("\n");
        sb.append("Address: ").append(blankToDash(n.ambAddress())).append("\n");
        sb.append("Phone: ").append(blankToDash(n.ambPhone())).append("\n\n");
        sb.append("🏥 Hospital: ").append(n.hospitalName()).append("\n");
        sb.append("Address: ").append(blankToDash(n.hospitalAddress())).append("\n");
        sb.append("Phone: ").append(blankToDash(n.hospitalPhone())).append("\n");
        sb.append("🆘 National Emergency: 112");
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
        if (template != null && template.equalsIgnoreCase("pukaar_sos")) {
            return buildPukaarSosParams(user, event);
        }
        return buildLegacyEmergencyParams(user, event);
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
        // Template is "Name - Phone" — put full address on the name side so it shows.
        params.add(serviceNameWithAddress(nearby.policeName, nearby.policeAddress)); // 13
        params.add(nearby.policePhone);        // 14
        params.add(serviceNameWithAddress(nearby.ambName, nearby.ambAddress));       // 15
        params.add(nearby.ambPhone);           // 16
        params.add(serviceNameWithAddress(nearby.hospitalName, nearby.hospitalAddress)); // 17
        params.add(nearby.hospitalPhone);      // 18
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
        params.add(serviceNameWithAddress(nearby.policeName, nearby.policeAddress));
        params.add(nearby.policePhone);
        params.add(serviceNameWithAddress(nearby.ambName, nearby.ambAddress));
        params.add(nearby.ambPhone);
        params.add(serviceNameWithAddress(nearby.hospitalName, nearby.hospitalAddress));
        params.add(nearby.hospitalPhone);
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
