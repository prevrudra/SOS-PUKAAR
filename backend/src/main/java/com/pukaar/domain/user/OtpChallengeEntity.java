package com.pukaar.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp_challenges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpChallengeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;
    @Column(name = "code_hash", nullable = false)
    private String codeHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Builder.Default
    private int attempts = 0;
    @Builder.Default
    private boolean consumed = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
