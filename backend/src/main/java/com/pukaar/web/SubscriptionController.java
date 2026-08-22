package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.common.SubscriptionPlan;
import com.pukaar.common.SubscriptionStatus;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.referral.ReferralEntity;
import com.pukaar.domain.referral.ReferralRepository;
import com.pukaar.domain.subscription.SubscriptionEntity;
import com.pukaar.domain.subscription.SubscriptionRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionRepository subscriptionRepo;
    private final ReferralRepository referralRepo;
    private final UserRepository userRepo;
    private final PukaarProperties props;

    @GetMapping
    public Map<String, Object> current() {
        UUID userId = SecurityUtils.currentUserId();
        Optional<SubscriptionEntity> active = subscriptionRepo.findFirstByUserIdAndStatusInOrderByEndsAtDesc(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE));
        long successfulReferrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("plans", Map.of(
                "individual", props.getSubscription().getIndividualPriceInr(),
                "family", props.getSubscription().getFamilyPriceInr(),
                "referralFamily", props.getSubscription().getReferralFamilyPriceInr(),
                "referralsRequired", props.getSubscription().getReferralsRequired()
        ));
        m.put("successfulReferrals", successfulReferrals);
        m.put("eligibleForReferralFamilyPrice", successfulReferrals >= props.getSubscription().getReferralsRequired());
        active.ifPresentOrElse(s -> m.put("subscription", toDto(s)), () -> m.put("subscription", null));
        return m;
    }

    @PostMapping("/activate")
    @Transactional
    public Map<String, Object> activate(@RequestBody ActivateRequest req) {
        UUID userId = SecurityUtils.currentUserId();
        UserEntity user = userRepo.findById(userId).orElseThrow();
        if (!user.isMockDrillPassed()) {
            throw new ApiException("MOCK_DRILL_REQUIRED", "Complete mock drill before activation");
        }

        Optional<SubscriptionEntity> existing = subscriptionRepo.findFirstByUserIdAndStatusInOrderByEndsAtDesc(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE));
        if (existing.isPresent()) {
            SubscriptionEntity sub = existing.get();
            SubscriptionPlan requested = req.getPlan() == null ? sub.getPlan() : req.getPlan();
            if (requested != sub.getPlan()) {
                long referrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
                int price = priceFor(requested, referrals);
                sub.setPlan(requested);
                sub.setPriceInr(price);
                sub.setFamilySlotLimit(requested == SubscriptionPlan.FAMILY
                        ? props.getSubscription().getFamilyMemberLimit() : 1);
                if (req.getPurchaseToken() != null) sub.setStorePurchaseToken(req.getPurchaseToken());
                if (req.getStorePlatform() != null) sub.setStorePlatform(req.getStorePlatform());
                sub = subscriptionRepo.save(sub);
            }
            user.setProtectionReady(true);
            userRepo.save(user);
            Map<String, Object> dto = toDto(sub);
            dto.put("alreadyActive", true);
            return dto;
        }

        long referrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
        SubscriptionPlan plan = req.getPlan() == null ? SubscriptionPlan.INDIVIDUAL : req.getPlan();
        int price = priceFor(plan, referrals);
        Instant now = Instant.now();
        SubscriptionEntity sub = SubscriptionEntity.builder()
                .userId(userId)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .priceInr(price)
                .familySlotLimit(plan == SubscriptionPlan.FAMILY ? props.getSubscription().getFamilyMemberLimit() : 1)
                .startsAt(now)
                .endsAt(now.plus(365, ChronoUnit.DAYS))
                .graceEndsAt(now.plus(365 + props.getSubscription().getGraceDays(), ChronoUnit.DAYS))
                .storePlatform(req.getStorePlatform() == null ? "PLAY" : req.getStorePlatform())
                .storePurchaseToken(req.getPurchaseToken() == null ? "dev-token" : req.getPurchaseToken())
                .build();
        sub = subscriptionRepo.save(sub);
        user.setProtectionReady(true);
        userRepo.save(user);

        if (user.getReferredById() != null) {
            referralRepo.findByReferredUserId(userId)
                    .filter(r -> !r.isAbuseFlagged())
                    .ifPresent(r -> {
                        r.setPaidActivated(true);
                        r.setActivatedAt(Instant.now());
                        referralRepo.save(r);
                    });
        }
        Map<String, Object> dto = toDto(sub);
        dto.put("alreadyActive", false);
        return dto;
    }

    private int priceFor(SubscriptionPlan plan, long referrals) {
        int price = plan == SubscriptionPlan.FAMILY
                ? props.getSubscription().getFamilyPriceInr()
                : props.getSubscription().getIndividualPriceInr();
        if (plan == SubscriptionPlan.FAMILY && referrals >= props.getSubscription().getReferralsRequired()) {
            price = props.getSubscription().getReferralFamilyPriceInr();
        }
        return price;
    }

    @GetMapping("/referrals")
    public Map<String, Object> referrals() {
        UUID userId = SecurityUtils.currentUserId();
        List<ReferralEntity> list = referralRepo.findByReferrerUserId(userId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", list.size());
        m.put("successfulPaid", referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId));
        m.put("items", list.stream().map(r -> Map.of(
                "referredUserId", r.getReferredUserId(),
                "paidActivated", r.isPaidActivated(),
                "abuseFlagged", r.isAbuseFlagged(),
                "createdAt", r.getCreatedAt()
        )).toList());
        return m;
    }

    private Map<String, Object> toDto(SubscriptionEntity s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("plan", s.getPlan());
        m.put("status", s.getStatus());
        m.put("priceInr", s.getPriceInr());
        m.put("startsAt", s.getStartsAt());
        m.put("endsAt", s.getEndsAt());
        m.put("graceEndsAt", s.getGraceEndsAt());
        return m;
    }

    @Data
    public static class ActivateRequest {
        private SubscriptionPlan plan;
        private String purchaseToken;
        private String storePlatform;
    }
}
