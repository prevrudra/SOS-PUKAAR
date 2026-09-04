package com.pukaar.domain.payment;

import com.pukaar.common.PaymentOrderStatus;
import com.pukaar.common.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentOrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan;
    @Column(name = "amount_inr", nullable = false)
    private int amountInr;
    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;
    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 64)
    private String razorpayOrderId;
    @Column(name = "razorpay_payment_id", length = 64)
    private String razorpayPaymentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.PENDING;
    @Column(name = "subscription_id")
    private UUID subscriptionId;
    @CreationTimestamp @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @Column(name = "paid_at")
    private Instant paidAt;
}
