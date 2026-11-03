package com.yobbu.tracking.domain.model;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions", schema = "tracking")
public class CourierPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "courier_id", nullable = false)
    private UUID courierId;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point point;

    @Column(name = "accuracy_meters")
    private Float accuracyMeters;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt = Instant.now();

    protected CourierPosition() {}

    public CourierPosition(UUID courierId, UUID deliveryId, Point point, Float accuracyMeters) {
        this.courierId = courierId;
        this.deliveryId = deliveryId;
        this.point = point;
        this.accuracyMeters = accuracyMeters;
    }

    public Long getId() { return id; }
    public UUID getCourierId() { return courierId; }
    public UUID getDeliveryId() { return deliveryId; }
    public Point getPoint() { return point; }
    public Float getAccuracyMeters() { return accuracyMeters; }
    public Instant getRecordedAt() { return recordedAt; }
}
