import { createContext, useContext, useState, useCallback } from 'react';
import { getWalletBalance } from './api';

const WalletContext = createContext(null);

export function WalletProvider({ children }) {
  const [balanceCents, setBalanceCents] = useState(null);

  const refreshBalance = useCallback(async () => {
    try {
      const data = await getWalletBalance();
      setBalanceCents(data.balanceCents);
    } catch {
      setBalanceCents(0);
    }
  }, []);

  return (
    <WalletContext.Provider value={{ balanceCents, setBalanceCents, refreshBalance }}>
      {children}
    </WalletContext.Provider>
  );
}

export function useWallet() {
  const ctx = useContext(WalletContext);
  if (!ctx) return { balanceCents: null, setBalanceCents: () => {}, refreshBalance: () => Promise.resolve() };
  return ctx;
}
