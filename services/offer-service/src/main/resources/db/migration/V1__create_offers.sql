CREATE TABLE offers (
    id UUID PRIMARY KEY,
    partner_id VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    title VARCHAR(140) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_offers_partner_created ON offers(partner_id, created_at DESC);
CREATE INDEX idx_offers_partner_id ON offers(partner_id, id);
