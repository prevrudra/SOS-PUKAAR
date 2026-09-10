package com.pukaar.domain.nearby;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.hospital.HospitalEntity;
import com.pukaar.domain.hospital.HospitalRepository;
import com.pukaar.domain.police.PoliceStationEntity;
import com.pukaar.domain.police.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Nearby emergency services via Google Places (preferred) with DB + OSM fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NearbyPlacesService {
    private final PoliceStationRepository policeRepo;
    private final HospitalRepository hospitalRepo;
    private final PukaarProperties props;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> nearby(double lat, double lng, int limit) {
        int lim = Math.max(1, Math.min(limit, 10));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("police", fromDbPolice(lat, lng, lim));
        out.put("hospitals", fromDbHospitals(lat, lng, lim));
        out.put("ambulance", nationalAmbulance());
        out.put("latitude", lat);
        out.put("longitude", lng);

        String key = props.getGoogle().getMapsApiKey();
        if (key != null && !key.isBlank()) {
            try {
                List<Map<String, Object>> police = googleNearby(lat, lng, lim, List.of("police"), "POLICE");
                List<Map<String, Object>> hospitals = googleNearby(lat, lng, lim, List.of("hospital"), "HOSPITAL");
                List<Map<String, Object>> ambulance = googleTextSearch(lat, lng, lim, "ambulance station");
                if (!police.isEmpty()) out.put("police", police);
                if (!hospitals.isEmpty()) out.put("hospitals", hospitals);
                if (!ambulance.isEmpty()) {
                    List<Map<String, Object>> amb = new ArrayList<>(nationalAmbulance());
                    amb.addAll(0, ambulance);
                    out.put("ambulance", amb.stream().limit(lim + 2).toList());
                }
                out.put("source", "GOOGLE");
                return out;
            } catch (Exception e) {
                log.warn("Google Places nearby failed: {}", e.getMessage());
            }
        }

        try {
            enrichWithOsm(out, lat, lng, lim);
            out.put("source", "DB+OSM");
        } catch (Exception e) {
            log.warn("Overpass nearby lookup failed: {}", e.getMessage());
            out.put("source", "DB");
        }
        return out;
    }

    private List<Map<String, Object>> nationalAmbulance() {
        return List.of(
                Map.of("name", "National Ambulance", "phone", "108", "type", "AMBULANCE", "source", "NATIONAL"),
                Map.of("name", "Emergency", "phone", "112", "type", "EMERGENCY", "source", "NATIONAL")
        );
    }

    private List<Map<String, Object>> googleNearby(double lat, double lng, int lim, List<String> types, String typeLabel) throws Exception {
        String key = props.getGoogle().getMapsApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("includedTypes", types);
        body.put("maxResultCount", lim);
        body.put("rankPreference", "DISTANCE");
        body.put("locationRestriction", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", 8000.0
                )
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", key);
        headers.set("X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location,places.nationalPhoneNumber,places.internationalPhoneNumber,places.types");
        String json = restTemplate.postForObject(
                "https://places.googleapis.com/v1/places:searchNearby",
                new HttpEntity<>(body, headers),
                String.class
        );
        return parseGooglePlaces(json, typeLabel);
    }

    private List<Map<String, Object>> googleTextSearch(double lat, double lng, int lim, String query) throws Exception {
        String key = props.getGoogle().getMapsApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", query);
        body.put("maxResultCount", lim);
        body.put("locationBias", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", 8000.0
                )
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", key);
        headers.set("X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location,places.nationalPhoneNumber,places.internationalPhoneNumber,places.types");
        String json = restTemplate.postForObject(
                "https://places.googleapis.com/v1/places:searchText",
                new HttpEntity<>(body, headers),
                String.class
        );
        return parseGooglePlaces(json, "AMBULANCE");
    }

    private List<Map<String, Object>> parseGooglePlaces(String json, String typeLabel) throws Exception {
        if (json == null || json.isBlank()) return List.of();
        JsonNode root = mapper.readTree(json);
        JsonNode places = root.path("places");
        if (!places.isArray()) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (JsonNode p : places) {
            String name = p.path("displayName").path("text").asText(null);
            if (name == null || name.isBlank()) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            String phone = p.path("nationalPhoneNumber").asText(null);
            if (phone == null || phone.isBlank()) phone = p.path("internationalPhoneNumber").asText(null);
            m.put("phone", phone);
            m.put("address", p.path("formattedAddress").asText(null));
            JsonNode loc = p.path("location");
            if (loc.has("latitude")) m.put("latitude", loc.path("latitude").asDouble());
            if (loc.has("longitude")) m.put("longitude", loc.path("longitude").asDouble());
            m.put("type", typeLabel);
            m.put("source", "GOOGLE");
            out.add(m);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private void enrichWithOsm(Map<String, Object> out, double lat, double lng, int lim) {
        List<Map<String, Object>> osm = overpass(lat, lng, lim);
        List<Map<String, Object>> osmPolice = osm.stream().filter(m -> "POLICE".equals(m.get("type"))).toList();
        List<Map<String, Object>> osmHospitals = osm.stream().filter(m -> "HOSPITAL".equals(m.get("type"))).toList();
        List<Map<String, Object>> osmAmbulance = osm.stream().filter(m -> "AMBULANCE".equals(m.get("type"))).toList();
        if (!osmPolice.isEmpty()) {
            out.put("police", merge((List<Map<String, Object>>) out.get("police"), osmPolice, lim));
        }
        if (!osmHospitals.isEmpty()) {
            out.put("hospitals", merge((List<Map<String, Object>>) out.get("hospitals"), osmHospitals, lim));
        }
        if (!osmAmbulance.isEmpty()) {
            List<Map<String, Object>> amb = new ArrayList<>((List<Map<String, Object>>) out.get("ambulance"));
            amb.addAll(0, osmAmbulance);
            out.put("ambulance", amb.stream().limit(lim + 2).toList());
        }
    }

    private List<Map<String, Object>> fromDbPolice(double lat, double lng, int lim) {
        return policeRepo.findNearest(lat, lng, lim).stream().map(this::policeDto).toList();
    }

    private List<Map<String, Object>> fromDbHospitals(double lat, double lng, int lim) {
        return hospitalRepo.findNearest(lat, lng, lim).stream().map(this::hospitalDto).toList();
    }

    private Map<String, Object> policeDto(PoliceStationEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("phone", p.getPhoneE164());
        m.put("address", p.getAddress());
        m.put("latitude", p.getLatitude());
        m.put("longitude", p.getLongitude());
        m.put("type", "POLICE");
        m.put("source", "DB");
        return m;
    }

    private Map<String, Object> hospitalDto(HospitalEntity h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", h.getName());
        m.put("phone", h.getPhoneE164());
        m.put("address", h.getAddress());
        m.put("latitude", h.getLatitude());
        m.put("longitude", h.getLongitude());
        m.put("type", "HOSPITAL");
        m.put("source", "DB");
        return m;
    }

    private List<Map<String, Object>> merge(List<Map<String, Object>> primary, List<Map<String, Object>> extra, int lim) {
        LinkedHashMap<String, Map<String, Object>> byName = new LinkedHashMap<>();
        for (Map<String, Object> m : primary) byName.put(String.valueOf(m.get("name")).toLowerCase(Locale.ROOT), m);
        for (Map<String, Object> m : extra) {
            byName.putIfAbsent(String.valueOf(m.get("name")).toLowerCase(Locale.ROOT), m);
        }
        return byName.values().stream().limit(lim).toList();
    }

    private List<Map<String, Object>> overpass(double lat, double lng, int lim) {
        double radiusM = 5000;
        String query = """
                [out:json][timeout:15];
                (
                  node["amenity"="police"](around:%d,%f,%f);
                  way["amenity"="police"](around:%d,%f,%f);
                  node["amenity"="hospital"](around:%d,%f,%f);
                  way["amenity"="hospital"](around:%d,%f,%f);
                  node["emergency"="ambulance_station"](around:%d,%f,%f);
                  node["amenity"="ambulance_station"](around:%d,%f,%f);
                );
                out center %d;
                """.formatted(
                (int) radiusM, lat, lng,
                (int) radiusM, lat, lng,
                (int) radiusM, lat, lng,
                (int) radiusM, lat, lng,
                (int) radiusM, lat, lng,
                (int) radiusM, lat, lng,
                lim * 3
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.USER_AGENT, "PUKAAR-Emergency/1.0");
        String body = "data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        Map<?, ?> resp = restTemplate.postForObject(
                "https://overpass-api.de/api/interpreter",
                new HttpEntity<>(body, headers),
                Map.class
        );
        if (resp == null || resp.get("elements") == null) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object elObj : (List<?>) resp.get("elements")) {
            if (!(elObj instanceof Map<?, ?> el)) continue;
            Map<?, ?> tags = el.get("tags") instanceof Map<?, ?> t ? t : Map.of();
            String name = tags.get("name") != null ? String.valueOf(tags.get("name")) : null;
            if (name == null || name.isBlank()) continue;
            String amenity = tags.get("amenity") != null ? String.valueOf(tags.get("amenity")) : "";
            String emergency = tags.get("emergency") != null ? String.valueOf(tags.get("emergency")) : "";
            String type = amenity.equals("police") ? "POLICE"
                    : amenity.equals("hospital") ? "HOSPITAL"
                    : (amenity.contains("ambulance") || emergency.contains("ambulance")) ? "AMBULANCE"
                    : null;
            if (type == null) continue;
            Double plat = el.get("lat") instanceof Number n ? n.doubleValue()
                    : el.get("center") instanceof Map<?, ?> c && c.get("lat") instanceof Number n2 ? n2.doubleValue() : null;
            Double plng = el.get("lon") instanceof Number n ? n.doubleValue()
                    : el.get("center") instanceof Map<?, ?> c && c.get("lon") instanceof Number n2 ? n2.doubleValue() : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("phone", tags.get("phone") != null ? String.valueOf(tags.get("phone"))
                    : tags.get("contact:phone") != null ? String.valueOf(tags.get("contact:phone")) : null);
            m.put("address", tags.get("addr:full") != null ? String.valueOf(tags.get("addr:full")) : null);
            m.put("latitude", plat);
            m.put("longitude", plng);
            m.put("type", type);
            m.put("source", "OSM");
            out.add(m);
        }
        return out;
    }
}
