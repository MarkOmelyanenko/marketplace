import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { ToastProvider, NotFound } from "@design-system";
import { getOpsToken } from "./api";
import AppShell from "./AppShell";
import Login from "./pages/Login";
import Search from "./pages/Search";
import PaymentDetails from "./pages/PaymentDetails";
import RefundRequests from "./pages/RefundRequests";

function ProtectedRoute({ children }) {
  const token = getOpsToken();
  return token ? (
    <AppShell>{children}</AppShell>
  ) : (
    <Navigate to="/login" replace />
  );
}

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/search"
            element={
              <ProtectedRoute>
                <Search />
              </ProtectedRoute>
            }
          />
          <Route
            path="/payments/:id"
            element={
              <ProtectedRoute>
                <PaymentDetails />
              </ProtectedRoute>
            }
          />
          <Route
            path="/refund-requests"
            element={
              <ProtectedRoute>
                <RefundRequests />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/search" replace />} />
          <Route
            path="*"
            element={<NotFound homePath="/search" homeLabel="Go to Search" />}
          />
        </Routes>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
