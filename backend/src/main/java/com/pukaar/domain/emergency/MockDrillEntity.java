package com.pukaar.domain.emergency;

import com.pukaar.common.MockDrillResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mock_drills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MockDrillEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "event_id")
    private UUID eventId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MockDrillResult result = MockDrillResult.IN_PROGRESS;
    @Column(name = "location_ok")
    private Boolean locationOk;
    @Column(name = "contacts_ok")
    private Boolean contactsOk;
    @Column(name = "permissions_ok")
    private Boolean permissionsOk;
    @Column(name = "failure_notes")
    private String failureNotes;
    @Column(name = "confirmed_by_user", nullable = false)
    @Builder.Default
    private boolean confirmedByUser = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;
}
