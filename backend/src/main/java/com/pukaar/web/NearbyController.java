package com.pukaar.web;

import com.pukaar.domain.nearby.NearbyPlacesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/nearby")
@RequiredArgsConstructor
public class NearbyController {
    private final NearbyPlacesService nearbyPlacesService;

    @GetMapping
    public Map<String, Object> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return nearbyPlacesService.nearby(lat, lng, limit);
    }
}
