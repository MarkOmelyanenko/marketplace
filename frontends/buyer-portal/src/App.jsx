import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider, NotFound } from '@design-system';
import { WalletProvider } from './WalletContext';
import AppShell from './AppShell';
import Login from './pages/Login';
import Catalog from './pages/Catalog';
import Orders from './pages/Orders';
import OrderDetails from './pages/OrderDetails';

function PrivateRoute({ children }) {
  const buyerId = localStorage.getItem('buyerId');
  return buyerId
    ? (
      <WalletProvider>
        <AppShell>{children}</AppShell>
      </WalletProvider>
    )
    : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter basename={import.meta.env.BASE_URL?.replace(/\/$/, '') || ''}>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/catalog" element={<PrivateRoute><Catalog /></PrivateRoute>} />
          <Route path="/orders" element={<PrivateRoute><Orders /></PrivateRoute>} />
          <Route path="/orders/:id" element={<PrivateRoute><OrderDetails /></PrivateRoute>} />
          <Route path="/" element={<Navigate to="/catalog" replace />} />
          <Route path="*" element={<NotFound homePath="/catalog" homeLabel="Go to Catalog" />} />
        </Routes>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
