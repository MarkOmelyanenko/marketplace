import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { opsGetPayment, opsGetTimeline } from '../api';
import {
  Button, Card, Alert, StatusPill, Skeleton,
  CopyableId,
} from '@design-system';

export default function PaymentDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [payment, setPayment] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { loadData(); }, [id]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [paymentData, timelineData] = await Promise.all([
        opsGetPayment(id),
        opsGetTimeline(id),
      ]);
      setPayment(paymentData);
      setTimeline(timelineData || []);
    } catch (err) {
      if (err.message === 'UNAUTHORIZED') navigate('/login');
      else setError(err.message || 'Failed to load payment details');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => (dateString ? new Date(dateString).toLocaleString() : '');
  const formatAmount = (cents, currency) =>
    cents != null ? `$${(cents / 100).toFixed(2)} ${currency || 'USD'}` : '';

  if (loading) {
    return (
      <div className="ds-container">
        <Skeleton height={32} width={200} style={{ marginBottom: 'var(--space-4)' }} />
        <Card>
          <Skeleton height={20} width="100%" style={{ marginBottom: 'var(--space-3)' }} />
          <Skeleton height={20} width="60%" />
        </Card>
      </div>
    );
  }

  if (error && !payment) {
    return (
      <div className="ds-container">
        <Alert variant="error">{error}</Alert>
        <Button variant="primary" onClick={() => navigate('/search')}>Back to search</Button>
      </div>
    );
  }

  return (
    <div className="ds-container">
      <div style={{ marginBottom: 'var(--space-5)' }}>
        <Button variant="secondary" size="small" onClick={() => navigate('/search')} style={{ marginBottom: 'var(--space-4)' }}>
          ← Back to search
        </Button>
        <h1 className="ds-page-title">Payment details</h1>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {payment && (
        <>
          <Card style={{ marginBottom: 'var(--space-5)' }}>
            <h2 className="ds-page-title" style={{ fontSize: 'var(--text-h2)', marginBottom: 'var(--space-4)' }}>
              Core fields
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Payment ID</div>
                <CopyableId id={payment.id} />
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Partner ID</div>
                <strong>{payment.partnerId}</strong>
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Offer ID</div>
                <CopyableId id={payment.offerId} />
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Status</div>
                <StatusPill status={payment.status} />
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Amount</div>
                <strong>{formatAmount(payment.amountCents, payment.currency)}</strong>
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Currency</div>
                <span>{payment.currency}</span>
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Provider Payment ID</div>
                {payment.providerPaymentId
                  ? <CopyableId id={payment.providerPaymentId} />
                  : <span style={{ color: 'var(--color-text-muted)' }}>N/A</span>}
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Created At</div>
                <span>{formatDate(payment.createdAt)}</span>
              </div>
              <div>
                <div className="ds-helper" style={{ marginBottom: 'var(--space-1)' }}>Updated At</div>
                <span>{formatDate(payment.updatedAt)}</span>
              </div>
            </div>
          </Card>

          <Card>
            <h2 className="ds-page-title" style={{ fontSize: 'var(--text-h2)', marginBottom: 'var(--space-4)' }}>
              Timeline
            </h2>
            {timeline.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)' }}>
                {timeline.map((event, index) => (
                  <div
                    key={index}
                    style={{
                      padding: 'var(--space-4)',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg)',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 'var(--space-2)' }}>
                      <strong>{event.type}</strong>
                      <span className="ds-helper">{formatDate(event.at)}</span>
                    </div>
                    <div className="ds-helper">{event.details}</div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="ds-helper">No timeline events found.</p>
            )}
          </Card>
        </>
      )}
    </div>
  );
}
