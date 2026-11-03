-- Dispatch service schema — V1 initial migration

CREATE TABLE dispatch.couriers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255) NOT NULL,
    phone            VARCHAR(50)  NOT NULL UNIQUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE', -- AVAILABLE | BUSY | OFFLINE
    current_position GEOMETRY(Point, 4326),
    last_seen_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE dispatch.delivery_offers (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id  UUID        NOT NULL,
    courier_id   UUID        NOT NULL REFERENCES dispatch.couriers(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | ACCEPTED | REFUSED | EXPIRED
    offered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ
);

-- Spatial index for proximity queries
CREATE INDEX ON dispatch.couriers USING GIST(current_position);
CREATE INDEX ON dispatch.couriers(status);
CREATE INDEX ON dispatch.delivery_offers(delivery_id);
CREATE INDEX ON dispatch.delivery_offers(courier_id, status);
