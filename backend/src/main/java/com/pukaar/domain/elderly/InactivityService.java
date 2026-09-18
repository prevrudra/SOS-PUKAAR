package com.pukaar.domain.elderly;

import com.pukaar.common.InactivityLevel;
import com.pukaar.domain.emergency.ContactDeliveryEntity;
import com.pukaar.domain.emergency.ContactDeliveryRepository;
import com.pukaar.domain.emergency.EmergencyOrchestrator;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InactivityService {
    private final UserRepository userRepo;
    private final ElderlySettingsRepository settingsRepo;
    private final InactivityEpisodeRepository episodeRepo;
    private final InactivityAlertRepository alertRepo;
    private final ContactDeliveryRepository deliveryRepo;
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
        long hours = Duration.between(user.getLastActivityAt(), now).toHours();
        if (hours < settings.getSoftHours()) {
            resolveActiveEpisode(user.getId(), "BELOW_THRESHOLD");
            return;
        }

        Optional<InactivityEpisodeEntity> active =
                episodeRepo.findFirstByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(user.getId());

        if (active.isEmpty()) {
            startEpisode(user, now);
            return;
        }

        InactivityEpisodeEntity episode = active.get();
        if (user.getLastActivityAt().isAfter(episode.getActivityAnchor())) {
            resolve(episode, "ACTIVITY_RESET");
            return;
        }

        if (hours >= settings.getUrgentHours()
                && episode.getLastLevel().ordinal() < InactivityLevel.URGENT.ordinal()) {
            UUID eventId = orchestrator.deliverInactivityContactAlert(
                    user.getId(), episode.getEventId(), 1, InactivityLevel.URGENT);
            episode.setEventId(eventId);
            episode.setLastLevel(InactivityLevel.URGENT);
            episode.setLastContactPriority(1);
            episode.setLastAlertAt(now);
            episodeRepo.save(episode);
            log.info("Inactivity URGENT alert for user {} ({}h quiet)", user.getId(), hours);
            return;
        }

        if (hours >= settings.getMediumHours()
                && episode.getLastLevel() == InactivityLevel.SOFT) {
            UUID eventId = orchestrator.deliverInactivityContactAlert(
                    user.getId(), episode.getEventId(), 1, InactivityLevel.MEDIUM);
            episode.setEventId(eventId);
            episode.setLastLevel(InactivityLevel.MEDIUM);
            episode.setLastContactPriority(1);
            episode.setLastAlertAt(now);
            episodeRepo.save(episode);
            log.info("Inactivity MEDIUM alert for user {} ({}h quiet)", user.getId(), hours);
            return;
        }

        maybeEscalate(user, settings, episode, now);
    }

    private void startEpisode(UserEntity user, Instant now) {
        InactivityEpisodeEntity episode = InactivityEpisodeEntity.builder()
                .userId(user.getId())
                .activityAnchor(user.getLastActivityAt())
                .lastLevel(InactivityLevel.SOFT)
                .lastAlertAt(now)
                .build();
        episode = episodeRepo.save(episode);
        alertRepo.save(InactivityAlertEntity.builder()
                .userId(user.getId())
                .episodeId(episode.getId())
                .level(InactivityLevel.SOFT)
                .message("Silent monitoring: no phone activity detected since " + user.getLastActivityAt())
                .build());
        log.info("Inactivity SOFT episode started for user {}", user.getId());
    }

    private void maybeEscalate(
            UserEntity user,
            ElderlySettingsEntity settings,
            InactivityEpisodeEntity episode,
            Instant now
    ) {
        if (episode.getEventId() == null || episode.getLastAlertAt() == null) return;
        long minutesSince = Duration.between(episode.getLastAlertAt(), now).toMinutes();
        if (minutesSince < settings.getEscalationMinutes()) return;

        List<ContactDeliveryEntity> deliveries = deliveryRepo.findByEventId(episode.getEventId());
        if (deliveries.stream().anyMatch(d -> d.getAcknowledgedAt() != null)) {
            return;
        }

        int nextPriority = episode.getLastContactPriority() + 1;
        if (nextPriority > 3) return;

        UUID eventId = orchestrator.deliverInactivityContactAlert(
                user.getId(), episode.getEventId(), nextPriority, episode.getLastLevel());
        if (eventId == null) return;
        episode.setEventId(eventId);
        episode.setLastContactPriority(nextPriority);
        episode.setLastAlertAt(now);
        episodeRepo.save(episode);
        log.info("Inactivity escalated to contact priority {} for user {}", nextPriority, user.getId());
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
}
