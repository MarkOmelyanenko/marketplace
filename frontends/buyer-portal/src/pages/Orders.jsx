import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { listMyOrders, getBuyerId } from "../api";
import {
  Button,
  PageHeader,
  Alert,
  StatusPill,
  Table,
  TableSkeleton,
  EmptyState,
  CopyableId,
} from "@design-system";

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const pollingIntervalRef = useRef(null);

  useEffect(() => {
    loadOrders();
  }, []);

  useEffect(() => {
    const hasPending = orders.some((o) => o.status === "PENDING_PAYMENT");
    if (hasPending && !pollingIntervalRef.current) {
      pollingIntervalRef.current = setInterval(() => loadOrders(), 2000);
      const t = setTimeout(() => {
        if (pollingIntervalRef.current) {
          clearInterval(pollingIntervalRef.current);
          pollingIntervalRef.current = null;
        }
      }, 30000);
      return () => clearTimeout(t);
    }
    if (!hasPending && pollingIntervalRef.current) {
      clearInterval(pollingIntervalRef.current);
      pollingIntervalRef.current = null;
    }
    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
        pollingIntervalRef.current = null;
      }
    };
  }, [orders]);

  const loadOrders = async () => {
    try {
      setError("");
      const data = await listMyOrders();
      setOrders(data);
    } catch (err) {
      setError(err.message || "Failed to load orders");
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (cents, currency = "USD") =>
    cents != null ? `$${(cents / 100).toFixed(2)} ${currency}` : "";

  const buyerId = getBuyerId();

  const columns = [
    {
      key: "id",
      label: "Order ID",
      render: (_, row) => <CopyableId id={row.id} />,
    },
    { key: "offerTitle", label: "Offer" },
    {
      key: "quantity",
      label: "Quantity",
      render: (_, row) => row.quantity ?? 1,
      cellStyle: () => ({ textAlign: "right" }),
    },
    {
      key: "amountCents",
      label: "Amount",
      render: (_, row) => formatPrice(row.amountCents, row.currency),
      cellStyle: () => ({ textAlign: "right" }),
    },
    {
      key: "status",
      label: "Status",
      render: (_, row) => <StatusPill status={row.status} />,
    },
    {
      key: "createdAt",
      label: "Created",
      render: (v) => (v ? new Date(v).toLocaleString() : ""),
    },
    {
      key: "actions",
      label: "",
      sortable: false,
      render: (_, row) => (
        <Button
          size="small"
          variant="primary"
          onClick={() => navigate(`/orders/${row.id}`)}
        >
          Details
        </Button>
      ),
    },
  ];

  const rows = orders.map((o) => ({ ...o, key: o.id }));

  return (
    <div className="ds-container">
      <PageHeader
        title="My Orders"
        actions={
          <>
            <span className="ds-helper" style={{ alignSelf: "center" }}>
              Buyer: {buyerId}
            </span>
            <Button variant="primary" onClick={() => navigate("/catalog")}>
              Browse catalog
            </Button>
          </>
        }
      />

      {error && <Alert variant="error">{error}</Alert>}

      {loading && <TableSkeleton rows={5} columns={7} />}

      {!loading && orders.length === 0 && (
        <EmptyState
          title="No orders yet"
          description="Browse the catalog to make your first purchase."
          action={
            <Button variant="primary" onClick={() => navigate("/catalog")}>
              Browse catalog
            </Button>
          }
        />
      )}

      {!loading && orders.length > 0 && (
        <Table
          columns={columns}
          rows={rows}
          emptyMessage="No orders."
          numberColumns={["amountCents"]}
          pageSize={10}
        />
      )}
    </div>
  );
}
