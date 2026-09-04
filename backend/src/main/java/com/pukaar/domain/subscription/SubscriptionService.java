package com.pukaar.domain.subscription;

import com.pukaar.common.ApiException;
import com.pukaar.common.SubscriptionPlan;
import com.pukaar.common.SubscriptionStatus;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.referral.ReferralRepository;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepo;
    private final ReferralRepository referralRepo;
    private final UserRepository userRepo;
    private final PukaarProperties props;

    @Transactional
    public SubscriptionEntity activateFromPayment(UUID userId, SubscriptionPlan plan, String paymentId, String platform) {
        UserEntity user = userRepo.findById(userId).orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
        if (!user.isMockDrillPassed()) {
            throw new ApiException("MOCK_DRILL_REQUIRED", "Complete mock drill before activation");
        }

        Optional<SubscriptionEntity> existing = subscriptionRepo.findFirstByUserIdAndStatusInOrderByEndsAtDesc(
                userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE));
        if (existing.isPresent()) {
            SubscriptionEntity sub = existing.get();
            if (plan != sub.getPlan()) {
                long referrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
                sub.setPlan(plan);
                sub.setPriceInr(priceFor(plan, referrals));
                sub.setFamilySlotLimit(plan == SubscriptionPlan.FAMILY
                        ? props.getSubscription().getFamilyMemberLimit() : 1);
            }
            sub.setStorePurchaseToken(paymentId);
            sub.setStorePlatform(platform);
            sub = subscriptionRepo.save(sub);
            user.setProtectionReady(true);
            userRepo.save(user);
            return sub;
        }

        long referrals = referralRepo.countByReferrerUserIdAndPaidActivatedTrueAndAbuseFlaggedFalse(userId);
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
                .storePlatform(platform)
                .storePurchaseToken(paymentId)
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
        return sub;
    }

    public int priceFor(SubscriptionPlan plan, long referrals) {
        int price = plan == SubscriptionPlan.FAMILY
                ? props.getSubscription().getFamilyPriceInr()
                : props.getSubscription().getIndividualPriceInr();
        if (plan == SubscriptionPlan.FAMILY && referrals >= props.getSubscription().getReferralsRequired()) {
            price = props.getSubscription().getReferralFamilyPriceInr();
        }
        return price;
    }

    public Map<String, Object> toDto(SubscriptionEntity s) {
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
}
