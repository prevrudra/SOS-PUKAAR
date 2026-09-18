package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.common.ContactRole;
import com.pukaar.common.PhoneNumbers;
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
    private final TrustedContactRepository contactRepo;

    @GetMapping
    public List<Map<String, Object>> list() {
        return contactRepo.findByOwnerUserIdAndActiveTrueOrderByPriorityOrderAsc(SecurityUtils.currentUserId())
                .stream().map(this::toDto).toList();
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody ContactRequest req) {
        UUID ownerId = SecurityUtils.currentUserId();
        String phone = normalize(req.getPhone());
        ContactRole role = req.getRole() == null ? ContactRole.SOS_TRUSTED : req.getRole();

        Optional<TrustedContactEntity> match = findMatching(ownerId, phone, role);
        if (match.isPresent()) {
            TrustedContactEntity c = match.get();
            applyRequest(c, req, phone, role);
            c.setActive(true);
            c.setVerified(true);
            return toDto(contactRepo.save(c));
        }

        if (contactRepo.countByOwnerUserIdAndActiveTrue(ownerId) >= 10) {
            throw new ApiException("CONTACT_LIMIT", "Maximum 10 trusted contacts allowed");
        }
        TrustedContactEntity c = TrustedContactEntity.builder()
                .ownerUserId(ownerId)
                .name(req.getName())
                .phoneE164(phone)
                .contactRole(role)
                .relationship(req.getRelationship())
                .notes(req.getNotes())
                .priorityOrder(req.getPriorityOrder() == null ? 1 : req.getPriorityOrder())
                .verified(true)
                .active(true)
                .build();
        try {
            return toDto(contactRepo.save(c));
        } catch (DataIntegrityViolationException ex) {
            // Soft-deleted or race: reuse the conflicting row instead of 409.
            TrustedContactEntity existing = findMatching(ownerId, phone, role)
                    .orElseThrow(() -> new ApiException("CONTACT_EXISTS", "This contact already exists"));
            applyRequest(existing, req, phone, role);
            existing.setActive(true);
            existing.setVerified(true);
            return toDto(contactRepo.save(existing));
        }
    }

    @PostMapping("/{id}/verify")
    public Map<String, Object> verify(@PathVariable UUID id, @RequestBody(required = false) VerifyRequest req) {
        TrustedContactEntity c = owned(id);
        c.setVerified(true);
        return toDto(contactRepo.save(c));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody ContactRequest req) {
        TrustedContactEntity c = owned(id);
        UUID ownerId = SecurityUtils.currentUserId();
        String newPhone = req.getPhone() != null ? normalize(req.getPhone()) : c.getPhoneE164();
        ContactRole newRole = req.getRole() != null ? req.getRole() : c.getContactRole();

        Optional<TrustedContactEntity> conflict = findMatching(ownerId, newPhone, newRole)
                .filter(other -> !other.getId().equals(c.getId()));
        if (conflict.isPresent()) {
            // Phone/role already on another row — merge into that row and retire this one.
            TrustedContactEntity other = conflict.get();
            applyRequest(other, req, newPhone, newRole);
            if (req.getName() == null) other.setName(c.getName());
            if (req.getRelationship() == null && c.getRelationship() != null) {
                other.setRelationship(c.getRelationship());
            }
            if (req.getNotes() == null && c.getNotes() != null) other.setNotes(c.getNotes());
            other.setActive(true);
            other.setVerified(true);
            c.setActive(false);
            contactRepo.save(c);
            return toDto(contactRepo.save(other));
        }

        applyRequest(c, req, newPhone, newRole);
        c.setActive(true);
        try {
            return toDto(contactRepo.save(c));
        } catch (DataIntegrityViolationException ex) {
            TrustedContactEntity existing = findMatching(ownerId, newPhone, newRole)
                    .filter(other -> !other.getId().equals(c.getId()))
                    .orElseThrow(() -> new ApiException("CONTACT_EXISTS", "This contact already exists"));
            applyRequest(existing, req, newPhone, newRole);
            existing.setActive(true);
            existing.setVerified(true);
            c.setActive(false);
            contactRepo.save(c);
            return toDto(contactRepo.save(existing));
        }
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

    /**
     * Match by exact E.164 + role, then by last-10-digit suffix + role
     * (covers formatting drift and soft-deleted rows).
     */
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
        return m;
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
