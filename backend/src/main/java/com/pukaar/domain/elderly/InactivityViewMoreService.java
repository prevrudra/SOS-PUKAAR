package com.pukaar.domain.elderly;

import com.pukaar.common.ContactRole;
import com.pukaar.common.PlanRegion;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.domain.emergency.EmergencyEventEntity;
import com.pukaar.domain.emergency.EmergencyEventRepository;
import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.subscription.PlanRegionService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class InactivityViewMoreService {
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(IST);

    private final InactivityEpisodeRepository episodeRepo;
    private final UserRepository userRepo;
    private final ElderlySettingsRepository settingsRepo;
    private final TrustedContactRepository contactRepo;
    private final EmergencyEventRepository eventRepo;
    private final NearbyPlacesService nearbyPlacesService;
    private final PlanRegionService planRegionService;
    private final PukaarProperties props;

    public Optional<String> renderHtml(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        return episodeRepo.findByViewToken(token).map(this::buildHtml);
    }

    private String buildHtml(InactivityEpisodeEntity episode) {
        UserEntity user = userRepo.findById(episode.getUserId()).orElse(null);
        String who = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName().trim()
                : (user != null ? user.getPhoneE164() : "PUKAAR user");
        String lastActive = user != null && user.getLastActivityAt() != null
                ? FMT.format(user.getLastActivityAt()) + " IST"
                : "Unknown";

        EmergencyEventEntity event = episode.getEventId() != null
                ? eventRepo.findById(episode.getEventId()).orElse(null)
                : null;

        String locationBlock = locationHtml(event, episode.getUserId());
        String trusted = contactsHtml(episode.getUserId(), List.of(ContactRole.HELP_BACKUP), "TRUSTED CONTACTS",
                "You may coordinate with the other trusted contact to check on " + esc(who) + ".");
        String help = contactsHtml(episode.getUserId(),
                List.of(ContactRole.HELP_MONITOR, ContactRole.DOCTOR, ContactRole.NEIGHBOUR),
                "PRE-SAVED HELP NUMBERS",
                "If required, you may call these pre-saved help numbers if they can provide immediate help.");
        String medical = medicalHtml(episode.getUserId());
        String nearby = nearbyHtml(event, episode.getUserId());

        return """
                <!DOCTYPE html>
                <html lang="en"><head>
                <meta charset="utf-8"/>
                <meta name="viewport" content="width=device-width, initial-scale=1"/>
                <title>PUKAAR — Additional Safety Information</title>
                <style>
                body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,sans-serif;background:#0b0b0b;color:#f5f5f5;margin:0;padding:20px;line-height:1.45}
                h1{color:#a78bfa;font-size:1.15rem;letter-spacing:.04em}
                h2{color:#fff;font-size:.95rem;margin:22px 0 8px;border-top:1px solid #333;padding-top:14px}
                .card{background:#161616;border-radius:12px;padding:14px 16px;margin:10px 0}
                a{color:#60a5fa}
                .muted{color:#9ca3af;font-size:.9rem}
                .em{color:#f87171;font-weight:700}
                </style></head><body>
                <h1>PUKAAR — ADDITIONAL SAFETY INFORMATION</h1>
                <div class="card">
                  <div><strong>%s</strong></div>
                  <div class="muted">Last active: %s</div>
                  %s
                </div>
                %s
                %s
                %s
                %s
                <h2>EMERGENCY</h2>
                <div class="card em">112 — National Emergency</div>
                <p class="muted">PUKAAR inactivity is a welfare check — it does not confirm an emergency.</p>
                </body></html>
                """.formatted(
                esc(who),
                esc(lastActive),
                locationBlock,
                trusted,
                help,
                nearby,
                medical
        );
    }

    private String locationHtml(EmergencyEventEntity event, UUID userId) {
        if (event == null || event.getLatitude() == null || event.getLongitude() == null) {
            return "<div class=\"muted\">Location: not available</div>";
        }
        if (!planRegionService.locationServicesAllowed(userId, event.getLatitude(), event.getLongitude())) {
            return "<div class=\"muted\">Location services are limited on the India plan outside India.</div>";
        }
        String maps = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                event.getLatitude(), event.getLongitude());
        return "<div>Current / last available location:<br/><a href=\"" + maps + "\">" + maps + "</a></div>";
    }

    private String contactsHtml(UUID userId, List<ContactRole> roles, String title, String note) {
        List<TrustedContactEntity> list = contactRepo
                .findByOwnerUserIdAndContactRoleInAndActiveTrue(userId, roles)
                .stream()
                .limit(2)
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
    private String nearbyHtml(EmergencyEventEntity event, UUID userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>NEAREST SERVICES</h2>");
        if (event == null || event.getLatitude() == null || event.getLongitude() == null) {
            sb.append("<div class=\"card muted\">Not available</div>");
            return sb.toString();
        }
        if (!planRegionService.locationServicesAllowed(userId, event.getLatitude(), event.getLongitude())) {
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

    public String publicViewUrl(String token) {
        String base = props.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "https://pukaar.app";
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/view/" + token;
    }
}
