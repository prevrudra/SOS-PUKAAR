package com.pukaar.web;

import com.pukaar.common.UserRole;
import com.pukaar.domain.admin.AdminService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.users(page, size);
    }

    @GetMapping("/subscriptions")
    public Map<String, Object> subscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.subscriptions(page, size);
    }

    @GetMapping("/payments")
    public Map<String, Object> payments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.payments(page, size);
    }

    @GetMapping("/recordings")
    public Map<String, Object> recordings(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.recordings(userId, page, size);
    }

    @GetMapping("/recordings/{segmentId}/content")
    public ResponseEntity<Resource> streamRecording(@PathVariable UUID segmentId) {
        Resource resource = adminService.streamRecording(segmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + segmentId + ".m4a\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(resource);
    }

    @PatchMapping("/users/{id}/role")
    public Map<String, Object> setRole(@PathVariable UUID id, @RequestBody RoleRequest req) {
        return adminService.setUserRole(id, req.getRole());
    }

    @Data
    public static class RoleRequest {
        private UserRole role;
    }
}
