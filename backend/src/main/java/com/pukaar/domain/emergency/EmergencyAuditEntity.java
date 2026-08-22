package com.pukaar.domain.emergency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "emergency_audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "actor_user_id")
    private UUID actorUserId;
    @Column(nullable = false, length = 80)
    private String action;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> detail;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
