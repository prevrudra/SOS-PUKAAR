package com.pukaar.domain.elderly;

import com.pukaar.common.InactivityLevel;
import com.pukaar.domain.emergency.EmergencyOrchestrator;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inactivity V1 — single threshold based on last qualifying activity.
 * Any heartbeat resets the timer; one alert per episode until user is active again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InactivityService {
    private static final Set<Integer> ALLOWED_DURATIONS = Set.of(12, 18, 24, 30, 36);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final ElderlySettingsRepository settingsRepo;
    private final InactivityEpisodeRepository episodeRepo;
    private final InactivityAlertRepository alertRepo;
    private final EmergencyOrchestrator orchestrator;

    @Transactional
    public void scanAll() {
        Instant now = Instant.now();
        List<UserEntity> users = userRepo.findAll();
        for (UserEntity user : users) {
            ElderlySettingsEntity settings = settingsRepo.findById(user.getId()).orElse(null);
            if (settings == null || !settings.isInactivityMonitoringEnabled()) continue;
            if (user.getLastActivityAt() == null) continue;
            try {
                processUser(user, settings, now);
            } catch (Exception e) {
                log.error("Inactivity scan failed for user {}", user.getId(), e);
            }
        }
    }

    @Transactional
    public void onUserActivity(UUID userId) {
        resolveActiveEpisode(userId, "USER_ACTIVE");
    }

    @Transactional
    public void processUser(UserEntity user, ElderlySettingsEntity settings, Instant now) {
        int durationHours = normalizeDuration(settings.getDurationHours());
        long hoursQuiet = Duration.between(user.getLastActivityAt(), now).toHours();

        Optional<InactivityEpisodeEntity> active =
                episodeRepo.findFirstByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(user.getId());

        if (hoursQuiet < durationHours) {
            // Still within window — clear any stale unalerted episode / resolve if activity moved
            if (active.isPresent()) {
                InactivityEpisodeEntity ep = active.get();
                if (user.getLastActivityAt().isAfter(ep.getActivityAnchor())) {
                    resolve(ep, "ACTIVITY_RESET");
                } else if (!ep.isAlerted()) {
                    // Below threshold and never alerted — discard soft episode
                    resolve(ep, "BELOW_THRESHOLD");
                }
            }
            return;
        }

        // Threshold reached
        if (active.isPresent()) {
            InactivityEpisodeEntity episode = active.get();
            if (user.getLastActivityAt().isAfter(episode.getActivityAnchor())) {
                resolve(episode, "ACTIVITY_RESET");
                return;
            }
            if (episode.isAlerted()) {
                // Already alerted this cycle — wait for user activity before a new cycle
                return;
            }
            fireAlert(user, settings, episode, durationHours, now);
            return;
        }

        InactivityEpisodeEntity episode = InactivityEpisodeEntity.builder()
                .userId(user.getId())
                .activityAnchor(user.getLastActivityAt())
                .lastLevel(InactivityLevel.MEDIUM)
                .viewToken(newViewToken())
                .alerted(false)
                .build();
        episode = episodeRepo.save(episode);
        fireAlert(user, settings, episode, durationHours, now);
    }

    private void fireAlert(
            UserEntity user,
            ElderlySettingsEntity settings,
            InactivityEpisodeEntity episode,
            int durationHours,
            Instant now
    ) {
        UUID eventId = orchestrator.deliverInactivityAlertToTrusted(
                user.getId(), episode.getEventId(), episode.getViewToken(), durationHours);
        episode.setEventId(eventId);
        episode.setAlerted(true);
        episode.setLastLevel(InactivityLevel.MEDIUM);
        episode.setLastAlertAt(now);
        if (episode.getViewToken() == null) {
            episode.setViewToken(newViewToken());
        }
        episodeRepo.save(episode);
        alertRepo.save(InactivityAlertEntity.builder()
                .userId(user.getId())
                .episodeId(episode.getId())
                .level(InactivityLevel.MEDIUM)
                .message("Inactivity alert: no qualifying activity for " + durationHours + " hours")
                .build());
        log.info("Inactivity alert fired for user {} after {}h (token={})",
                user.getId(), durationHours, episode.getViewToken());
    }

    private void resolveActiveEpisode(UUID userId, String reason) {
        episodeRepo.findFirstByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(userId)
                .ifPresent(ep -> resolve(ep, reason));
    }

    private void resolve(InactivityEpisodeEntity episode, String reason) {
        episode.setResolvedAt(Instant.now());
        episodeRepo.save(episode);
        log.info("Inactivity episode {} resolved ({})", episode.getId(), reason);
    }

    public static int normalizeDuration(int hours) {
        if (ALLOWED_DURATIONS.contains(hours)) return hours;
        // Migrate legacy soft/medium/urgent values toward nearest allowed
        int best = 12;
        int bestDist = Integer.MAX_VALUE;
        for (int d : ALLOWED_DURATIONS) {
            int dist = Math.abs(d - hours);
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }

    private static String newViewToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
