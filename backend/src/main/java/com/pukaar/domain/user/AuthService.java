package com.pukaar.domain.user;

import com.pukaar.common.ApiException;
import com.pukaar.common.HashUtil;
import com.pukaar.common.HomeMode;
import com.pukaar.config.AdminBootstrap;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.alert.YourBulkSmsSender;
import com.pukaar.domain.elderly.ElderlySettingsEntity;
import com.pukaar.domain.elderly.ElderlySettingsRepository;
import com.pukaar.domain.referral.ReferralEntity;
import com.pukaar.domain.referral.ReferralRepository;
import com.pukaar.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final OtpChallengeRepository otpRepo;
    private final UserRepository userRepo;
    private final ReferralRepository referralRepo;
    private final ElderlySettingsRepository elderlyRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PukaarProperties props;
    private final AdminBootstrap adminBootstrap;
    private final YourBulkSmsSender smsSender;

    @Transactional
    public Map<String, Object> requestOtp(String phoneE164) {
        String normalized = normalizePhone(phoneE164);
        String code = props.getOtp().isMockEnabled() ? props.getOtp().getMockCode() : HashUtil.numericOtp(props.getOtp().getLength());
        OtpChallengeEntity challenge = OtpChallengeEntity.builder()
                .phoneE164(normalized)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(Instant.now().plusSeconds(props.getOtp().getTtlSeconds()))
                .build();
        otpRepo.save(challenge);
        if (!props.getOtp().isMockEnabled()) {
            if (!smsSender.sendOtp(normalized, code)) {
                throw new ApiException("OTP_SEND_FAILED", "Could not send OTP SMS");
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("phone", normalized);
        resp.put("expiresInSeconds", props.getOtp().getTtlSeconds());
        if (props.getOtp().isMockEnabled()) {
            resp.put("devCode", code);
        }
        return resp;
    }

    @Transactional
    public Map<String, Object> verifyOtp(String phoneE164, String code, String deviceId, String referralCode) {
        String normalized = normalizePhone(phoneE164);
        OtpChallengeEntity challenge = otpRepo.findFirstByPhoneE164AndConsumedFalseOrderByCreatedAtDesc(normalized)
                .orElseThrow(() -> new ApiException("OTP_NOT_FOUND", "No active OTP"));
        if (challenge.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("OTP_EXPIRED", "OTP expired");
        }
        if (challenge.getAttempts() >= 5) {
            throw new ApiException("OTP_LOCKED", "Too many attempts");
        }
        challenge.setAttempts(challenge.getAttempts() + 1);
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            otpRepo.save(challenge);
            throw new ApiException("OTP_INVALID", "Invalid OTP");
        }
        challenge.setConsumed(true);
        otpRepo.save(challenge);

        UserEntity user = userRepo.findByPhoneE164(normalized).orElseGet(() -> createUser(normalized, deviceId, referralCode));
        if (deviceId != null) {
            user.setDeviceId(deviceId);
        }
        user.setLastActivityAt(Instant.now());
        adminBootstrap.promoteIfAdminPhone(user);
        userRepo.save(user);

        String access = jwtService.createAccessToken(user.getId(), user.getRole().name());
        String refresh = jwtService.createRefreshToken(user.getId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("accessToken", access);
        resp.put("refreshToken", refresh);
        resp.put("user", toUserDto(user));
        return resp;
    }

    private UserEntity createUser(String phone, String deviceId, String referralCode) {
        UUID referredBy = null;
        if (referralCode != null && !referralCode.isBlank()) {
            referredBy = userRepo.findByReferralCode(referralCode.trim().toUpperCase())
                    .map(UserEntity::getId)
                    .orElse(null);
        }
        UserEntity user = UserEntity.builder()
                .phoneE164(phone)
                .phoneHash(HashUtil.sha256(phone))
                .deviceId(deviceId)
                .referralCode(uniqueReferralCode())
                .referredById(referredBy)
                .lastActivityAt(Instant.now())
                .build();
        user = userRepo.save(user);
        elderlyRepo.save(ElderlySettingsEntity.builder().userId(user.getId()).build());

        if (referredBy != null) {
            boolean abuse = referredBy.equals(user.getId())
                    || (deviceId != null && userRepo.existsByDeviceId(deviceId) && referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(referredBy) > 0);
            referralRepo.save(ReferralEntity.builder()
                    .referrerUserId(referredBy)
                    .referredUserId(user.getId())
                    .referredPhoneHash(user.getPhoneHash())
                    .referredDeviceId(deviceId)
                    .abuseFlagged(abuse)
                    .build());
        }
        return user;
    }

    public UserEntity getUser(UUID userId) {
        return userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
    }

    @Transactional
    public UserEntity updateProfile(UUID userId, String fullName, String language, HomeMode homeMode,
                                    Boolean consentLocation, Boolean consentAudio, Boolean consentTerms) {
        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        if (fullName != null) user.setFullName(fullName);
        if (language != null) user.setLanguageCode(language);
        if (homeMode != null) user.setHomeMode(homeMode);
        if (consentLocation != null) user.setConsentLocation(consentLocation);
        if (consentAudio != null) user.setConsentAudio(consentAudio);
        if (Boolean.TRUE.equals(consentTerms)) user.setConsentTermsAt(Instant.now());
        user.setLastActivityAt(Instant.now());
        return userRepo.save(user);
    }

    @Transactional
    public UserEntity completeOnboarding(UUID userId) {
        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        user.setOnboardingComplete(true);
        return userRepo.save(user);
    }

    public Map<String, Object> toUserDto(UserEntity user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("phone", user.getPhoneE164());
        m.put("fullName", user.getFullName());
        m.put("languageCode", user.getLanguageCode());
        m.put("homeMode", user.getHomeMode());
        m.put("onboardingComplete", user.isOnboardingComplete());
        m.put("mockDrillPassed", user.isMockDrillPassed());
        m.put("protectionReady", user.isProtectionReady());
        m.put("referralCode", user.getReferralCode());
        m.put("consentLocation", user.isConsentLocation());
        m.put("consentAudio", user.isConsentAudio());
        m.put("role", user.getRole());
        return m;
    }

    private String uniqueReferralCode() {
        for (int i = 0; i < 10; i++) {
            String code = HashUtil.randomCode(8);
            if (userRepo.findByReferralCode(code).isEmpty()) return code;
        }
        throw new ApiException("REFERRAL_CODE_ERROR", "Could not allocate referral code");
    }

    private String normalizePhone(String phone) {
        String p = phone == null ? "" : phone.trim().replace(" ", "");
        if (!p.startsWith("+")) {
            if (p.length() == 10) p = "+91" + p;
            else p = "+" + p;
        }
        if (p.length() < 10) throw new ApiException("INVALID_PHONE", "Invalid phone number");
        return p;
    }
}
