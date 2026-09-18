package com.pukaar.domain.elderly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InactivityAlertRepository extends JpaRepository<InactivityAlertEntity, UUID> {
}
