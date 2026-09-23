package com.pukaar.domain.emergency;

import com.pukaar.common.ContactRole;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.elderly.ElderlySettingsEntity;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.subscription.PlanRegionService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Public HTML page for SOS/HELP alerts — shareable link for trusted contacts.
 */
@Service
@RequiredArgsConstructor
public class SosViewMoreService {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(IST);

    private final EmergencyEventRepository eventRepo;
    private final UserRepository userRepo;
    private final ElderlySettingsRepository settingsRepo;
    private final TrustedContactRepository contactRepo;
    private final NearbyPlacesService nearbyPlacesService;
    private final PlanRegionService planRegionService;
    private final PukaarProperties props;

    public Optional<String> renderHtml(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return eventRepo.findByViewToken(token).map(this::buildHtml);
    }

    public String publicViewUrl(String token) {
        String base = props.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://pukaaralert.com/pukaar";
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/view/" + token;
    }

    private String buildHtml(EmergencyEventEntity event) {
        UserEntity user = userRepo.findById(event.getUserId()).orElse(null);
        String who = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName().trim()
                : (user != null ? user.getPhoneE164() : "PUKAAR user");
        String started = event.getStartedAt() != null
                ? FMT.format(event.getStartedAt()) + " IST"
                : "Unknown";
        String status = event.getClosedAt() == null ? "ACTIVE" : "CLOSED — user marked safe";
        String trigger = event.getTriggerType() != null ? event.getTriggerType().name() : "SOS";

        String locationBlock = locationHtml(event);
        String trusted = contactsHtml(event.getUserId(), List.of(ContactRole.SOS_TRUSTED), "TRUSTED CONTACTS",
                "You may coordinate with other trusted contacts to help " + esc(who) + ".");
        String help = contactsHtml(event.getUserId(),
                List.of(ContactRole.HELP_MONITOR, ContactRole.DOCTOR, ContactRole.NEIGHBOUR),
                "PRE-SAVED HELP NUMBERS",
                "Call these numbers if they can provide immediate help.");
        String medical = medicalHtml(event.getUserId());
        String nearby = nearbyHtml(event);

        return """
                <!DOCTYPE html>
                <html lang="en"><head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <title>PUKAAR — SOS Alert Information</title>
                <style>
                body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;background:#0b0b0b;color:#f5f5f5;margin:0;padding:20px;line-height:1.45}
                h1{color:#f87171;font-size:1.15rem;letter-spacing:.04em}
                h2{color:#fff;font-size:.95rem;margin:22px 0 8px;border-top:1px solid #333;padding-top:14px}
                .card{background:#161616;border-radius:12px;padding:14px 16px;margin:10px 0}
                a{color:#60a5fa}
                .muted{color:#9ca3af;font-size:.9rem}
                .em{color:#f87171;font-weight:700}
                .ok{color:#4ade80;font-weight:700}
                </style></head><body>
                <h1>PUKAAR — %s ALERT</h1>
                <div class="card">
                  <div><strong>%s</strong></div>
                  <div class="muted">Started: %s</div>
                  <div class="%s">Status: %s</div>
                  %s
                </div>
                %s
                %s
                %s
                %s
                <h2>EMERGENCY</h2>
                <div class="card em">112 — National Emergency</div>
                <p class="muted">PUKAAR — Information that can help when you need it most.</p>
                </body></html>
                """.formatted(
                esc(trigger),
                esc(who),
                esc(started),
                event.getClosedAt() == null ? "em" : "ok",
                esc(status),
                locationBlock,
                trusted,
                help,
                nearby,
                medical
        );
    }

