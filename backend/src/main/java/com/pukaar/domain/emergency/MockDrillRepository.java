package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MockDrillRepository extends JpaRepository<MockDrillEntity, UUID> {
    Optional<MockDrillEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
}
