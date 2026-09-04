package com.pukaar.web;

import com.pukaar.common.ApiException;
import com.pukaar.common.SubscriptionPlan;
import com.pukaar.common.SubscriptionStatus;
import com.pukaar.config.PukaarProperties;
import com.pukaar.domain.payment.RazorpayService;
import com.pukaar.domain.referral.ReferralEntity;
import com.pukaar.domain.referral.ReferralRepository;
import com.pukaar.domain.subscription.SubscriptionEntity;
import com.pukaar.domain.subscription.SubscriptionRepository;
import com.pukaar.domain.subscription.SubscriptionService;
import com.pukaar.domain.user.UserEntity;
import com.pukaar.domain.user.UserRepository;
import com.pukaar.security.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionRepository subscriptionRepo;
    private final ReferralRepository referralRepo;
    private final UserRepository userRepo;
    private final PukaarProperties props;
    private final SubscriptionService subscriptionService;
    private final RazorpayService razorpayService;

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
        m.put("razorpayEnabled", razorpayService.isConfigured());
        active.ifPresentOrElse(s -> m.put("subscription", subscriptionService.toDto(s)), () -> m.put("subscription", null));
        return m;
    }

    @PostMapping("/activate")
    @Transactional
    public Map<String, Object> activate(@RequestBody ActivateRequest req) {
        if (razorpayService.isConfigured() && !"dev-bypass".equals(req.getPurchaseToken())) {
            throw new ApiException("USE_PAYMENT_FLOW", "Complete payment via Razorpay checkout");
        }
        UUID userId = SecurityUtils.currentUserId();
        SubscriptionPlan plan = req.getPlan() == null ? SubscriptionPlan.INDIVIDUAL : req.getPlan();
        String token = req.getPurchaseToken() == null ? "dev-token" : req.getPurchaseToken();
        String platform = req.getStorePlatform() == null ? "DEV" : req.getStorePlatform();
        SubscriptionEntity sub = subscriptionService.activateFromPayment(userId, plan, token, platform);
        Map<String, Object> dto = subscriptionService.toDto(sub);
        dto.put("alreadyActive", false);
        return dto;
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

    @Data
    public static class ActivateRequest {
        private SubscriptionPlan plan;
        private String purchaseToken;
        private String storePlatform;
    }
}
