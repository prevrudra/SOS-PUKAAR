package com.pukaar.domain.elderly;

import com.pukaar.common.InactivityLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inactivity_alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InactivityAlertEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InactivityLevel level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean acknowledged = false;

    @Column(name = "episode_id")
    private UUID episodeId;

    @Column(name = "contact_id")
    private UUID contactId;

    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
