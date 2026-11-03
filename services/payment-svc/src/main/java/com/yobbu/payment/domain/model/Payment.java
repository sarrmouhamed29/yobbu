package com.yobbu.payment.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 10)
    private String currency = "XOF";

    @Column(nullable = false, length = 50)
    private String provider = "SIMULATED";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {}

    public Payment(UUID orderId, int amountCents, String currency, String idempotencyKey) {
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
    }

    public void markPaid(String providerReference) {
        this.status = PaymentStatus.PAID;
        this.providerReference = providerReference;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProviderReference() { return providerReference; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
