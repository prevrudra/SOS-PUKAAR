package com.pukaar.domain.emergency;

import com.pukaar.common.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emergency_contact_deliveries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactDeliveryEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "event_id", nullable = false)
    private UUID eventId;
    @Column(name = "contact_id")
    private UUID contactId;
    @Column(name = "contact_name", nullable = false)
    private String contactName;
    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;
    @Builder.Default
    private String channel = "PUSH";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;
    @Builder.Default
    private int attempts = 0;
    @Column(name = "last_error")
    private String lastError;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
