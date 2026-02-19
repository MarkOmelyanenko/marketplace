import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getOrder, retryOrderPayment, createRefundRequest, cancelOrder } from "../api";
import { useWallet } from "../WalletContext";
import {
  Button,
  Card,
  Alert,
  StatusPill,
  Skeleton,
  CopyableId,
  TextArea,
  ConfirmDialog,
} from "@design-system";

const STATUS_EXPLANATIONS = {
  PENDING_PAYMENT: null, // Shown as Alert with Retry payment button below
  PAID: "Payment was successful! Your order is confirmed.",
  CANCELLED:
    "This order was cancelled. You can place a new order anytime from the catalog.",
};

export default function OrderDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { refreshBalance } = useWallet();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retrying, setRetrying] = useState(false);
  const [refundReason, setRefundReason] = useState("");
  const [refundDetails, setRefundDetails] = useState("");
  const [refundSubmitting, setRefundSubmitting] = useState(false);
  const [refundError, setRefundError] = useState("");
  const [showCancelConfirm, setShowCancelConfirm] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const pollingRef = useRef(null);
  const pollingStartRef = useRef(null);

  useEffect(() => {
    loadOrder();
    return () => stopPolling();
  }, [id]);

  const loadOrder = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await getOrder(id);
      setOrder(data);
      if (data.status === "PENDING_PAYMENT") startPolling();
      else stopPolling();
    } catch (err) {
      setError(err.message || "Failed to load order");
    } finally {
      setLoading(false);
    }
  };

  const startPolling = () => {
    stopPolling();
    pollingStartRef.current = Date.now();
    pollingRef.current = setInterval(async () => {
      if (Date.now() - pollingStartRef.current >= 30000) {
        stopPolling();
        return;
      }
      try {
        const data = await getOrder(id);
        setOrder(data);
        if (data.status !== "PENDING_PAYMENT") stopPolling();
      } catch (err) {
        console.error("Order polling error:", err);
      }
    }, 2000);
  };

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  const handleRefundRequest = async (e) => {
    e.preventDefault();
    if (!id || !order || order.status !== "PAID" || !refundReason.trim()) return;
    setRefundSubmitting(true);
    setRefundError("");
    try {
      await createRefundRequest(id, { reason: refundReason.trim(), details: refundDetails.trim() });
      await loadOrder();
      setRefundReason("");
      setRefundDetails("");
    } catch (err) {
      setRefundError(err.message || "Failed to submit refund request");
    } finally {
      setRefundSubmitting(false);
    }
  };

  const handleRetryPayment = async () => {
    if (!id || !order || order.status !== "PENDING_PAYMENT") return;
    setRetrying(true);
    setError("");
    try {
      await retryOrderPayment(id);
      refreshBalance();
      await loadOrder();
    } catch (err) {
      setError(err.message || "Payment retry failed");
    } finally {
      setRetrying(false);
    }
  };

  const handleCancelOrder = async () => {
    if (!id || !order || order.status !== "PENDING_PAYMENT") return;
    setCancelling(true);
    setError("");
    try {
      await cancelOrder(id);
      setShowCancelConfirm(false);
      stopPolling();
      await loadOrder();
    } catch (err) {
      setError(err.message || "Failed to cancel order");
    } finally {
      setCancelling(false);
    }
  };

  const formatPrice = (cents, currency = "USD") =>
    cents != null ? `$${(cents / 100).toFixed(2)} ${currency}` : "";

  const formatDate = (dateString) =>
    dateString ? new Date(dateString).toLocaleString() : "";

  if (loading) {
    return (
      <div className="ds-container ds-container--narrow">
        <Skeleton
          height={32}
          width={200}
          style={{ marginBottom: "var(--space-4)" }}
        />
        <Card>
          <Skeleton
            height={20}
            width="100%"
            style={{ marginBottom: "var(--space-3)" }}
          />
          <Skeleton
            height={20}
            width="60%"
            style={{ marginBottom: "var(--space-3)" }}
          />
          <Skeleton height={20} width="40%" />
        </Card>
      </div>
    );
  }

  if (error && !order) {
    return (
      <div className="ds-container ds-container--narrow">
        <Alert variant="error">{error}</Alert>
        <Button variant="primary" onClick={() => navigate("/orders")}>
          Back to orders
        </Button>
      </div>
    );
  }

  const statusExplanation = STATUS_EXPLANATIONS[order?.status];
  const statusExplanationText = statusExplanation ?? "";
  const isPaid = order?.status === "PAID";
  const isPendingPayment = order?.status === "PENDING_PAYMENT";

  return (
    <div className="ds-container ds-container--narrow">
      <div style={{ marginBottom: "var(--space-5)" }}>
        <Button
          variant="secondary"
          size="small"
          onClick={() => navigate("/orders")}
          style={{ marginBottom: "var(--space-4)" }}
        >
          ← Back to orders
        </Button>
        <h1 className="ds-page-title">
          {isPaid ? "Order confirmed" : "Order details"}
        </h1>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {isPaid && (
        <Alert variant="success">
          Payment successful! Your order has been confirmed. Thank you for your
          purchase.
        </Alert>
      )}

      {order && (
        <>
          {/* Receipt / summary card */}
          <Card
            style={{
              marginBottom: "var(--space-5)",
              borderLeft: isPaid ? "4px solid var(--color-success)" : undefined,
            }}
          >
            <h2
              className="ds-page-title"
              style={{
                fontSize: "var(--text-h2)",
                marginBottom: "var(--space-4)",
              }}
            >
              {isPaid ? "Receipt" : "Order summary"}
            </h2>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "var(--space-4)",
              }}
            >
              <div>
                <div
                  className="ds-helper"
                  style={{ marginBottom: "var(--space-1)" }}
                >
                  Order ID
                </div>
                <CopyableId id={order.id} />
              </div>
              <div>
                <div
                  className="ds-helper"
                  style={{ marginBottom: "var(--space-1)" }}
                >
                  Status
                </div>
                <StatusPill status={order.status} />
              </div>
              <div>
                <div
                  className="ds-helper"
                  style={{ marginBottom: "var(--space-1)" }}
                >
                  Offer
                </div>
                <strong>{order.offerTitle || "N/A"}</strong>
              </div>
              <div>
                <div
                  className="ds-helper"
                  style={{ marginBottom: "var(--space-1)" }}
                >
                  Amount
                  {order.quantity != null && order.quantity > 1 && (
                    <span style={{ marginLeft: "var(--space-2)" }}>
                      (Qty: {order.quantity})
                    </span>
                  )}
                </div>
                <strong
                  style={{
                    fontSize: "var(--text-h2)",
                    color: "var(--color-success)",
                  }}
                >
                  {formatPrice(order.amountCents, order.currency)}
                </strong>
              </div>
              <div>
                <div
                  className="ds-helper"
                  style={{ marginBottom: "var(--space-1)" }}
                >
                  Created
                </div>
                <span>{formatDate(order.createdAt)}</span>
              </div>
              {order.updatedAt && (
                <div>
                  <div
                    className="ds-helper"
                    style={{ marginBottom: "var(--space-1)" }}
                  >
                    Updated
                  </div>
                  <span>{formatDate(order.updatedAt)}</span>
                </div>
              )}
            </div>
          </Card>

          {/* Status explanation */}
          {(statusExplanationText || isPendingPayment) && (
            <Card style={{ marginBottom: "var(--space-5)" }}>
              <h2
                className="ds-page-title"
                style={{
                  fontSize: "var(--text-h2)",
                  marginBottom: "var(--space-3)",
                }}
              >
                What happens next?
              </h2>
              {isPendingPayment && (
                <>
                  <Alert variant="error" style={{ margin: 0 }}>
                    Payment could not be completed (e.g. insufficient balance).
                    Top up your balance via Deposit in the header, then click
                    Retry payment below.
                  </Alert>
                  <div
                    style={{
                      display: "flex",
                      gap: "var(--space-2)",
                      marginTop: "var(--space-3)",
                      flexWrap: "wrap",
                    }}
                  >
                    <Button
                      variant="primary"
                      onClick={handleRetryPayment}
                      disabled={retrying}
                    >
                      {retrying ? "Processing…" : "Retry payment"}
                    </Button>
                    <Button variant="secondary" onClick={loadOrder}>
                      Refresh status
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => setShowCancelConfirm(true)}
                      disabled={retrying}
                    >
                      Cancel order
                    </Button>
                  </div>
                  <ConfirmDialog
                    open={showCancelConfirm}
                    title="Cancel order?"
                    message="This order will be cancelled. You can place a new order anytime from the catalog."
                    confirmLabel="Cancel order"
                    cancelLabel="Keep order"
                    variant="secondary"
                    onConfirm={handleCancelOrder}
                    onCancel={() => setShowCancelConfirm(false)}
                    loading={cancelling}
                  />
                </>
              )}
              {order.status === "CANCELLED" && statusExplanationText && (
                <p style={{ color: "var(--color-text-muted)", margin: 0 }}>
                  {statusExplanationText}
                </p>
              )}
              {statusExplanationText &&
                !isPendingPayment &&
                order.status !== "CANCELLED" && (
                  <p style={{ color: "var(--color-text-muted)", margin: 0 }}>
                    {statusExplanationText}
                  </p>
                )}
            </Card>
          )}

          {/* Refund request (only for PAID orders) */}
          {isPaid && (
            <Card style={{ marginBottom: "var(--space-5)" }}>
              <h2
                className="ds-page-title"
                style={{
                  fontSize: "var(--text-h2)",
                  marginBottom: "var(--space-3)",
                }}
              >
                Refund
              </h2>
              {order.refundRequest?.status === "PENDING" && (
                <Alert variant="info">
                  Your refund request is under review. We will notify you once it is processed.
                </Alert>
              )}
              {order.refundRequest?.status === "APPROVED" && (
                <Alert variant="success">
                  Your refund request has been approved.
                </Alert>
              )}
              {order.refundRequest?.status === "REJECTED" && (
                <>
                  <Alert variant="error">
                    Your refund request was declined.
                    {order.refundRequest.rejectionReason && (
                      <span style={{ display: "block", marginTop: "var(--space-2)" }}>
                        Reason: {order.refundRequest.rejectionReason}
                      </span>
                    )}
                  </Alert>
                  <p className="ds-helper" style={{ marginTop: "var(--space-2)" }}>
                    You can submit a new request below if needed.
                  </p>
                </>
              )}
              {(!order.refundRequest || order.refundRequest.status === "REJECTED") && (
                <form onSubmit={handleRefundRequest}>
                  {refundError && (
                    <Alert variant="error" style={{ marginBottom: "var(--space-3)" }}>
                      {refundError}
                    </Alert>
                  )}
                  <div style={{ marginBottom: "var(--space-3)" }}>
                    <label htmlFor="refund-reason" className="ds-label">
                      Reason for refund <span style={{ color: "var(--color-error)" }}>*</span>
                    </label>
                    <select
                      id="refund-reason"
                      className="ds-input"
                      value={refundReason}
                      onChange={(e) => setRefundReason(e.target.value)}
                      required
                      style={{ width: "100%", padding: "var(--space-2) var(--space-3)" }}
                    >
                      <option value="">Select a reason</option>
                      <option value="Changed my mind">Changed my mind</option>
                      <option value="Wrong item">Wrong item</option>
                      <option value="Defective or damaged">Defective or damaged</option>
                      <option value="Not as described">Not as described</option>
                      <option value="Duplicate order">Duplicate order</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                  <div style={{ marginBottom: "var(--space-4)" }}>
                    <TextArea
                      id="refund-details"
                      label="Additional details (optional)"
                      placeholder="Describe your situation..."
                      value={refundDetails}
                      onChange={(e) => setRefundDetails(e.target.value)}
                      rows={4}
                    />
                  </div>
                  <Button
                    type="submit"
                    variant="secondary"
                    disabled={refundSubmitting || !refundReason.trim()}
                  >
                    {refundSubmitting ? "Submitting…" : "Request refund"}
                  </Button>
                </form>
              )}
            </Card>
          )}

          {/* Actions */}
          <div style={{ display: "flex", gap: "var(--space-3)" }}>
            <Button variant="primary" onClick={() => navigate("/catalog")}>
              Continue shopping
            </Button>
            <Button variant="secondary" onClick={() => navigate("/orders")}>
              View all orders
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
