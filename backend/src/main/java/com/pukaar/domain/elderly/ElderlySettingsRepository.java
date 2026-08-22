package com.pukaar.domain.elderly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ElderlySettingsRepository extends JpaRepository<ElderlySettingsEntity, UUID> {
}
