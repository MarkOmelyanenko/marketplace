import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Input } from '@design-system';

export default function Login() {
  const [partnerId, setPartnerId] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!partnerId.trim()) {
      setError('Partner ID is required');
      return;
    }
    localStorage.setItem('partnerId', partnerId.trim());
    navigate('/offers');
  };

  return (
    <div className="ds-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
      <div className="ds-card" style={{ width: '100%', maxWidth: 400 }}>
        <h1 className="ds-page-title" style={{ marginBottom: 'var(--space-4)', textAlign: 'center' }}>
          Partner Portal
        </h1>
        <p className="ds-helper" style={{ marginBottom: 'var(--space-5)', textAlign: 'center' }}>
          Enter your Partner ID to manage your offers
        </p>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 'var(--space-4)' }}>
            <Input
              id="partnerId"
              label="Partner ID"
              value={partnerId}
              onChange={(e) => { setPartnerId(e.target.value); setError(''); }}
              placeholder="e.g. partner-123"
              error={error}
            />
          </div>
          <Button type="submit" variant="primary" style={{ width: '100%' }}>
            Log in
          </Button>
        </form>
      </div>
    </div>
  );
}
