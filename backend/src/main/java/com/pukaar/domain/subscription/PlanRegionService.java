package com.pukaar.domain.subscription;

import com.pukaar.common.PlanRegion;
import com.pukaar.common.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanRegionService {
    /** Approximate India bounding box for location entitlement checks. */
    private static final double INDIA_LAT_MIN = 6.0;
    private static final double INDIA_LAT_MAX = 37.5;
    private static final double INDIA_LNG_MIN = 68.0;
    private static final double INDIA_LNG_MAX = 97.5;

    private final SubscriptionRepository subscriptionRepo;

    /** Active subscription region, or INDIA when none (India-first default). */
    public PlanRegion regionForUser(UUID userId) {
        return subscriptionRepo.findFirstByUserIdAndStatusInOrderByEndsAtDesc(
                        userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE))
                .map(s -> s.getRegion() == null ? PlanRegion.INDIA : s.getRegion())
                .orElse(PlanRegion.INDIA);
    }

    public static boolean isInsideIndia(double lat, double lng) {
        return lat >= INDIA_LAT_MIN && lat <= INDIA_LAT_MAX
                && lng >= INDIA_LNG_MIN && lng <= INDIA_LNG_MAX;
    }

    public boolean locationServicesAllowed(UUID userId, Double lat, Double lng) {
        if (lat == null || lng == null) return true;
        PlanRegion region = regionForUser(userId);
        if (region == PlanRegion.GLOBAL) return true;
        return isInsideIndia(lat, lng);
    }

    public static PlanRegion parseRegion(String raw) {
        if (raw == null || raw.isBlank()) return PlanRegion.INDIA;
        String u = raw.trim().toUpperCase();
        if (u.contains("GLOBAL") || u.contains("INTERNATIONAL")) return PlanRegion.GLOBAL;
        return PlanRegion.INDIA;
    }
}
