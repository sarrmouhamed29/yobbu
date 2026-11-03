package com.yobbu.order.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "delivery_address", nullable = false)
    private String deliveryAddress;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 10)
    private String currency = "XOF";

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Order() {}

    public Order(OrderType type, UUID customerId, String pickupAddress,
                 String deliveryAddress, String deliveryNotes, int amountCents, String currency) {
        this.type = type;
        this.customerId = customerId;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
        this.deliveryNotes = deliveryNotes;
        this.amountCents = amountCents;
        this.currency = currency;
    }

    public void applyStatus(OrderStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void addItem(String name, int quantity, int unitPriceCents) {
        items.add(new OrderItem(this, name, quantity, unitPriceCents));
    }

    public UUID getId() { return id; }
    public OrderType getType() { return type; }
    public OrderStatus getStatus() { return status; }
    public UUID getCustomerId() { return customerId; }
    public UUID getMerchantId() { return merchantId; }
    public String getPickupAddress() { return pickupAddress; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryNotes() { return deliveryNotes; }
    public int getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
