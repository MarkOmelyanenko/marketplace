import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { listOffers, listMyPayments, deleteOffer } from "../api";
import {
  Button,
  Card,
  Input,
  PageHeader,
  Alert,
  StatusPill,
  Table,
  TableSkeleton,
  EmptyState,
  OnboardingBanner,
  ConfirmDialog,
  CopyableId,
  useToast,
} from "@design-system";

const STATUS_OPTIONS = [
  "ALL",
  // "DRAFT",
  "ENRICHING",
  "READY",
  "PUBLISH_PENDING",
  "PUBLISHED",
  "PUBLISH_FAILED",
];

// Platform commission (10%); partner earns (1 - COMMISSION_RATE) of each sale
const COMMISSION_RATE = 0.1;

function isSale(p) {
  return p.status === "CAPTURED" && p.referenceType === "ORDER_PURCHASE";
}

function earnedAfterCommissionCents(payments) {
  return (
    payments.filter(isSale).reduce((sum, p) => sum + (p.amountCents ?? 0), 0) *
    (1 - COMMISSION_RATE)
  );
}

function salesInPeriod(payments, fromDate) {
  const from = fromDate.getTime();
  return payments.filter(
    (p) => isSale(p) && new Date(p.createdAt).getTime() >= from,
  );
}

