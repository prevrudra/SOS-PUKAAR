package com.pukaar.domain.subscription;

import com.pukaar.common.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
    Optional<SubscriptionEntity> findFirstByUserIdAndStatusInOrderByEndsAtDesc(UUID userId, List<SubscriptionStatus> statuses);
    List<SubscriptionEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByStatus(SubscriptionStatus status);
}
