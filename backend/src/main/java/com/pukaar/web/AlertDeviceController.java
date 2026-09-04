package com.pukaar.web;

import com.pukaar.domain.alert.ContactAlertDeviceService;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/alert-devices")
@RequiredArgsConstructor
public class AlertDeviceController {
    private final ContactAlertDeviceService deviceService;
    private final UserRepository userRepo;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        return deviceService.register(req.getPhone(), req.getFcmToken(), req.getDeviceId(), req.getPlatform());
    }

    @GetMapping("/pending")
    public Map<String, Object> pending() {
        var user = userRepo.findById(SecurityUtils.currentUserId()).orElseThrow();
        return deviceService.pendingAlertForContact(user.getPhoneE164());
    }

    @Data
    public static class RegisterRequest {
        private String phone;
        private String fcmToken;
        private String deviceId;
        private String platform;
    }
}
