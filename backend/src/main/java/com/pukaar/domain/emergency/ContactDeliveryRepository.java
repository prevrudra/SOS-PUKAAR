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
            ORDER BY d.createdAt ASC
            """)
    List<ContactDeliveryEntity> findRetryable(
            @Param("maxAttempts") int maxAttempts,
            @Param("since") Instant since
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

    @Query("""
            SELECT d FROM ContactDeliveryEntity d
            WHERE d.status IN (com.pukaar.common.DeliveryStatus.SENT,
                               com.pukaar.common.DeliveryStatus.PENDING,
                               com.pukaar.common.DeliveryStatus.DELIVERED)
              AND d.acknowledgedAt IS NULL
              AND d.createdAt <= :olderThan
              AND d.createdAt >= :since
              AND (d.channel IS NULL OR d.channel NOT LIKE '%VOICE%')
            ORDER BY d.createdAt ASC
            """)
    List<ContactDeliveryEntity> findUnackedNeedingVoice(
            @Param("olderThan") Instant olderThan,
            @Param("since") Instant since
    );
}
