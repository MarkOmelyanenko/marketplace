CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(32) NOT NULL,
    owner_id VARCHAR(255) NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uq_wallets_owner UNIQUE (owner_type, owner_id)
);

CREATE INDEX idx_wallets_owner ON wallets(owner_type, owner_id);

CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    amount_cents BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    reference_type VARCHAR(64),
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_wallet_transactions_wallet ON wallet_transactions(wallet_id, created_at DESC);
