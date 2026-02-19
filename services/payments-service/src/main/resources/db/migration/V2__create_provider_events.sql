CREATE TABLE provider_events (
    provider_event_id VARCHAR PRIMARY KEY,
    provider_payment_id VARCHAR NOT NULL,
    payment_id UUID NOT NULL,
    event_type VARCHAR NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_provider_events_payment_id ON provider_events(payment_id);
