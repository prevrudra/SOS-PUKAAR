package com.pukaar.web;

import com.pukaar.common.HomeMode;
import com.pukaar.domain.user.AuthService;
import com.pukaar.security.SecurityUtils;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/auth/otp/request")
    public Map<String, Object> requestOtp(@RequestBody OtpRequest req) {
        return authService.requestOtp(req.getPhone());
    }

    @PostMapping("/auth/otp/verify")
    public Map<String, Object> verifyOtp(@RequestBody OtpVerifyRequest req) {
        return authService.verifyOtp(req.getPhone(), req.getCode(), req.getDeviceId(), req.getReferralCode());
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        return authService.toUserDto(authService.getUser(SecurityUtils.currentUserId()));
    }

    @PutMapping("/me")
    public Map<String, Object> updateMe(@RequestBody ProfileUpdateRequest req) {
        return authService.toUserDto(authService.updateProfile(
                SecurityUtils.currentUserId(),
                req.getFullName(),
                req.getLanguageCode(),
                req.getHomeMode(),
                req.getConsentLocation(),
                req.getConsentAudio(),
                req.getConsentTerms()
        ));
    }

    @PostMapping("/me/onboarding/complete")
    public Map<String, Object> completeOnboarding() {
        return authService.toUserDto(authService.completeOnboarding(SecurityUtils.currentUserId()));
    }

    @Data
    public static class OtpRequest {
        @NotBlank
        private String phone;
    }

    @Data
    public static class OtpVerifyRequest {
        @NotBlank private String phone;
        @NotBlank private String code;
        private String deviceId;
        private String referralCode;
    }

    @Data
    public static class ProfileUpdateRequest {
        private String fullName;
        private String languageCode;
        private HomeMode homeMode;
        private Boolean consentLocation;
        private Boolean consentAudio;
        private Boolean consentTerms;
    }
}
