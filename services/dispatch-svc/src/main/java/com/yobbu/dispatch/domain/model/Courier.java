package com.yobbu.dispatch.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "couriers", schema = "dispatch")
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourierStatus status = CourierStatus.OFFLINE;

    @Column(name = "current_position", columnDefinition = "geometry(Point,4326)")
    private Point currentPosition;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Courier() {}

    public Courier(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public void goOnline() {
        this.status = CourierStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void goOffline() {
        this.status = CourierStatus.OFFLINE;
        this.updatedAt = Instant.now();
    }

    public void updatePosition(Point position) {
        this.currentPosition = position;
        this.lastSeenAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public CourierStatus getStatus() { return status; }
    public Point getCurrentPosition() { return currentPosition; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
