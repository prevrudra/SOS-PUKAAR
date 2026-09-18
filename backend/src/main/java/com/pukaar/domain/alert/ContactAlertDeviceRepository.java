package com.pukaar.domain.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContactAlertDeviceRepository extends JpaRepository<ContactAlertDeviceEntity, UUID> {
    Optional<ContactAlertDeviceEntity> findFirstByPhoneE164AndActiveTrueOrderByUpdatedAtDesc(String phoneE164);

    @Query(value = """
            SELECT d.* FROM contact_alert_devices d
            WHERE d.active = TRUE
              AND RIGHT(regexp_replace(d.phone_e164, '[^0-9]', '', 'g'), 10)
                = RIGHT(regexp_replace(:phone, '[^0-9]', '', 'g'), 10)
            ORDER BY d.updated_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<ContactAlertDeviceEntity> findActiveByPhoneLast10(@Param("phone") String phone);
}
