-- Order service schema — V1 initial migration

CREATE TABLE "order".orders (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL,                         -- MEAL | PARCEL
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    customer_id     UUID        NOT NULL,
    merchant_id     UUID,                                         -- simulated in V1
    pickup_address  TEXT        NOT NULL,
    delivery_address TEXT       NOT NULL,
    delivery_notes  TEXT,
    amount_cents    INTEGER     NOT NULL CHECK (amount_cents >= 0),
    currency        VARCHAR(10) NOT NULL DEFAULT 'XOF',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE "order".order_items (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID        NOT NULL REFERENCES "order".orders(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    quantity        INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price_cents INTEGER    NOT NULL CHECK (unit_price_cents >= 0)
);

CREATE TABLE "order".deliveries (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID        NOT NULL UNIQUE REFERENCES "order".orders(id),
    courier_id  UUID,
    status      VARCHAR(30) NOT NULL DEFAULT 'PENDING_ASSIGNMENT',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit trail: every status change is appended here
CREATE TABLE "order".order_events (
    id          BIGSERIAL   PRIMARY KEY,
    order_id    UUID        NOT NULL REFERENCES "order".orders(id),
    event_name  VARCHAR(50) NOT NULL,
    from_status VARCHAR(30) NOT NULL,
    to_status   VARCHAR(30) NOT NULL,
    triggered_by UUID,       -- actor id (customer, operator, system)
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ON "order".orders(customer_id);
CREATE INDEX ON "order".orders(status);
CREATE INDEX ON "order".orders(created_at DESC);
CREATE INDEX ON "order".order_events(order_id, occurred_at DESC);
