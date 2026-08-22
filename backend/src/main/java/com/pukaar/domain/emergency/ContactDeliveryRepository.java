package com.pukaar.domain.emergency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactDeliveryRepository extends JpaRepository<ContactDeliveryEntity, UUID> {
    List<ContactDeliveryEntity> findByEventId(UUID eventId);
}
