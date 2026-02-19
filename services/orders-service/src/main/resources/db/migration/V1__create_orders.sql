CREATE TABLE orders (
    id UUID PRIMARY KEY,
    buyer_id VARCHAR NOT NULL,
    offer_id UUID NOT NULL,
    offer_title_snapshot VARCHAR(140) NOT NULL,
    amount_cents INT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR NOT NULL,
    payment_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_orders_buyer_created ON orders(buyer_id, created_at DESC);
CREATE INDEX idx_orders_offer_id ON orders(offer_id);
