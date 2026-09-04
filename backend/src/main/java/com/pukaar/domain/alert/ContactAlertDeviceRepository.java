package com.pukaar.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactAlertDeviceRepository extends JpaRepository<ContactAlertDeviceEntity, UUID> {
    Optional<ContactAlertDeviceEntity> findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(String phoneE164);
}
