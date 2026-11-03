package com.yobbu.dispatch.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_offers", schema = "dispatch")
public class DeliveryOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    @Column(name = "offered_at", nullable = false, updatable = false)
    private Instant offeredAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    protected DeliveryOffer() {}

    public DeliveryOffer(UUID deliveryId, UUID courierId, Instant expiresAt) {
        this.deliveryId = deliveryId;
        this.courierId = courierId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getDeliveryId() { return deliveryId; }
    public UUID getCourierId() { return courierId; }
    public OfferStatus getStatus() { return status; }
    public Instant getOfferedAt() { return offeredAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRespondedAt() { return respondedAt; }
}
