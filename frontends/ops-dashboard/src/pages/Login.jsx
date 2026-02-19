import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { setOpsToken } from '../api';
import { Button, Input } from '@design-system';

export default function Login() {
  const [token, setToken] = useState('ops-dev');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!token || token.trim() === '') {
      setError('Token is required');
      return;
    }
    setOpsToken(token.trim());
    navigate('/search');
  };

  return (
    <div className="ds-page" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh' }}>
      <div className="ds-card" style={{ width: '100%', maxWidth: 400 }}>
        <h1 className="ds-page-title" style={{ marginBottom: 'var(--space-4)', textAlign: 'center' }}>Ops Dashboard</h1>
        <p className="ds-helper" style={{ marginBottom: 'var(--space-5)', textAlign: 'center' }}>
          Enter your ops token to investigate payments
        </p>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 'var(--space-4)' }}>
            <Input
              id="token"
              label="Ops token"
              value={token}
              onChange={(e) => { setToken(e.target.value); setError(''); }}
              placeholder="ops-dev"
              error={error}
            />
          </div>
          <Button type="submit" variant="primary" style={{ width: '100%' }}>Log in</Button>
        </form>
      </div>
    </div>
  );
}
