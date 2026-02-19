import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setBuyerId } from '../api';
import { Button, Input } from '@design-system';

export default function Login() {
  const [buyerId, setBuyerIdState] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!buyerId.trim()) {
      setError('Buyer ID is required');
      return;
    }
    setBuyerId(buyerId.trim());
    navigate('/catalog');
  };

  return (
    <div className="ds-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
      <div className="ds-card" style={{ width: '100%', maxWidth: 400 }}>
        <h1 className="ds-page-title" style={{ marginBottom: 'var(--space-4)', textAlign: 'center' }}>Buyer Portal</h1>
        <p className="ds-helper" style={{ marginBottom: 'var(--space-5)', textAlign: 'center' }}>
          Enter your Buyer ID to browse and purchase offers
        </p>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 'var(--space-4)' }}>
            <Input
              id="buyerId"
              label="Buyer ID"
              value={buyerId}
              onChange={(e) => { setBuyerIdState(e.target.value); setError(''); }}
              placeholder="e.g. buyer-123"
              error={error}
            />
          </div>
          <Button type="submit" variant="primary" style={{ width: '100%' }}>Log in</Button>
        </form>
      </div>
    </div>
  );
}
