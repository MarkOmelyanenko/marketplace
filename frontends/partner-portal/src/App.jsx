import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider, NotFound } from '@design-system';
import { WalletProvider } from './WalletContext';
import AppShell from './AppShell';
import Login from './pages/Login';
import OffersList from './pages/OffersList';
import CreateOffer from './pages/CreateOffer';
import OfferDetails from './pages/OfferDetails';

function PrivateRoute({ children }) {
  const partnerId = localStorage.getItem('partnerId');
  return partnerId
    ? (
      <WalletProvider>
        <AppShell>{children}</AppShell>
      </WalletProvider>
    )
    : <Navigate to="/login" replace />;
}

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/offers" element={<PrivateRoute><OffersList /></PrivateRoute>} />
          <Route path="/offers/new" element={<PrivateRoute><CreateOffer /></PrivateRoute>} />
          <Route path="/offers/:id" element={<PrivateRoute><OfferDetails /></PrivateRoute>} />
          <Route path="/" element={<Navigate to="/offers" replace />} />
          <Route path="*" element={<NotFound homePath="/offers" homeLabel="Go to My Offers" />} />
        </Routes>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
