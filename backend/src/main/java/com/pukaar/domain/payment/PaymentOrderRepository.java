package com.pukaar.domain.payment;

import com.pukaar.common.PaymentOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrderEntity, UUID> {
    Optional<PaymentOrderEntity> findByRazorpayOrderId(String razorpayOrderId);
    Page<PaymentOrderEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(PaymentOrderStatus status);
}
