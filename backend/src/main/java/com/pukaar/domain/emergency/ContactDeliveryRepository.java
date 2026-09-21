package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContactDeliveryRepository extends JpaRepository<ContactDeliveryEntity, UUID> {
    List<ContactDeliveryEntity> findByEventId(UUID eventId);

    @Query("""
            SELECT d FROM ContactDeliveryEntity d
            WHERE d.status IN (com.pukaar.common.DeliveryStatus.PENDING,
                               com.pukaar.common.DeliveryStatus.FAILED,
                               com.pukaar.common.DeliveryStatus.UNKNOWN)
              AND d.attempts < :maxAttempts
              AND d.createdAt >= :since
              AND (d.attempts > 0 OR d.createdAt <= :graceBefore OR d.status <> com.pukaar.common.DeliveryStatus.PENDING)
            ORDER BY d.createdAt ASC
            """)
    List<ContactDeliveryEntity> findRetryable(
            @Param("maxAttempts") int maxAttempts,
            @Param("since") Instant since,
            @Param("graceBefore") Instant graceBefore
    );

    /** SENT via FCM/SMS only — WhatsApp still missing; retry WhatsApp. */
    @Query("""
            SELECT d FROM ContactDeliveryEntity d
            WHERE d.status = com.pukaar.common.DeliveryStatus.SENT
              AND d.attempts < :maxAttempts
              AND d.createdAt >= :since
              AND (d.channel IS NULL OR (d.channel NOT LIKE '%WHATSAPP%'))
            ORDER BY d.createdAt ASC
            """)
    List<ContactDeliveryEntity> findMissingWhatsApp(
            @Param("maxAttempts") int maxAttempts,
            @Param("since") Instant since
    );

    @Query("""
            SELECT d FROM ContactDeliveryEntity d
            WHERE d.status IN (com.pukaar.common.DeliveryStatus.SENT,
                               com.pukaar.common.DeliveryStatus.PENDING)
              AND d.acknowledgedAt IS NULL
              AND d.createdAt <= :olderThan
              AND d.createdAt >= :since
              AND (d.channelUsed IS NULL OR d.channelUsed <> 'SMS')
            ORDER BY d.createdAt ASC
            """)
    List<ContactDeliveryEntity> findUnackedNeedingSms(
            @Param("olderThan") Instant olderThan,
            @Param("since") Instant since
    );

    @Query(value = """
            SELECT d.* FROM emergency_contact_deliveries d
            WHERE d.status IN ('SENT', 'PENDING', 'DELIVERED')
              AND d.acknowledged_at IS NULL
              AND d.created_at <= :olderThan
              AND d.created_at >= :since
              AND (d.channel IS NULL OR d.channel NOT LIKE '%VOICE%')
              AND NOT EXISTS (
                  SELECT 1 FROM voice_sos_sent v
                  WHERE v.event_id = d.event_id
                    AND v.phone_e164 = d.contact_phone
              )
            ORDER BY d.created_at ASC
            """, nativeQuery = true)
    List<ContactDeliveryEntity> findUnackedNeedingVoice(
            @Param("olderThan") Instant olderThan,
            @Param("since") Instant since
    );

    @Query("""
            SELECT d FROM ContactDeliveryEntity d
            WHERE d.eventId = :eventId AND d.contactPhone = :phone
            """)
    List<ContactDeliveryEntity> findByEventIdAndContactPhone(
            @Param("eventId") UUID eventId,
            @Param("phone") String phone
    );

    /** Recent deliveries for WhatsApp read/delivered webhooks (open or recently closed SOS). */
    @Query(value = """
            SELECT d.* FROM emergency_contact_deliveries d
            JOIN emergency_events e ON e.id = d.event_id
            WHERE d.created_at >= :since
              AND RIGHT(regexp_replace(d.contact_phone, '[^0-9]', '', 'g'), 10) = :last10
            ORDER BY d.created_at DESC
            """, nativeQuery = true)
    List<ContactDeliveryEntity> findRecentByContactLast10(
            @Param("last10") String last10,
            @Param("since") Instant since
    );
}
