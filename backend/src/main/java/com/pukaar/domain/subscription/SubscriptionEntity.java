package com.pukaar.domain.subscription;

import com.pukaar.common.SubscriptionPlan;
import com.pukaar.common.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.NONE;
    @Column(name = "price_inr", nullable = false)
    private int priceInr;
    @Column(name = "family_slot_limit", nullable = false)
    @Builder.Default
    private int familySlotLimit = 1;
    @Column(name = "starts_at")
    private Instant startsAt;
    @Column(name = "ends_at")
    private Instant endsAt;
    @Column(name = "grace_ends_at")
    private Instant graceEndsAt;
    @Column(name = "store_purchase_token")
    private String storePurchaseToken;
    @Column(name = "store_platform", length = 20)
    private String storePlatform;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
