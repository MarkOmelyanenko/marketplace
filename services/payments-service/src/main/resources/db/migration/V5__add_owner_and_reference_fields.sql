ALTER TABLE payments
    ADD COLUMN owner_type VARCHAR NOT NULL DEFAULT 'PARTNER',
    ADD COLUMN owner_id VARCHAR NOT NULL DEFAULT '',
    ADD COLUMN reference_type VARCHAR NOT NULL DEFAULT 'OFFER_LISTING',
    ADD COLUMN reference_id UUID NULL;

UPDATE payments SET owner_id = partner_id WHERE owner_id = '';

ALTER TABLE payments ALTER COLUMN owner_id DROP DEFAULT;

CREATE INDEX idx_payments_reference ON payments(reference_type, reference_id);
