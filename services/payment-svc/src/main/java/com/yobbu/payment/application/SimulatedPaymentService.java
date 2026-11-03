package com.yobbu.payment.application;

import com.yobbu.payment.domain.model.Payment;
import com.yobbu.payment.infrastructure.persistence.JpaPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Simulated mobile-money provider. No real money moves.
 * Idempotent: same idempotencyKey always returns the same payment.
 */
@Service
@Transactional
public class SimulatedPaymentService {

    private final JpaPaymentRepository repository;

    public SimulatedPaymentService(JpaPaymentRepository repository) {
        this.repository = repository;
    }

    public Payment initiate(UUID orderId, int amountCents, String currency, String idempotencyKey) {
        Optional<Payment> existing = repository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        Payment payment = new Payment(orderId, amountCents, currency, idempotencyKey);
        return repository.save(payment);
    }

    public Payment confirm(UUID paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Payment not found: " + paymentId));
        payment.markPaid("SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return repository.save(payment);
    }

    public Payment fail(UUID paymentId, String reason) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Payment not found: " + paymentId));
        payment.markFailed(reason);
        return repository.save(payment);
    }
}
