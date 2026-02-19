CREATE TABLE idempotency_keys (
    idempotency_key VARCHAR PRIMARY KEY,
    partner_id VARCHAR NOT NULL,
    offer_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_partner_key ON idempotency_keys(partner_id, idempotency_key);