    private String locationHtml(EmergencyEventEntity event) {
        if (event.getLatitude() == null || event.getLongitude() == null) {
            return "<div class=\"muted\">Location: not available</div>";
        }
        if (!planRegionService.locationServicesAllowed(event.getUserId(), event.getLatitude(), event.getLongitude())) {
            return "<div class=\"muted\">Location services are limited on the India plan outside India.</div>";
        }
        String maps = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                event.getLatitude(), event.getLongitude());
        return "<div>Last available location:<br/><a href=\"" + maps + "\">" + maps + "</a></div>";
    }

    private String contactsHtml(UUID userId, List<ContactRole> roles, String title, String note) {
        List<TrustedContactEntity> list = contactRepo
                .findByOwnerUserIdAndContactRoleInAndActiveTrue(userId, roles)
                .stream()
                .filter(TrustedContactEntity::isVerified)
                .limit(3)
                .toList();
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(esc(title)).append("</h2>");
        if (list.isEmpty()) {
            sb.append("<div class=\"card muted\">None provided</div>");
            return sb.toString();
        }
        for (TrustedContactEntity c : list) {
            String rel = c.getRelationship() == null || c.getRelationship().isBlank() ? "Contact" : c.getRelationship();
            sb.append("<div class=\"card\">")
                    .append(esc(c.getName())).append(" — ").append(esc(rel)).append(" — ")
                    .append("<a href=\"tel:").append(esc(c.getPhoneE164())).append("\">")
                    .append(esc(c.getPhoneE164())).append("</a></div>");
        }
        sb.append("<p class=\"muted\">").append(esc(note)).append("</p>");
        return sb.toString();
    }

    private String medicalHtml(UUID userId) {
        ElderlySettingsEntity s = settingsRepo.findById(userId).orElse(null);
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>EMERGENCY INFORMATION</h2><div class=\"card\">");
        boolean any = false;
        any |= line(sb, "Medical conditions", s.getMedicalConditions());
        any |= line(sb, "Medications", s.getMedications());
        any |= line(sb, "Allergies", s.getAllergies());
        any |= line(sb, "Blood group", s.getBloodGroup());
        if (!any) {
            sb.append("<span class=\"muted\">No medical information provided</span>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private boolean line(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) return false;
        sb.append("<div>").append(esc(label)).append(": ").append(esc(value.trim())).append("</div>");
        return true;
    }

    @SuppressWarnings("unchecked")
    private String nearbyHtml(EmergencyEventEntity event) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>NEAREST SERVICES</h2>");
        if (event.getLatitude() == null || event.getLongitude() == null) {
            sb.append("<div class=\"card muted\">Not available</div>");
            return sb.toString();
        }
        if (!planRegionService.locationServicesAllowed(event.getUserId(), event.getLatitude(), event.getLongitude())) {
            sb.append("<div class=\"card muted\">India plan — nearby services only inside India.</div>");
            return sb.toString();
        }
        Map<String, Object> nearby = nearbyPlacesService.nearby(event.getLatitude(), event.getLongitude(), 1);
        sb.append(placeCard("NEAREST POLICE", first(nearby.get("police"))));
        sb.append(placeCard("NEAREST AMBULANCE", first(nearby.get("ambulance"))));
        sb.append(placeCard("NEAREST HOSPITAL", first(nearby.get("hospitals"))));
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> first(Object raw) {
        if (raw instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        if (raw instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return null;
    }

    private String placeCard(String title, Map<String, Object> place) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>").append(esc(title)).append("</h2>");
        if (place == null || place.isEmpty()) {
            sb.append("<div class=\"card muted\">Not available</div>");
            return sb.toString();
        }
        Object name = place.getOrDefault("name", place.get("title"));
        Object address = place.get("address");
        Object phone = place.get("phone");
        sb.append("<div class=\"card\">");
        if (name != null) sb.append("<div><strong>").append(esc(String.valueOf(name))).append("</strong></div>");
        if (address != null && !String.valueOf(address).isBlank()) {
            sb.append("<div class=\"muted\">").append(esc(String.valueOf(address))).append("</div>");
        }
        if (phone != null && !String.valueOf(phone).isBlank()) {
            String p = String.valueOf(phone);
            sb.append("<div><a href=\"tel:").append(esc(p)).append("\">").append(esc(p)).append("</a></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
