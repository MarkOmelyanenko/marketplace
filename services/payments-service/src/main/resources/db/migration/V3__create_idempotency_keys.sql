CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR NOT NULL,
    partner_id VARCHAR NOT NULL,
    payment_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (idempotency_key, partner_id)
);
