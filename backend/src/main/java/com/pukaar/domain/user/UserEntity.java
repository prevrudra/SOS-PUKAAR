package com.pukaar.domain.user;

import com.pukaar.common.HomeMode;
import com.pukaar.common.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_e164", nullable = false, unique = true, length = 20)
    private String phoneE164;

    @Column(name = "phone_hash", nullable = false, unique = true, length = 64)
    private String phoneHash;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "language_code", nullable = false, length = 10)
    @Builder.Default
    private String languageCode = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "home_mode", nullable = false, length = 20)
    @Builder.Default
    private HomeMode homeMode = HomeMode.SOS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "referral_code", nullable = false, unique = true, length = 16)
    private String referralCode;

    @Column(name = "referred_by_id")
    private UUID referredById;

    @Column(name = "onboarding_complete", nullable = false)
    @Builder.Default
    private boolean onboardingComplete = false;

    @Column(name = "mock_drill_passed", nullable = false)
    @Builder.Default
    private boolean mockDrillPassed = false;

    @Column(name = "protection_ready", nullable = false)
    @Builder.Default
    private boolean protectionReady = false;

    @Column(name = "consent_location", nullable = false)
    @Builder.Default
    private boolean consentLocation = false;

    @Column(name = "consent_audio", nullable = false)
    @Builder.Default
    private boolean consentAudio = false;

    @Column(name = "consent_terms_at")
    private Instant consentTermsAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
