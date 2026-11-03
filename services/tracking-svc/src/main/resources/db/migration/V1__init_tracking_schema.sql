-- Tracking service schema — V1 initial migration

CREATE TABLE tracking.positions (
    id              BIGSERIAL   PRIMARY KEY,
    courier_id      UUID        NOT NULL,
    delivery_id     UUID,
    point           GEOMETRY(Point, 4326) NOT NULL,
    accuracy_meters REAL,
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Latest position lookup and spatial queries
CREATE INDEX ON tracking.positions(courier_id, recorded_at DESC);
CREATE INDEX ON tracking.positions USING GIST(point);
