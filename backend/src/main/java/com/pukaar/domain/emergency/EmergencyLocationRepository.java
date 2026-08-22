package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyLocationRepository extends JpaRepository<EmergencyLocationEntity, UUID> {
    List<EmergencyLocationEntity> findByEventIdOrderByRecordedAtDesc(UUID eventId);
}
