package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEventEntity, UUID> {
    Optional<EmergencyEventEntity> findFirstByUserIdAndClosedAtIsNullOrderByStartedAtDesc(UUID userId);
    List<EmergencyEventEntity> findByUserIdOrderByStartedAtDesc(UUID userId);

    @Query(value = """
            SELECT e.* FROM emergency_events e
            JOIN emergency_contact_deliveries d ON d.event_id = e.id
            WHERE e.closed_at IS NULL AND d.contact_phone = :phone
            ORDER BY e.started_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<EmergencyEventEntity> findOpenAlertsForContactPhone(@Param("phone") String phone);
}
