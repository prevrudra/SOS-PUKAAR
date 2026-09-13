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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nearby emergency services via Google Places (preferred) with DB + OSM fallback.
 * Caches results briefly to avoid burning the low Places daily quota.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NearbyPlacesService {
    private static final double MAX_DB_DISTANCE_KM = 20.0;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    private final PoliceStationRepository policeRepo;
    private final HospitalRepository hospitalRepo;
    private final PukaarProperties props;
    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4_000);
        factory.setReadTimeout(8_000);
        return new RestTemplate(factory);
    }

    public Map<String, Object> nearby(double lat, double lng, int limit) {
        int lim = Math.max(1, Math.min(limit, 10));
        String cacheKey = String.format(Locale.ROOT, "%.3f,%.3f,%d", lat, lng, lim);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
            return deepCopy(cached.payload);
        }

        Map<String, Object> out = resolveNearby(lat, lng, lim);
        cache.put(cacheKey, new CacheEntry(out, System.currentTimeMillis() + CACHE_TTL_MS));
        return deepCopy(out);
    }

    private Map<String, Object> resolveNearby(double lat, double lng, int lim) {
        Map<String, Object> out = new LinkedHashMap<>();
        // Fetch a wider candidate set so we can prefer real hospitals over tiny clinics
        int fetch = Math.max(lim, 5);
        List<Map<String, Object>> dbPolice = withinDistance(fromDbPolice(lat, lng, fetch), lat, lng);
        List<Map<String, Object>> dbHospitals = withinDistance(fromDbHospitals(lat, lng, fetch), lat, lng);
        out.put("police", dbPolice);
        out.put("hospitals", dbHospitals);
        out.put("ambulance", nationalAmbulance());
        out.put("latitude", lat);
        out.put("longitude", lng);

        String key = props.getGoogle().getMapsApiKey();
        if (key != null && !key.isBlank()) {
            List<Map<String, Object>> police = googleNearbySafe(lat, lng, fetch, List.of("police"), "POLICE");
            if (police.isEmpty()) {
                police = googleTextSearchSafe(lat, lng, fetch, "police station", "POLICE", "police");
            }
            List<Map<String, Object>> hospitals = googleNearbySafe(lat, lng, fetch, List.of("hospital"), "HOSPITAL");
            if (hospitals.isEmpty()) {
                hospitals = googleTextSearchSafe(lat, lng, fetch, "major hospital emergency", "HOSPITAL", "hospital");
            }
            List<Map<String, Object>> ambulance = googleTextSearchSafe(lat, lng, fetch, "ambulance", "AMBULANCE", null);
            if (ambulance.isEmpty()) {
                ambulance = googleTextSearchSafe(lat, lng, fetch, "ambulance service", "AMBULANCE", null);
            }

            // Merge Google with DB (never discard a good DB hospital for a nearer clinic)
            if (!police.isEmpty()) {
                out.put("police", rankPlaces(merge(dbPolice, police, fetch * 2), lat, lng, lim, "POLICE"));
            } else {
                out.put("police", rankPlaces(dbPolice, lat, lng, lim, "POLICE"));
            }
            if (!hospitals.isEmpty()) {
                out.put("hospitals", rankPlaces(merge(dbHospitals, hospitals, fetch * 2), lat, lng, lim, "HOSPITAL"));
            } else {
                out.put("hospitals", rankPlaces(dbHospitals, lat, lng, lim, "HOSPITAL"));
            }
            if (!ambulance.isEmpty()) {
                List<Map<String, Object>> amb = new ArrayList<>(ambulance);
                amb.addAll(nationalAmbulance());
                out.put("ambulance", amb.stream().limit(lim + 2).toList());
            }
            boolean usedGoogle = !police.isEmpty() || !hospitals.isEmpty() || !ambulance.isEmpty();
            if (usedGoogle) {
                ensureNationalFallbacks(out);
                out.put("source", "GOOGLE");
                return out;
            }
        }

        try {
            enrichWithOsm(out, lat, lng, lim);
            out.put("police", rankPlaces(asPlaceList(out.get("police")), lat, lng, lim, "POLICE"));
            out.put("hospitals", rankPlaces(asPlaceList(out.get("hospitals")), lat, lng, lim, "HOSPITAL"));
            out.put("source", "DB+OSM");
        } catch (Exception e) {
            log.warn("Overpass nearby lookup failed: {}", e.getMessage());
            out.put("police", rankPlaces(dbPolice, lat, lng, lim, "POLICE"));
            out.put("hospitals", rankPlaces(dbHospitals, lat, lng, lim, "HOSPITAL"));
            out.put("source", "DB");
        }
        ensureNationalFallbacks(out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asPlaceList(Object obj) {
        if (obj instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        copy.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    out.add(copy);
                }
            }
            return out;
        }
        return List.of();
    }

    /**
     * Prefer real hospitals over clinics/doctors, real phones over national numbers,
     * then closer distance. Keeps large hospitals from losing to a nearer clinic.
     */
    private List<Map<String, Object>> rankPlaces(
            List<Map<String, Object>> places, double lat, double lng, int lim, String typeLabel
    ) {
        if (places == null || places.isEmpty()) return List.of();
        record Scored(Map<String, Object> place, double score) {}
        List<Scored> scored = new ArrayList<>();
        for (Map<String, Object> p : places) {
            Double plat = asDouble(p.get("latitude"));
            Double plng = asDouble(p.get("longitude"));
            double dist = (plat != null && plng != null) ? haversineKm(lat, lng, plat, plng) : 50.0;
            String name = String.valueOf(p.getOrDefault("name", "")).toLowerCase(Locale.ROOT);
            String phone = p.get("phone") == null ? "" : String.valueOf(p.get("phone"));
            String source = String.valueOf(p.getOrDefault("source", ""));
            double score = dist;
            if ("HOSPITAL".equals(typeLabel)) {
                if (name.contains("hospital") || name.contains("medical college") || name.contains("multi special")) {
                    score -= 4.0;
                } else if (name.contains("clinic") || name.contains("dental") || name.contains("diagnostic")) {
                    score += 6.0;
                } else if (name.contains("doctor") || name.contains("pharmacy")) {
                    score += 8.0;
                }
            }
            if ("NATIONAL".equals(source) || "112".equals(phone) || "100".equals(phone) || "108".equals(phone)) {
                score += 20.0;
            } else if (phone.isBlank()) {
                score += 3.0;
            }
            scored.add(new Scored(p, score));
        }
        scored.sort(Comparator.comparingDouble(Scored::score));
        return scored.stream().limit(lim).map(Scored::place).toList();
    }

    private void ensureNationalFallbacks(Map<String, Object> out) {
        Object policeObj = out.get("police");
        if (!(policeObj instanceof List<?> list) || list.isEmpty()) {
            out.put("police", nationalPolice());
        }
        Object hospitalObj = out.get("hospitals");
        if (!(hospitalObj instanceof List<?> hList) || hList.isEmpty()) {
            out.put("hospitals", nationalHospital());
        }
        if (!"GOOGLE".equals(out.get("source")) && !"DB+OSM".equals(out.get("source"))) {
            Object p = out.get("police");
            boolean nationalOnly = p instanceof List<?> pl && !pl.isEmpty()
                    && pl.get(0) instanceof Map<?, ?> m && "NATIONAL".equals(String.valueOf(m.get("source")));
            if (nationalOnly) out.put("source", "NATIONAL");
        }
    }

    private List<Map<String, Object>> nationalHospital() {
        return List.of(
                Map.of("name", "Nearest Hospital / Emergency", "phone", "112", "type", "HOSPITAL", "source", "NATIONAL")
        );
    }

    private List<Map<String, Object>> nationalPolice() {
        return List.of(
                Map.of("name", "Police Emergency", "phone", "100", "type", "POLICE", "source", "NATIONAL"),
                Map.of("name", "Emergency", "phone", "112", "type", "EMERGENCY", "source", "NATIONAL")
        );
    }

    private List<Map<String, Object>> nationalAmbulance() {
        return List.of(
                Map.of("name", "National Ambulance", "phone", "108", "type", "AMBULANCE", "source", "NATIONAL"),
                Map.of("name", "Emergency", "phone", "112", "type", "EMERGENCY", "source", "NATIONAL")
        );
    }

    private List<Map<String, Object>> googleNearbySafe(double lat, double lng, int lim, List<String> types, String typeLabel) {
        try {
            return googleNearby(lat, lng, lim, types, typeLabel);
        } catch (Exception e) {
            log.warn("Google Places searchNearby ({}) failed: {}", typeLabel, shortMsg(e));
            return List.of();
        }
    }

    private List<Map<String, Object>> googleTextSearchSafe(
            double lat, double lng, int lim, String query, String typeLabel, String includedType
    ) {
        try {
            return googleTextSearch(lat, lng, lim, query, typeLabel, includedType);
        } catch (Exception e) {
            log.warn("Google Places textSearch ({}) failed: {}", query, shortMsg(e));
            return List.of();
        }
    }

    private static String shortMsg(Exception e) {
        String m = e.getMessage();
        if (m == null) return e.getClass().getSimpleName();
        return m.length() > 180 ? m.substring(0, 180) + "…" : m;
    }

    private List<Map<String, Object>> googleNearby(double lat, double lng, int lim, List<String> types, String typeLabel) throws Exception {
        String key = props.getGoogle().getMapsApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("includedTypes", types);
        body.put("maxResultCount", Math.min(Math.max(lim, 5), 10));
        body.put("rankPreference", "DISTANCE");
        body.put("locationRestriction", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", 15000.0
                )
        ));
        HttpHeaders headers = googleHeaders(key);
        String json = restTemplate.postForObject(
                "https://places.googleapis.com/v1/places:searchNearby",
                new HttpEntity<>(body, headers),
                String.class
        );
        return parseGooglePlaces(json, typeLabel);
    }

    private List<Map<String, Object>> googleTextSearch(
            double lat, double lng, int lim, String query, String typeLabel, String includedType
    ) throws Exception {
        String key = props.getGoogle().getMapsApiKey();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", query);
        body.put("maxResultCount", Math.min(Math.max(lim, 5), 10));
        if (includedType != null && !includedType.isBlank()) {
            body.put("includedType", includedType);
        }
        body.put("locationBias", Map.of(
                "circle", Map.of(
                        "center", Map.of("latitude", lat, "longitude", lng),
                        "radius", 15000.0
                )
        ));
        HttpHeaders headers = googleHeaders(key);
        String json = restTemplate.postForObject(
                "https://places.googleapis.com/v1/places:searchText",
                new HttpEntity<>(body, headers),
                String.class
        );
        return parseGooglePlaces(json, typeLabel);
    }

    private HttpHeaders googleHeaders(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", key);
        headers.set("X-Goog-FieldMask",
                "places.displayName,places.formattedAddress,places.location,places.nationalPhoneNumber,places.internationalPhoneNumber,places.types");
        return headers;
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
            if (!matchesType(p, name, typeLabel)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            String phone = p.path("nationalPhoneNumber").asText(null);
            if (phone == null || phone.isBlank()) phone = p.path("internationalPhoneNumber").asText(null);
            if (phone == null || phone.isBlank()) {
                phone = switch (typeLabel) {
                    case "POLICE" -> "100";
                    case "HOSPITAL" -> "112";
                    case "AMBULANCE" -> "108";
                    default -> null;
                };
            }
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

    private static boolean matchesType(JsonNode place, String name, String typeLabel) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        JsonNode types = place.path("types");
        String typesJoined = "";
        if (types.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode t : types) sb.append(t.asText("")).append(' ');
            typesJoined = sb.toString().toLowerCase(Locale.ROOT);
        }
        return switch (typeLabel) {
            case "POLICE" -> typesJoined.contains("police")
                    || lowerName.contains("police")
                    || lowerName.contains("thana");
            case "HOSPITAL" -> {
                boolean hospitalType = typesJoined.contains("hospital");
                boolean weakType = typesJoined.contains("doctor") || typesJoined.contains("dentist")
                        || typesJoined.contains("pharmacy");
                boolean hospitalName = lowerName.contains("hospital")
                        || lowerName.contains("medical college")
                        || lowerName.contains("nursing home");
                boolean weakName = lowerName.contains("clinic") || lowerName.contains("dental")
                        || lowerName.contains("diagnostic") || lowerName.contains("pharmacy");
                // Prefer true hospitals; allow clinic only if no hospital-type signal at all
                yield (hospitalType || hospitalName) && !weakName
                        || (!weakType && !weakName && (lowerName.contains("medical") || typesJoined.contains("health")));
            }
            case "AMBULANCE" -> typesJoined.contains("ambulance")
                    || lowerName.contains("ambulance");
            default -> true;
        };
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

    private List<Map<String, Object>> withinDistance(List<Map<String, Object>> places, double lat, double lng) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> p : places) {
            Double plat = asDouble(p.get("latitude"));
            Double plng = asDouble(p.get("longitude"));
            if (plat == null || plng == null) continue;
            if (haversineKm(lat, lng, plat, plng) <= MAX_DB_DISTANCE_KM) out.add(p);
        }
        return out;
    }

    private static Double asDouble(Object v) {
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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
                [out:json][timeout:8];
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> deepCopy(Map<String, Object> src) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : src.entrySet()) {
            if (e.getValue() instanceof List<?> list) {
                List<Object> items = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> m) items.add(new LinkedHashMap<>(m));
                    else items.add(item);
                }
                copy.put(e.getKey(), items);
            } else {
                copy.put(e.getKey(), e.getValue());
            }
        }
        return copy;
    }

    private record CacheEntry(Map<String, Object> payload, long expiresAt) {}
}
