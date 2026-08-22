package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmergencyAuditRepository extends JpaRepository<EmergencyAuditEntity, UUID> {
    List<EmergencyAuditEntity> findByEventIdOrderByCreatedAtAsc(UUID eventId);
}
