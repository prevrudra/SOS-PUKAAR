package com.pukaar.domain.contact;

import com.pukaar.common.ContactRole;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Idempotently ensures the configured default SOS trusted contact exists for each user. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTrustedContactService {
    private final PukaarProperties props;
    private final TrustedContactRepository contactRepo;
    private final UserRepository userRepo;

    @Transactional
    public void ensureForUser(UUID userId) {
        var cfg = props.getDefaultTrustedContact();
        if (!cfg.isEnabled() || cfg.getPhone() == null || cfg.getPhone().isBlank()) {
            return;
        }
        UserEntity user = userRepo.findById(userId).orElse(null);
        if (user == null) return;

        String phone = PhoneNumbers.toE164(cfg.getPhone());
        if (PhoneNumbers.sameNumber(phone, user.getPhoneE164())) {
            return;
        }

        var existing = contactRepo.findByOwnerUserIdAndPhoneE164AndContactRole(
                userId, phone, ContactRole.SOS_TRUSTED);
        if (existing.isPresent()) {
            TrustedContactEntity c = existing.get();
            if (!c.isActive() || !c.isVerified()) {
                c.setActive(true);
                c.setVerified(true);
                if (cfg.getName() != null && !cfg.getName().isBlank()) {
                    c.setName(cfg.getName().trim());
                }
                contactRepo.save(c);
                log.info("Reactivated default trusted contact for user {}", userId);
            }
            return;
        }

        if (contactRepo.countByOwnerUserIdAndActiveTrue(userId) >= 10) {
            log.warn("Skipping default trusted contact for {} — contact limit reached", userId);
            return;
        }

        TrustedContactEntity created = TrustedContactEntity.builder()
                .ownerUserId(userId)
                .name(cfg.getName() != null && !cfg.getName().isBlank() ? cfg.getName().trim() : "PUKAAR Support")
                .phoneE164(phone)
                .contactRole(ContactRole.SOS_TRUSTED)
                .relationship(cfg.getRelationship())
                .priorityOrder(99)
                .verified(true)
                .active(true)
                .build();
        contactRepo.save(created);
        log.info("Added default trusted contact {} for user {}", phone, userId);
    }
}
