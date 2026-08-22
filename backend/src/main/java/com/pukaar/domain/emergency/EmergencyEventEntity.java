package com.pukaar.domain.emergency;

import com.pukaar.common.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private TriggerType triggerType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    @Builder.Default
    private EmergencyStatus status = EmergencyStatus.TRIGGERED;
    @Column(name = "is_mock_drill", nullable = false)
    @Builder.Default
    private boolean mockDrill = false;
    private Double latitude;
    private Double longitude;
    @Column(name = "location_accuracy_m")
    private Double locationAccuracyM;
    @Column(name = "location_acquired_at")
    private Instant locationAcquiredAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "call_112_status", nullable = false, length = 20)
    @Builder.Default
    private Call112Status call112Status = Call112Status.NOT_STARTED;
    @Column(name = "police_station_id")
    private UUID policeStationId;
    @Enumerated(EnumType.STRING)
    @Column(name = "closure_reason", length = 30)
    private ClosureReason closureReason;
    @Column(name = "closed_at")
    private Instant closedAt;
    @CreationTimestamp @Column(name = "started_at", updatable = false)
    private Instant startedAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
