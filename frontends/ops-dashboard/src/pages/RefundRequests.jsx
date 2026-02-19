import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  opsListRefundRequests,
  opsApproveRefundRequest,
  opsRejectRefundRequest,
} from "../api";
import {
  Button,
  Card,
  Alert,
  StatusPill,
  Table,
  TableSkeleton,
  EmptyState,
  CopyableId,
  ConfirmDialog,
  TextArea,
  useToast,
} from "@design-system";

export default function RefundRequests() {
  const navigate = useNavigate();
  const { addToast } = useToast();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [detailsRequest, setDetailsRequest] = useState(null);
  const [approveConfirm, setApproveConfirm] = useState(null);
  const [rejectState, setRejectState] = useState(null);
  const [rejectReason, setRejectReason] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const loadList = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await opsListRefundRequests(statusFilter || undefined);
      setList(Array.isArray(data) ? data : []);
    } catch (err) {
      if (err.message === "UNAUTHORIZED") navigate("/login");
      else setError(err.message || "Failed to load refund requests");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadList();
  }, [statusFilter]);

  const handleApprove = async () => {
    if (!approveConfirm) return;
    setActionLoading(true);
    try {
      await opsApproveRefundRequest(approveConfirm.id);
      addToast("Refund request approved.", "success");
      setApproveConfirm(null);
      setDetailsRequest(null);
      loadList();
    } catch (err) {
      if (err.message === "UNAUTHORIZED") navigate("/login");
      else addToast(err.message || "Failed to approve", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const handleRejectSubmit = async () => {
    if (!rejectState) return;
    setActionLoading(true);
    try {
      await opsRejectRefundRequest(rejectState.id, rejectReason.trim());
      addToast("Refund request rejected.", "success");
      setRejectState(null);
      setRejectReason("");
      setDetailsRequest(null);
      loadList();
    } catch (err) {
      if (err.message === "UNAUTHORIZED") navigate("/login");
      else addToast(err.message || "Failed to reject", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const formatDate = (dateString) =>
    dateString ? new Date(dateString).toLocaleString() : "";
  const formatAmount = (cents) =>
    cents != null ? `$${(cents / 100).toFixed(2)}` : "—";

  const columns = [
    {
      key: "id",
      label: "Request ID",
      render: (_, row) => <CopyableId id={row.id} />,
    },
    {
      key: "orderId",
      label: "Order ID",
      render: (_, row) => <CopyableId id={row.orderId} />,
    },
    {
      key: "buyerId",
      label: "Buyer ID",
      render: (_, row) => <code>{row.buyerId}</code>,
    },
    { key: "reason", label: "Reason" },
    {
      key: "orderAmountCents",
      label: "Order amount",
      render: (_, row) => formatAmount(row.orderAmountCents),
      cellStyle: () => ({ textAlign: "right" }),
    },
    {
      key: "status",
      label: "Status",
      render: (_, row) => <StatusPill status={row.status} />,
    },
    { key: "createdAt", label: "Created", render: (v) => formatDate(v) },
    {
      key: "action",
      label: "",
      sortable: false,
      render: (_, row) => (
        <Button
          size="small"
          variant="secondary"
          onClick={() => setDetailsRequest(row)}
        >
          Details
        </Button>
      ),
    },
  ];

  return (
    <div className="ds-container">
      <div style={{ marginBottom: "var(--space-4)" }}>
        <h1 className="ds-page-title">Refund requests</h1>
        <p className="ds-helper" style={{ marginTop: "var(--space-1)" }}>
          Review and approve or reject buyer refund requests for paid orders.
        </p>
      </div>

      {error && (
        <Alert variant="danger" style={{ marginBottom: "var(--space-4)" }}>
          {error}
        </Alert>
      )}

      <Card style={{ marginBottom: "var(--space-4)" }}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--space-3)",
            flexWrap: "wrap",
          }}
        >
          <span className="ds-helper">Status:</span>
          <select
            className="ds-input"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ width: "auto", padding: "var(--space-2) var(--space-3)" }}
          >
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
          <Button variant="secondary" size="small" onClick={loadList}>
            Refresh
          </Button>
        </div>
      </Card>

      <Card>
        {loading ? (
          <TableSkeleton rows={5} columns={columns.length} />
        ) : list.length === 0 ? (
          <EmptyState
            title="No refund requests"
            description={
              statusFilter
                ? `No ${statusFilter.toLowerCase()} refund requests.`
                : "No refund requests found."
            }
          />
        ) : (
          <Table
            columns={columns}
            rows={list.map((r) => ({ ...r, key: r.id }))}
            emptyMessage="No refund requests."
            pageSize={10}
          />
        )}
      </Card>

      {/* Details modal */}
      {detailsRequest && (
        <div
          className="ds-overlay"
          onClick={() => !actionLoading && setDetailsRequest(null)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="details-dialog-title"
        >
          <div
            className="ds-dialog ds-dialog--wide"
            onClick={(e) => e.stopPropagation()}
            style={{ maxWidth: "480px" }}
          >
            <h2 id="details-dialog-title" className="ds-dialog__title">
              Refund request details
            </h2>
            <div
              className="ds-dialog__body"
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-3)",
              }}
            >
              <div>
                <span className="ds-helper">Request ID</span>
                <div>
                  <CopyableId id={detailsRequest.id} />
                </div>
              </div>
              <div>
                <span className="ds-helper">Order ID</span>
                <div>
                  <CopyableId id={detailsRequest.orderId} />
                </div>
              </div>
              <div>
                <span className="ds-helper">Buyer ID</span>
                <div>
                  <code>{detailsRequest.buyerId}</code>
                </div>
              </div>
              <div>
                <span className="ds-helper">Reason</span>
                <div>
                  <strong>{detailsRequest.reason}</strong>
                </div>
              </div>
              <div>
                <span className="ds-helper">Details</span>
                <div
                  style={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}
                >
                  {detailsRequest.details || "—"}
                </div>
              </div>
              <div>
                <span className="ds-helper">Status</span>
                <div>
                  <StatusPill status={detailsRequest.status} />
                </div>
              </div>
              <div>
                <span className="ds-helper">Created</span>
                <div>{formatDate(detailsRequest.createdAt)}</div>
              </div>
              {detailsRequest.reviewedAt && (
                <>
                  <div>
                    <span className="ds-helper">Reviewed at</span>
                    <div>{formatDate(detailsRequest.reviewedAt)}</div>
                  </div>
                  {detailsRequest.reviewedBy && (
                    <div>
                      <span className="ds-helper">Reviewed by</span>
                      <div>
                        <code>{detailsRequest.reviewedBy}</code>
                      </div>
                    </div>
                  )}
                  {detailsRequest.rejectionReason != null &&
                    detailsRequest.rejectionReason !== "" && (
                      <div>
                        <span className="ds-helper">Rejection reason</span>
                        <div style={{ whiteSpace: "pre-wrap" }}>
                          {detailsRequest.rejectionReason}
                        </div>
                      </div>
                    )}
                </>
              )}
            </div>
            <div
              className="ds-dialog__actions"
              style={{ flexWrap: "wrap", gap: "var(--space-2)" }}
            >
              <Button
                variant="secondary"
                onClick={() => setDetailsRequest(null)}
                disabled={actionLoading}
              >
                Close
              </Button>
              {detailsRequest.status === "PENDING" && (
                <>
                  <Button
                    variant="success"
                    onClick={() => {
                      setApproveConfirm(detailsRequest);
                      setDetailsRequest(null);
                    }}
                    disabled={actionLoading}
                  >
                    Approve
                  </Button>
                  <Button
                    variant="danger"
                    onClick={() => {
                      setRejectState(detailsRequest);
                      setDetailsRequest(null);
                    }}
                    disabled={actionLoading}
                  >
                    Reject
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!approveConfirm}
        title="Approve refund request?"
        message={
          approveConfirm
            ? "After confirmation, the money will be refunded to the buyer. Continue?"
            : ""
        }
        confirmLabel="Approve"
        variant="success"
        onConfirm={handleApprove}
        onCancel={() => setApproveConfirm(null)}
        loading={actionLoading}
      />

      {rejectState && (
        <div
          className="ds-overlay"
          onClick={() => !actionLoading && setRejectState(null)}
          role="dialog"
          aria-modal="true"
          aria-labelledby="reject-dialog-title"
        >
          <div className="ds-dialog" onClick={(e) => e.stopPropagation()}>
            <h2 id="reject-dialog-title" className="ds-dialog__title">
              Reject refund request?
            </h2>
            <div className="ds-dialog__body">
              <p style={{ marginBottom: "var(--space-3)" }}>
                Order: <CopyableId id={rejectState.orderId} /> — Reason:{" "}
                {rejectState.reason}
              </p>
              <TextArea
                id="reject-reason"
                label="Rejection reason (optional, shown to buyer)"
                placeholder="e.g. Refund period expired"
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={3}
              />
            </div>
            <div className="ds-dialog__actions">
              <Button
                variant="secondary"
                onClick={() => setRejectState(null)}
                disabled={actionLoading}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleRejectSubmit}
                disabled={actionLoading}
              >
                {actionLoading ? "Processing…" : "Reject"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
