package com.pukaar.domain.contact;

import com.pukaar.common.ContactRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrustedContactRepository extends JpaRepository<TrustedContactEntity, UUID> {
    List<TrustedContactEntity> findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(UUID ownerUserId);
    List<TrustedContactEntity> findByOwnerUserIdAndContactRoleInAndActiveTrue(UUID ownerUserId, List<ContactRole> roles);
}
