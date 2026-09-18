package com.pukaar.domain.elderly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InactivityEpisodeRepository extends JpaRepository<InactivityEpisodeEntity, UUID> {
    Optional<InactivityEpisodeEntity> findFirstByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
