CREATE TABLE refund_requests (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES orders(id),
    buyer_id VARCHAR NOT NULL,
    reason VARCHAR(100) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by VARCHAR(255),
    rejection_reason TEXT
);

CREATE UNIQUE INDEX idx_refund_requests_order_pending ON refund_requests(order_id) WHERE status = 'PENDING';

CREATE INDEX idx_refund_requests_order_id ON refund_requests(order_id);
CREATE INDEX idx_refund_requests_status ON refund_requests(status);
CREATE INDEX idx_refund_requests_created_at ON refund_requests(created_at DESC);

COMMENT ON TABLE refund_requests IS 'Buyer refund requests for PAID orders; ops approve/reject in dashboard';
