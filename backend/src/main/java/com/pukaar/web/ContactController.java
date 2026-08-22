package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.common.ContactRole;
import com.pukaar.domain.contact.TrustedContactEntity;
import com.pukaar.domain.contact.TrustedContactRepository;
import com.pukaar.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
        TrustedContactEntity c = TrustedContactEntity.builder()
                .ownerUserId(SecurityUtils.currentUserId())
                .name(req.getName())
                .phoneE164(normalize(req.getPhone()))
                .contactRole(req.getRole() == null ? ContactRole.SOS_TRUSTED : req.getRole())
                .relationship(req.getRelationship())
                .priorityOrder(req.getPriorityOrder() == null ? 1 : req.getPriorityOrder())
                .build();
        return toDto(contactRepo.save(c));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody ContactRequest req) {
        TrustedContactEntity c = owned(id);
        if (req.getName() != null) c.setName(req.getName());
        if (req.getPhone() != null) c.setPhoneE164(normalize(req.getPhone()));
        if (req.getRole() != null) c.setContactRole(req.getRole());
        if (req.getRelationship() != null) c.setRelationship(req.getRelationship());
        if (req.getPriorityOrder() != null) c.setPriorityOrder(req.getPriorityOrder());
        return toDto(contactRepo.save(c));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable UUID id) {
        TrustedContactEntity c = owned(id);
        c.setActive(false);
        contactRepo.save(c);
        return Map.of("deleted", true);
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
        m.put("priorityOrder", c.getPriorityOrder());
        m.put("verified", c.isVerified());
        return m;
    }

    private String normalize(String phone) {
        String p = phone.trim().replace(" ", "");
        if (!p.startsWith("+")) p = p.length() == 10 ? "+91" + p : "+" + p;
        return p;
    }

    @Data
    public static class ContactRequest {
        @NotBlank private String name;
        @NotBlank private String phone;
        private ContactRole role;
        private String relationship;
        private Integer priorityOrder;
    }
}
