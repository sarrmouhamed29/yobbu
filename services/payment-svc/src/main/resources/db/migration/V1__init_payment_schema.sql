-- Payment service schema — V1 initial migration
-- All payments are simulated (no real money, no real provider).

CREATE TABLE payment.payments (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID        NOT NULL UNIQUE,
    amount_cents        INTEGER     NOT NULL CHECK (amount_cents > 0),
    currency            VARCHAR(10) NOT NULL DEFAULT 'XOF',
    provider            VARCHAR(50) NOT NULL DEFAULT 'SIMULATED',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | PAID | FAILED
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    provider_reference  VARCHAR(255),
    failure_reason      TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ON payment.payments(order_id);
CREATE INDEX ON payment.payments(idempotency_key);
CREATE INDEX ON payment.payments(status);
