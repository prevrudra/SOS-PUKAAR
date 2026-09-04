package com.pukaar.domain.contact;

import com.pukaar.common.ContactRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trusted_contacts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrustedContactEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_role", nullable = false, length = 30)
    @Builder.Default
    private ContactRole contactRole = ContactRole.SOS_TRUSTED;
    private String relationship;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "priority_order", nullable = false)
    @Builder.Default
    private int priorityOrder = 1;
    @Builder.Default
    private boolean verified = false;
    @Builder.Default
    private boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
