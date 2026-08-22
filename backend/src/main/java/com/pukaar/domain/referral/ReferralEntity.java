package com.pukaar.domain.referral;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referrals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "referrer_user_id", nullable = false)
    private UUID referrerUserId;
    @Column(name = "referred_user_id", nullable = false, unique = true)
    private UUID referredUserId;
    @Column(name = "referred_phone_hash", nullable = false, length = 64)
    private String referredPhoneHash;
    @Column(name = "referred_device_id", length = 128)
    private String referredDeviceId;
    @Column(name = "paid_activated", nullable = false)
    @Builder.Default
    private boolean paidActivated = false;
    @Column(name = "abuse_flagged", nullable = false)
    @Builder.Default
    private boolean abuseFlagged = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "activated_at")
    private Instant activatedAt;
}
