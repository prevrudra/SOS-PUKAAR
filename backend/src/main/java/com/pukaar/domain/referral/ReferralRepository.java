package com.pukaar.domain.referral;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralRepository extends JpaRepository<ReferralEntity, UUID> {
    long countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(UUID referrerUserId);
    List<ReferralEntity> findByReferrerUserId(UUID referrerUserId);
    boolean existsByReferredUserId(UUID referredUserId);
    Optional<ReferralEntity> findByReferredUserId(UUID referredUserId);
}