export default function OffersList() {
  const [offers, setOffers] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const navigate = useNavigate();
  const { addToast } = useToast();

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        setLoading(true);
        setError("");
        const [offersData, paymentsData] = await Promise.all([
          listOffers(),
          listMyPayments().catch(() => []),
        ]);
        if (!cancelled) {
          setOffers(offersData);
          setPayments(Array.isArray(paymentsData) ? paymentsData : []);
        }
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load offers");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const loadOffers = async () => {
    try {
      setLoading(true);
      setError("");
      const [offersData, paymentsData] = await Promise.all([
        listOffers(),
        listMyPayments().catch(() => []),
      ]);
      setOffers(offersData);
      setPayments(Array.isArray(paymentsData) ? paymentsData : []);
    } catch (err) {
      setError(err.message || "Failed to load offers");
    } finally {
      setLoading(false);
    }
  };

  const now = new Date();
  const startOfToday = new Date(
    now.getFullYear(),
    now.getMonth(),
    now.getDate(),
  );
  const startOf7d = new Date(startOfToday);
  startOf7d.setDate(startOf7d.getDate() - 7);
  const startOf30d = new Date(startOfToday);
  startOf30d.setDate(startOf30d.getDate() - 30);
  const salesToday = salesInPeriod(payments, startOfToday);
  const salesLast7d = salesInPeriod(payments, startOf7d);
  const salesLast30d = salesInPeriod(payments, startOf30d);
  const earnedTodayCents = earnedAfterCommissionCents(salesToday);
  const earned7dCents = earnedAfterCommissionCents(salesLast7d);
  const earned30dCents = earnedAfterCommissionCents(salesLast30d);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      setDeleting(true);
      await deleteOffer(deleteTarget.id);
      setOffers((prev) => prev.filter((o) => o.id !== deleteTarget.id));
      addToast("Offer deleted successfully", "success");
    } catch (err) {
      addToast(err.message || "Failed to delete offer", "error");
    } finally {
      setDeleting(false);
      setDeleteTarget(null);
    }
  };

  const filteredOffers = offers.filter((o) => {
    const matchesSearch =
      !search ||
      o.title?.toLowerCase().includes(search.toLowerCase()) ||
      o.id?.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = statusFilter === "ALL" || o.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const formatDate = (dateString) => new Date(dateString).toLocaleString();
  const formatPrice = (cents, currency = "USD") =>
    cents != null ? `$${(cents / 100).toFixed(2)} ${currency}` : "—";

  const columns = [
    { key: "id", label: "ID", render: (_, row) => <CopyableId id={row.id} /> },
    { key: "title", label: "Title" },
    {
      key: "priceCents",
      label: "Price",
      render: (_, row) => formatPrice(row.priceCents, row.currency),
      cellStyle: () => ({ textAlign: "right" }),
    },
    {
      key: "status",
      label: "Status",
      render: (_, row) => <StatusPill status={row.status} />,
    },
    { key: "createdAt", label: "Created", render: (v) => formatDate(v) },
    {
      key: "actions",
      label: "",
      sortable: false,
      render: (_, row) => (
        <div style={{ display: "flex", gap: "var(--space-2)" }}>
          <Button
            size="small"
            variant="primary"
            onClick={() => navigate(`/offers/${row.id}`)}
          >
            View
          </Button>
          <Button
            size="small"
            variant="danger"
            onClick={() => setDeleteTarget(row)}
          >
            Delete
          </Button>
        </div>
      ),
    },
  ];

  const rows = filteredOffers.map((o) => ({ ...o, key: o.id }));

  return (
    <div className="ds-container">
      <OnboardingBanner
        storageKey="partner-onboarding-dismissed"
        title="Welcome to Partner Portal!"
        steps={[
          "Create your first offer with a title, description, and price (in USD).",
          "AI will suggest improved titles, descriptions, and tags — each offer gets different suggestions.",
          "Review AI suggestions, apply what you like, then publish.",
          "Publishing charges a $1.99 USD listing fee and makes your offer visible to buyers.",
        ]}
      />

      <PageHeader
        title="My Offers"
        actions={
          <Button variant="success" onClick={() => navigate("/offers/new")}>
            + Create offer
          </Button>
        }
      />

      {!loading && offers.length > 0 && (
        <Card style={{ marginBottom: "var(--space-4)" }}>
          <h2
            className="ds-page-title"
            style={{
              fontSize: "var(--text-h2)",
              marginBottom: "var(--space-3)",
            }}
          >
            Statistics
          </h2>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(160px, 1fr))",
              gap: "var(--space-4)",
            }}
          >
            <div>
              <span className="ds-helper">Earned today (after commission)</span>
              <div style={{ fontSize: "var(--text-h2)", fontWeight: 600 }}>
                {formatPrice(Math.round(earnedTodayCents))}
              </div>
            </div>
            <div>
              <span className="ds-helper">
                Earned last 7 days (after commission)
              </span>
              <div style={{ fontSize: "var(--text-h2)", fontWeight: 600 }}>
                {formatPrice(Math.round(earned7dCents))}
              </div>
            </div>
            <div>
              <span className="ds-helper">
                Earned last 30 days (after commission)
              </span>
              <div style={{ fontSize: "var(--text-h2)", fontWeight: 600 }}>
                {formatPrice(Math.round(earned30dCents))}
              </div>
            </div>
          </div>
        </Card>
      )}

      {!loading && offers.length > 0 && (
        <div className="ds-filter-bar">
          <Input
            id="search"
            placeholder="Search by title or ID…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ maxWidth: 280 }}
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="ds-input"
            style={{ maxWidth: 180 }}
            aria-label="Filter by status"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s === "ALL" ? "All statuses" : s.replace(/_/g, " ")}
              </option>
            ))}
          </select>
        </div>
      )}

      {error && (
        <Alert variant="error">
          {error}
          <Button
            size="small"
            variant="secondary"
            onClick={loadOffers}
            style={{ marginTop: "var(--space-2)" }}
          >
            Retry
          </Button>
        </Alert>
      )}

      {loading && <TableSkeleton rows={6} columns={5} />}

      {!loading && !error && offers.length === 0 && (
        <EmptyState
          title="No offers yet"
          description="Create your first offer to get started."
          action={
            <Button variant="success" onClick={() => navigate("/offers/new")}>
              + Create offer
            </Button>
          }
        />
      )}

      {!loading &&
        !error &&
        offers.length > 0 &&
        filteredOffers.length === 0 && (
          <EmptyState
            title="No matching offers"
            description="Try adjusting your search or filter criteria."
            action={
              <Button
                variant="secondary"
                onClick={() => {
                  setSearch("");
                  setStatusFilter("ALL");
                }}
              >
                Clear filters
              </Button>
            }
          />
        )}

      {!loading && !error && filteredOffers.length > 0 && (
        <Table
          columns={columns}
          rows={rows}
          emptyMessage="No offers."
          pageSize={10}
        />
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="Delete offer?"
        message={`Are you sure you want to delete "${deleteTarget?.title}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  );
}
