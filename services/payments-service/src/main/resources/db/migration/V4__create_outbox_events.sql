CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    headers TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT NULL
);

CREATE INDEX idx_outbox_events_status_created ON outbox_events(status, created_at ASC);
CREATE INDEX idx_outbox_events_aggregate ON outbox_events(aggregate_type, aggregate_id);
