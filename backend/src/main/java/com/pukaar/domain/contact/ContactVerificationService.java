package com.pukaar.domain.contact;

import com.pukaar.common.ApiException;
import com.pukaar.common.ContactRole;
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
    private static final int RESEND_COOLDOWN_SECONDS = 30;

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

    /** Reject adding the owner's own phone as a trusted / inactivity contact. */
    public void enforceNotSelf(UUID ownerUserId, String phoneE164) {
        userRepo.findById(ownerUserId).ifPresent(owner -> {
            if (PhoneNumbers.sameNumber(owner.getPhoneE164(), phoneE164)) {
                throw new ApiException("SELF_CONTACT",
                        "You cannot add your own phone number as a trusted contact");
            }
        });
    }

    /** SOS + inactivity trusted contacts need OTP; pre-saved help numbers do not. */
    public static boolean requiresOtp(ContactRole role) {
        return role == ContactRole.SOS_TRUSTED || role == ContactRole.HELP_BACKUP;
    }

    /** True when a non-expired OTP was already issued — safe to skip re-send on save. */
    public boolean hasOutstandingOtp(TrustedContactEntity contact) {
        return contact != null
                && contact.getVerifyCodeHash() != null
                && contact.getVerifyCodeExpiresAt() != null
                && contact.getVerifyCodeExpiresAt().isAfter(Instant.now());
    }

    public record OtpIssueResult(TrustedContactEntity contact, boolean delivered, String channel, String message) {}

    @Transactional
    public OtpIssueResult issueAndSendOtp(TrustedContactEntity contact, String ownerDisplayName) {
        if (contact.getVerifySentAt() != null) {
            long since = ChronoUnit.SECONDS.between(contact.getVerifySentAt(), Instant.now());
            if (since >= 0 && since < RESEND_COOLDOWN_SECONDS) {
                // Do not wipe verified / rotate code on cooldown — just tell the client to wait.
                throw new ApiException("OTP_COOLDOWN",
                        "Please wait " + (RESEND_COOLDOWN_SECONDS - since) + " seconds before resending");
            }
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        contact.setVerified(false);
        contact.setVerifyCodeHash(passwordEncoder.encode(code));
        contact.setVerifyCodeExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        contact.setVerifySentAt(Instant.now());
        contact = contactRepo.save(contact);

        String who = ownerDisplayName == null || ownerDisplayName.isBlank()
                ? "a PUKAAR user" : ownerDisplayName.trim();
        String phone = contact.getPhoneE164();
        boolean sent = false;
        String channel = null;

        // Prefer WhatsApp — no DLT template restrictions.
        if (whatsApp.isConfigured()) {
            String waBody = "PUKAAR: " + who + " added you as a trusted contact.\n\n"
                    + "Your verification code is: *" + code + "*\n\n"
                    + "It expires in " + OTP_TTL_MINUTES + " minutes.\n\n"
                    + "Install PUKAAR High Alert to receive their safety alerts.";
            sent = whatsApp.sendText(phone, waBody);
            if (sent) {
                channel = "WHATSAPP";
                log.info("Contact verification OTP sent via WhatsApp to {}", phone);
            }
        }

        // Fallback to SMS using DLT OTP template format.
        if (!sent && smsSender.isConfigured()) {
            sent = smsSender.sendOtp(phone, code);
            if (sent) {
                channel = "SMS";
                log.info("Contact verification OTP sent via SMS to {}", phone);
            }
        }

        if (!sent) {
            log.warn("Could not deliver contact OTP to {} — code generated for verify API", phone);
            return new OtpIssueResult(contact, false, null,
                    "Could not deliver verification code. Ask them to check WhatsApp, or tap Resend.");
        }
        return new OtpIssueResult(contact, true, channel,
                "Verification code sent via " + channel + " to " + phone);
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
