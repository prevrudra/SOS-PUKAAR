package com.pukaar.domain.elderly;

import com.pukaar.common.InactivityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inactivity_episodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InactivityEpisodeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** lastActivityAt when this episode began (soft threshold crossed). */
    @Column(name = "activity_anchor", nullable = false)
    private Instant activityAnchor;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_level", nullable = false, length = 20)
    @Builder.Default
    private InactivityLevel lastLevel = InactivityLevel.SOFT;

    @Column(name = "last_contact_priority", nullable = false)
    @Builder.Default
    private int lastContactPriority = 0;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "last_alert_at")
    private Instant lastAlertAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
