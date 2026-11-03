package com.yobbu.payment.infrastructure.persistence;

import com.yobbu.payment.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
