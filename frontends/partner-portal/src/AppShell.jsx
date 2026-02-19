import { useState, useEffect } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Button, Input, useToast } from "@design-system";
import { useWallet } from "./WalletContext";
import { deposit, listMyPayments } from "./api";

function formatBalance(cents) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format((cents || 0) / 100);
}

export default function AppShell({ children }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const { balanceCents, setBalanceCents, refreshBalance } = useWallet();
  const [depositOpen, setDepositOpen] = useState(false);
  const [depositAmount, setDepositAmount] = useState("");
  const [depositLoading, setDepositLoading] = useState(false);
  const [earnedAfterCommissionCents, setEarnedAfterCommissionCents] = useState(null);

  const COMMISSION_RATE = 0.1;

  useEffect(() => {
    refreshBalance();
  }, [refreshBalance]);

  useEffect(() => {
    let cancelled = false;
    listMyPayments()
      .then((payments) => {
        if (cancelled) return;
        const salePayments = (payments || []).filter(
          (p) => p.status === "CAPTURED" && p.referenceType === "ORDER_PURCHASE"
        );
        const total = salePayments.reduce(
          (sum, p) => sum + (p.amountCents ?? 0),
          0
        );
        setEarnedAfterCommissionCents(Math.round(total * (1 - COMMISSION_RATE)));
      })
      .catch(() => setEarnedAfterCommissionCents(0));
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("partnerId");
    navigate("/login");
  };

  const handleDepositSubmit = async (e) => {
    e.preventDefault();
    const cents = Math.round(parseFloat(depositAmount) * 100);
    if (!Number.isFinite(cents) || cents < 1) {
      addToast("Invalid amount", "error");
      return;
    }
    setDepositLoading(true);
    try {
      const data = await deposit(cents, "USD");
      setBalanceCents(data.balanceCents);
      setDepositAmount("");
      setDepositOpen(false);
      addToast("Deposit successful", "success");
      refreshBalance();
    } catch (err) {
      addToast(err.message || "Deposit failed", "error");
    } finally {
      setDepositLoading(false);
    }
  };

  return (
    <div className="ds-page">
      <nav className="ds-nav">
        <div className="ds-nav__inner">
          <Link to="/offers" className="ds-nav__brand">
            Partner Portal
          </Link>
          <div className="ds-nav__links">
            <span
              className="ds-nav__link"
              style={{ marginRight: "var(--space-2)" }}
            >
              Balance:{" "}
              {balanceCents != null ? formatBalance(balanceCents) : "—"}
              {earnedAfterCommissionCents != null && (
                <>
                  {" "}
                  · Earned (after commission):{" "}
                  {formatBalance(earnedAfterCommissionCents)}
                </>
              )}
            </span>
            <Button
              type="button"
              variant="secondary"
              size="small"
              onClick={() => setDepositOpen(true)}
            >
              Deposit
            </Button>
            <Link
              to="/offers"
              className={`ds-nav__link ${location.pathname === "/offers" ? "ds-nav__link--active" : ""}`}
            >
              My Offers
            </Link>
            <Link
              to="/offers/new"
              className={`ds-nav__link ${location.pathname === "/offers/new" ? "ds-nav__link--active" : ""}`}
            >
              Create Offer
            </Link>
            <button className="ds-nav__logout" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </div>
      </nav>
      <main>{children}</main>

      {depositOpen && (
        <div
          className="ds-overlay"
          onClick={() => !depositLoading && setDepositOpen(false)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="ds-deposit-title"
        >
          <div className="ds-dialog" onClick={(e) => e.stopPropagation()}>
            <h2 id="ds-deposit-title" className="ds-dialog__title">
              Deposit
            </h2>
            <form onSubmit={handleDepositSubmit}>
              <div className="ds-dialog__body">
                <div style={{ marginBottom: "var(--space-3)" }}>
                  <span
                    className="ds-label"
                    style={{ display: "block", marginBottom: "var(--space-2)" }}
                  >
                    Quick amounts
                  </span>
                  <div
                    style={{
                      display: "flex",
                      gap: "var(--space-2)",
                      flexWrap: "wrap",
                    }}
                  >
                    {[10, 25, 50].map((amount) => (
                      <Button
                        key={amount}
                        type="button"
                        variant="secondary"
                        size="small"
                        onClick={() => setDepositAmount(String(amount))}
                      >
                        ${amount}
                      </Button>
                    ))}
                  </div>
                </div>
                <Input
                  label="Amount (USD)"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={depositAmount}
                  onChange={(e) => setDepositAmount(e.target.value)}
                  placeholder="e.g. 10.00"
                />
              </div>
              <div className="ds-dialog__actions">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setDepositOpen(false)}
                  disabled={depositLoading}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={depositLoading}
                >
                  {depositLoading ? "Depositing…" : "Deposit"}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
