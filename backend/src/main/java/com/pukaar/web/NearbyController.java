package com.pukaar.web;

import com.pukaar.domain.nearby.NearbyPlacesService;
import com.pukaar.domain.subscription.PlanRegionService;
import com.pukaar.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/nearby")
@RequiredArgsConstructor
public class NearbyController {
    private final NearbyPlacesService nearbyPlacesService;
    private final PlanRegionService planRegionService;

    @GetMapping
    public Map<String, Object> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int limit
    ) {
        var userId = SecurityUtils.currentUserId();
        if (!planRegionService.locationServicesAllowed(userId, lat, lng)) {
            Map<String, Object> blocked = new LinkedHashMap<>();
            blocked.put("police", List.of());
            blocked.put("hospitals", List.of());
            blocked.put("ambulance", List.of(Map.of(
                    "name", "National Ambulance",
                    "phone", "108",
                    "address", "India plan — location services only inside India"
            )));
            blocked.put("latitude", lat);
            blocked.put("longitude", lng);
            blocked.put("regionBlocked", true);
            blocked.put("message", "India plan location services work only inside India. Upgrade to Global for international coverage.");
            return blocked;
        }
        return nearbyPlacesService.nearby(lat, lng, limit);
    }
}
