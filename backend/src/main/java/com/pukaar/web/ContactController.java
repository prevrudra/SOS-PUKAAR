package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.common.ContactRole;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.domain.contact.ContactVerificationService;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
public class ContactController {
    /** SOS / HELP_MONITOR etc. — product caps applied per role group below. */
    private static final int MAX_SOS = 3;
    private static final int MAX_INACTIVITY_TRUSTED = 2;
    private static final int MAX_HELP_NUMBERS = 2;

    private final TrustedContactRepository contactRepo;
    private final ContactVerificationService verificationService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return contactRepo.findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(SecurityUtils.currentUserId())
                .stream().map(this::toDto).toList();
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody ContactRequest req) {
        UUID ownerId = SecurityUtils.currentUserId();
        String phone = normalize(req.getPhone());
        verificationService.enforcePhoneRegion(ownerId, phone);
        ContactRole role = req.getRole() == null ? ContactRole.SOS_TRUSTED : req.getRole();

        Optional<TrustedContactEntity> match = findMatching(ownerId, phone, role);
        if (match.isPresent()) {
            TrustedContactEntity c = match.get();
            applyRequest(c, req, phone, role);
            c.setActive(true);
            c = contactRepo.save(c);
            if (!c.isVerified()) {
                c = verificationService.issueAndSendOtp(c, verificationService.ownerDisplayName(ownerId));
            }
            return toDto(c);
        }

        enforceRoleLimit(ownerId, role);
        TrustedContactEntity c = TrustedContactEntity.builder()
                .ownerUserId(ownerId)
                .name(req.getName())
                .phoneE164(phone)
                .contactRole(role)
                .relationship(req.getRelationship())
                .notes(req.getNotes())
                .priorityOrder(req.getPriorityOrder() == null ? 1 : req.getPriorityOrder())
                .verified(false)
                .active(true)
                .build();
        try {
            c = contactRepo.save(c);
        } catch (DataIntegrityViolationException ex) {
            TrustedContactEntity existing = findMatching(ownerId, phone, role)
                    .orElseThrow(() -> new ApiException("CONTACT_EXISTS", "This contact already exists"));
            applyRequest(existing, req, phone, role);
            existing.setActive(true);
            c = contactRepo.save(existing);
        }
        c = verificationService.issueAndSendOtp(c, verificationService.ownerDisplayName(ownerId));
        return toDto(c);
    }

    @PostMapping("/{id}/verify")
    public Map<String, Object> verify(@PathVariable UUID id, @RequestBody(required = false) VerifyRequest req) {
        String code = req == null ? null : req.getCode();
        return toDto(verificationService.verify(SecurityUtils.currentUserId(), id, code));
    }

    @PostMapping("/{id}/resend-verification")
    public Map<String, Object> resend(@PathVariable UUID id) {
        TrustedContactEntity c = owned(id);
        if (c.isVerified()) {
            return toDto(c);
        }
        c = verificationService.issueAndSendOtp(c, verificationService.ownerDisplayName(SecurityUtils.currentUserId()));
        return toDto(c);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody ContactRequest req) {
        TrustedContactEntity c = owned(id);
        final UUID contactId = c.getId();
        UUID ownerId = SecurityUtils.currentUserId();
        String newPhone = req.getPhone() != null ? normalize(req.getPhone()) : c.getPhoneE164();
        verificationService.enforcePhoneRegion(ownerId, newPhone);
        ContactRole newRole = req.getRole() != null ? req.getRole() : c.getContactRole();
        boolean phoneChanged = !PhoneNumbers.sameNumber(c.getPhoneE164(), newPhone);

        Optional<TrustedContactEntity> conflict = findMatching(ownerId, newPhone, newRole)
                .filter(other -> !other.getId().equals(contactId));
        if (conflict.isPresent()) {
            TrustedContactEntity other = conflict.get();
            applyRequest(other, req, newPhone, newRole);
            if (req.getName() == null) other.setName(c.getName());
            if (req.getRelationship() == null && c.getRelationship() != null) {
                other.setRelationship(c.getRelationship());
            }
            if (req.getNotes() == null && c.getNotes() != null) other.setNotes(c.getNotes());
            other.setActive(true);
            c.setActive(false);
            contactRepo.save(c);
            other = contactRepo.save(other);
            if (phoneChanged || !other.isVerified()) {
                other = verificationService.issueAndSendOtp(other, verificationService.ownerDisplayName(ownerId));
            }
            return toDto(other);
        }

        applyRequest(c, req, newPhone, newRole);
        c.setActive(true);
        if (phoneChanged) {
            c.setVerified(false);
        }
        try {
            c = contactRepo.save(c);
        } catch (DataIntegrityViolationException ex) {
            TrustedContactEntity existing = findMatching(ownerId, newPhone, newRole)
                    .filter(other -> !other.getId().equals(contactId))
                    .orElseThrow(() -> new ApiException("CONTACT_EXISTS", "This contact already exists"));
            applyRequest(existing, req, newPhone, newRole);
            existing.setActive(true);
            c.setActive(false);
            contactRepo.save(c);
            c = contactRepo.save(existing);
        }
        if (phoneChanged || !c.isVerified()) {
            c = verificationService.issueAndSendOtp(c, verificationService.ownerDisplayName(ownerId));
        }
        return toDto(c);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        TrustedContactEntity c = owned(id);
        c.setActive(false);
        contactRepo.save(c);
        return Map.of("deleted", true);
    }

    private void applyRequest(TrustedContactEntity c, ContactRequest req, String phone, ContactRole role) {
        if (req.getName() != null) c.setName(req.getName());
        c.setPhoneE164(phone);
        c.setContactRole(role);
        if (req.getRelationship() != null) c.setRelationship(req.getRelationship());
        if (req.getNotes() != null) c.setNotes(req.getNotes());
        if (req.getPriorityOrder() != null) c.setPriorityOrder(req.getPriorityOrder());
    }

    private Optional<TrustedContactEntity> findMatching(UUID ownerId, String phone, ContactRole role) {
        Optional<TrustedContactEntity> exact =
                contactRepo.findByOwnerUserIdAndPhoneE164AndContactRole(ownerId, phone, role);
        if (exact.isPresent()) return exact;

        String digits = phone.startsWith("+") ? phone.substring(1) : phone;
        String suffix = digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
        if (suffix.isBlank()) return Optional.empty();

        List<TrustedContactEntity> all = contactRepo.findByOwnerUserId(ownerId);
        return all.stream()
                .filter(c -> c.getContactRole() == role)
                .filter(c -> {
                    String p = c.getPhoneE164() == null ? "" : c.getPhoneE164();
                    String d = p.startsWith("+") ? p.substring(1) : p.replaceAll("\\D", "");
                    return d.equals(digits) || (suffix.length() >= 10 && d.endsWith(suffix));
                })
                .max(Comparator.comparing(TrustedContactEntity::isActive)
                        .thenComparing(TrustedContactEntity::getUpdatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private TrustedContactEntity owned(UUID id) {
        TrustedContactEntity c = contactRepo.findById(id).orElseThrow(() -> new ApiException("NOT_FOUND", "Contact not found"));
        if (!c.getOwnerUserId().equals(SecurityUtils.currentUserId())) {
            throw new ApiException("FORBIDDEN", "Not your contact");
        }
        return c;
    }

    private Map<String, Object> toDto(TrustedContactEntity c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("phone", c.getPhoneE164());
        m.put("role", c.getContactRole());
        m.put("relationship", c.getRelationship());
        m.put("notes", c.getNotes());
        m.put("priorityOrder", c.getPriorityOrder());
        m.put("verified", c.isVerified());
        m.put("verificationPending", !c.isVerified());
        return m;
    }

    private void enforceRoleLimit(UUID ownerId, ContactRole role) {
        long count = contactRepo.countByOwnerUserIdAndContactRoleAndActiveTrue(ownerId, role);
        int max = switch (role) {
            case SOS_TRUSTED -> MAX_SOS;
            case HELP_BACKUP -> MAX_INACTIVITY_TRUSTED;
            case HELP_MONITOR, DOCTOR, NEIGHBOUR -> MAX_HELP_NUMBERS;
            default -> MAX_SOS;
        };
        // Help numbers share a combined pool of 2 across HELP_MONITOR/DOCTOR/NEIGHBOUR
        if (role == ContactRole.HELP_MONITOR || role == ContactRole.DOCTOR || role == ContactRole.NEIGHBOUR) {
            long helpTotal = contactRepo.countByOwnerUserIdAndContactRoleAndActiveTrue(ownerId, ContactRole.HELP_MONITOR)
                    + contactRepo.countByOwnerUserIdAndContactRoleAndActiveTrue(ownerId, ContactRole.DOCTOR)
                    + contactRepo.countByOwnerUserIdAndContactRoleAndActiveTrue(ownerId, ContactRole.NEIGHBOUR);
            if (helpTotal >= MAX_HELP_NUMBERS) {
                throw new ApiException("CONTACT_LIMIT", "Maximum " + MAX_HELP_NUMBERS + " pre-saved help numbers allowed");
            }
            return;
        }
        if (count >= max) {
            throw new ApiException("CONTACT_LIMIT",
                    "Maximum " + max + " contacts allowed for " + role.name());
        }
    }

    private String normalize(String phone) {
        return PhoneNumbers.toE164(phone);
    }

    @Data
    public static class ContactRequest {
        @NotBlank private String name;
        @NotBlank private String phone;
        private ContactRole role;
        private String relationship;
        private String notes;
        private Integer priorityOrder;
    }

    @Data
    public static class VerifyRequest {
        private String code;
    }
}
