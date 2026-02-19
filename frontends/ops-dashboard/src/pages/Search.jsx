import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { opsSearchPayments } from "../api";
import {
  Button,
  Input,
  PageHeader,
  Alert,
  StatusPill,
  Table,
  TableSkeleton,
  EmptyState,
  Card,
  CopyableId,
  OnboardingBanner,
} from "@design-system";

export default function Search() {
  const [paymentId, setPaymentId] = useState("");
  const [offerId, setOfferId] = useState("");
  const [partnerId, setPartnerId] = useState("");
  const [createdFrom, setCreatedFrom] = useState("");
  const [createdTo, setCreatedTo] = useState("");
  const [status, setStatus] = useState("");
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [hasSearched, setHasSearched] = useState(true);
  const navigate = useNavigate();

  const runSearch = async (params = {}) => {
    setLoading(true);
    setError("");
    try {
      const data = await opsSearchPayments(params);
      setResults(data || []);
      setHasSearched(true);
    } catch (err) {
      if (err.message === "UNAUTHORIZED") navigate("/login");
      else setError(err.message || "Failed to load payments");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    runSearch({});
  }, []);

  const handleSearch = async (e) => {
    e.preventDefault();
    const params = {};
    if (paymentId) params.paymentId = paymentId;
    if (offerId) params.offerId = offerId;
    if (partnerId) params.partnerId = partnerId;
    if (createdFrom)
      params.createdFrom =
        createdFrom.length === 10
          ? `${createdFrom}T00:00:00.000Z`
          : createdFrom;
    if (createdTo)
      params.createdTo =
        createdTo.length === 10 ? `${createdTo}T23:59:59.999Z` : createdTo;
    if (status) params.status = status;
    await runSearch(params);
  };

  const handleClear = () => {
    setPaymentId("");
    setOfferId("");
    setPartnerId("");
    setCreatedFrom("");
    setCreatedTo("");
    setStatus("");
    setResults([]);
    setError("");
    setHasSearched(false);
  };

  const formatDate = (dateString) =>
    dateString ? new Date(dateString).toLocaleString() : "";
  const formatAmount = (cents, currency) =>
    cents != null ? `$${(cents / 100).toFixed(2)} ${currency || "USD"}` : "";

  const columns = [
    {
      key: "id",
      label: "Payment ID",
      render: (_, row) => <CopyableId id={row.id} />,
    },
    { key: "partnerId", label: "Partner ID" },
    {
      key: "offerId",
      label: "Offer ID",
      render: (_, row) => <CopyableId id={row.offerId} />,
    },
    {
      key: "status",
      label: "Status",
      render: (_, row) => <StatusPill status={row.status} />,
    },
    {
      key: "amountCents",
      label: "Amount",
      render: (_, row) => formatAmount(row.amountCents, row.currency),
      cellStyle: () => ({ textAlign: "right" }),
    },
    { key: "createdAt", label: "Created", render: (v) => formatDate(v) },
    {
      key: "action",
      label: "",
      sortable: false,
      render: (_, row) => (
        <Button
          size="small"
          variant="success"
          onClick={() => navigate(`/payments/${row.id}`)}
        >
          Open
        </Button>
      ),
    },
  ];

  const rows = results.map((r) => ({ ...r, key: r.id }));

  return (
    <div className="ds-container">
      <OnboardingBanner
        storageKey="ops-onboarding-dismissed"
        title="Ops Dashboard — Quick Start"
        steps={[
          "All payments are listed below (most recent first). Use the filters to search by Payment ID, Offer ID, or Partner ID.",
          'Click "Open" to view payment details, status timeline, and metadata.',
          'Use "Retry latest webhook" to re-process failed payment callbacks.',
        ]}
      />

      <PageHeader title="Search payments" />

      <Card style={{ marginBottom: "var(--space-5)" }}>
        <form onSubmit={handleSearch}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "var(--space-4)",
              alignItems: "end",
            }}
          >
            <Input
              id="paymentId"
              label="Payment ID"
              value={paymentId}
              onChange={(e) => setPaymentId(e.target.value)}
              placeholder="UUID"
            />
            <Input
              id="offerId"
              label="Offer ID"
              value={offerId}
              onChange={(e) => setOfferId(e.target.value)}
              placeholder="UUID"
            />
            <Input
              id="partnerId"
              label="Partner ID"
              value={partnerId}
              onChange={(e) => setPartnerId(e.target.value)}
              placeholder="String"
            />
            <Input
              id="createdFrom"
              label="Created from"
              type="date"
              value={createdFrom}
              onChange={(e) => setCreatedFrom(e.target.value)}
            />
            <Input
              id="createdTo"
              label="Created to"
              type="date"
              value={createdTo}
              onChange={(e) => setCreatedTo(e.target.value)}
            />
            <label
              className="ds-helper"
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "var(--space-1)",
              }}
            >
              Status
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="ds-input"
                style={{ padding: "var(--space-2) var(--space-3)" }}
                aria-label="Status"
              >
                <option value="">All</option>
                <option value="PENDING">PENDING</option>
                <option value="CAPTURED">CAPTURED</option>
                <option value="FAILED">FAILED</option>
              </select>
            </label>
            <div style={{ display: "flex", gap: "var(--space-2)" }}>
              <Button type="submit" variant="primary" disabled={loading}>
                {loading ? "Searching…" : "Search"}
              </Button>
              {hasSearched && (
                <Button type="button" variant="secondary" onClick={handleClear}>
                  Clear
                </Button>
              )}
            </div>
          </div>
        </form>
      </Card>

      {error && <Alert variant="error">{error}</Alert>}

      {loading && <TableSkeleton rows={5} columns={7} />}

      {!loading && hasSearched && results.length > 0 && (
        <Table
          columns={columns}
          rows={rows}
          numberColumns={["amountCents"]}
          pageSize={10}
        />
      )}

      {!loading && hasSearched && results.length === 0 && !error && (
        <EmptyState
          title="No payments found"
          description="No payments to show. Try different filters or wait for new payments."
        />
      )}
    </div>
  );
}
