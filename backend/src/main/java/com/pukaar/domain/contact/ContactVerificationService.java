package com.pukaar.domain.contact;

import com.pukaar.common.ApiException;
import com.pukaar.common.PhoneNumbers;
import com.pukaar.common.PlanRegion;
import com.pukaar.domain.alert.YourBulkSmsSender;
import com.pukaar.domain.alert.WhatsAppAlertSender;
import com.pukaar.domain.subscription.PlanRegionService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 15;

    private final TrustedContactRepository contactRepo;
    private final UserRepository userRepo;
    private final PlanRegionService planRegionService;
    private final YourBulkSmsSender smsSender;
    private final WhatsAppAlertSender whatsApp;
    private final PasswordEncoder passwordEncoder;

    public void enforcePhoneRegion(UUID ownerUserId, String phoneE164) {
        PlanRegion region = planRegionService.regionForUser(ownerUserId);
        PhoneNumbers.requireAllowedForRegion(phoneE164, region);
    }

    @Transactional
    public TrustedContactEntity issueAndSendOtp(TrustedContactEntity contact, String ownerDisplayName) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        contact.setVerified(false);
        contact.setVerifyCodeHash(passwordEncoder.encode(code));
        contact.setVerifyCodeExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        contact.setVerifySentAt(Instant.now());
        contact = contactRepo.save(contact);

        String who = ownerDisplayName == null || ownerDisplayName.isBlank() ? "a PUKAAR user" : ownerDisplayName.trim();
        String phone = contact.getPhoneE164();
        boolean sent = false;

        // Prefer WhatsApp — no DLT template restrictions.
        if (whatsApp.isConfigured()) {
            String waBody = "PUKAAR: " + who + " added you as a trusted contact.\n\n"
                    + "Your verification code is: *" + code + "*\n\n"
                    + "It expires in " + OTP_TTL_MINUTES + " minutes.\n\n"
                    + "Install PUKAAR High Alert to receive their safety alerts.";
            sent = whatsApp.sendText(phone, waBody);
            if (sent) {
                log.info("Contact verification OTP sent via WhatsApp to {}", phone);
            }
        }

        // Fallback to SMS using DLT OTP template format.
        if (!sent && smsSender.isConfigured()) {
            // Use sendOtp which applies the DLT-approved OTP template.
            sent = smsSender.sendOtp(phone, code);
            if (sent) {
                log.info("Contact verification OTP sent via SMS to {}", phone);
            }
        }

        if (!sent) {
            log.warn("Could not deliver contact OTP to {} — code generated for verify API", phone);
        }
        return contact;
    }

    @Transactional
    public TrustedContactEntity verify(UUID ownerId, UUID contactId, String code) {
        TrustedContactEntity c = contactRepo.findById(contactId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "Contact not found"));
        if (!c.getOwnerUserId().equals(ownerId)) {
            throw new ApiException("FORBIDDEN", "Not your contact");
        }
        if (c.isVerified()) {
            return c;
        }
        if (code == null || code.isBlank()) {
            throw new ApiException("OTP_REQUIRED", "Enter the verification code sent to your contact");
        }
        if (c.getVerifyCodeHash() == null || c.getVerifyCodeExpiresAt() == null) {
            throw new ApiException("OTP_NOT_SENT", "Request a verification code first");
        }
        if (c.getVerifyCodeExpiresAt().isBefore(Instant.now())) {
            throw new ApiException("OTP_EXPIRED", "Verification code expired — resend and try again");
        }
        if (!passwordEncoder.matches(code.trim(), c.getVerifyCodeHash())) {
            throw new ApiException("OTP_INVALID", "Incorrect verification code");
        }
        c.setVerified(true);
        c.setVerifyCodeHash(null);
        c.setVerifyCodeExpiresAt(null);
        return contactRepo.save(c);
    }

    public String ownerDisplayName(UUID ownerId) {
        return userRepo.findById(ownerId)
                .map(UserEntity::getFullName)
                .filter(n -> n != null && !n.isBlank())
                .orElse("PUKAAR user");
    }

    public Map<String, Object> regionDto(UUID userId) {
        PlanRegion region = planRegionService.regionForUser(userId);
        return Map.of(
                "region", region.name(),
                "indiaOnlyPhones", region == PlanRegion.INDIA,
                "internationalLocation", region == PlanRegion.GLOBAL
        );
    }
}
