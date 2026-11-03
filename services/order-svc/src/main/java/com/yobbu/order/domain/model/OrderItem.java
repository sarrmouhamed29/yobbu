package com.yobbu.order.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "order_items", schema = "order")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private int unitPriceCents;

    protected OrderItem() {}

    public OrderItem(Order order, String name, int quantity, int unitPriceCents) {
        this.order = order;
        this.name = name;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public int getUnitPriceCents() { return unitPriceCents; }
}
