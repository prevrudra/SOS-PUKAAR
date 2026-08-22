package com.pukaar.domain.emergency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyLocationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(nullable = false)
    private Double latitude;
    @Column(nullable = false)
    private Double longitude;
    @Column(name = "accuracy_m")
    private Double accuracyM;
    @CreationTimestamp @Column(name = "recorded_at", updatable = false)
    private Instant recordedAt;
}
