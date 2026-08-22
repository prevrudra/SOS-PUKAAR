package com.pukaar.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallengeEntity, UUID> {
    Optional<OtpChallengeEntity> findFirstByPhoneE164AndConsumedFalseOrderByCreatedAtDesc(String phoneE164);
}
