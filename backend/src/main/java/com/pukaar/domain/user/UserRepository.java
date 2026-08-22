package com.pukaar.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhoneE164(String phoneE164);
    Optional<UserEntity> findByReferralCode(String referralCode);
    boolean existsByPhoneE164(String phoneE164);
    boolean existsByDeviceId(String deviceId);
}
