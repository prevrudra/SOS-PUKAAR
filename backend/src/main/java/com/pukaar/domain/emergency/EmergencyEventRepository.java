package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEventEntity, UUID> {
    Optional<EmergencyEventEntity> findFirstByUserIdAndClosedAtIsNullOrderByStartedAtDesc(UUID userId);
    List<EmergencyEventEntity> findByUserIdOrderByStartedAtDesc(UUID userId);
}
