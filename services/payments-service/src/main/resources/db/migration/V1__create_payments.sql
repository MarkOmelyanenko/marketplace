CREATE TABLE payments (
    id UUID PRIMARY KEY,
    partner_id VARCHAR NOT NULL,
    offer_id UUID NOT NULL,
    amount_cents INT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR NOT NULL,
    provider_payment_id VARCHAR NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_payments_partner_created ON payments(partner_id, created_at DESC);
CREATE INDEX idx_payments_offer_id ON payments(offer_id);
