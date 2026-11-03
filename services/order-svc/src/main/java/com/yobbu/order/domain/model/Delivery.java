package com.yobbu.order.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries", schema = "order")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "courier_id")
    private UUID courierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.PENDING_ASSIGNMENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Delivery() {}

    public Delivery(UUID orderId) {
        this.orderId = orderId;
    }

    public void applyStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void assignCourier(UUID courierId) {
        this.courierId = courierId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getCourierId() { return courierId; }
    public DeliveryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
