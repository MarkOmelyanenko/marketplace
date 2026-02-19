import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { listCatalogOffers, createOrder, getBuyerId } from "../api";
import { useWallet } from "../WalletContext";
import {
  Button,
  PageHeader,
  Alert,
  Card,
  Skeleton,
  EmptyState,
  ConfirmDialog,
  OnboardingBanner,
  Input,
  Pagination,
  useToast,
} from "@design-system";

export default function Catalog() {
  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [sortBy, setSortBy] = useState(""); // '', 'price_asc', 'price_desc', 'date_desc'
  const [priceMin, setPriceMin] = useState("");
  const [priceMax, setPriceMax] = useState("");
  const [buyTarget, setBuyTarget] = useState(null);
  const [buyQuantity, setBuyQuantity] = useState(1);
  const [buying, setBuying] = useState(false);
  const [page, setPage] = useState(1);
  const navigate = useNavigate();
  const CATALOG_PAGE_SIZE = 12;
  const { addToast } = useToast();
  const { refreshBalance } = useWallet();

  useEffect(() => {
    loadOffers();
  }, []);

  useEffect(() => {
    setPage(1);
  }, [search, sortBy, priceMin, priceMax]);

  const loadOffers = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await listCatalogOffers();
      setOffers(data);
    } catch (err) {
      setError(err.message || "Failed to load offers");
    } finally {
      setLoading(false);
    }
  };

  const handleBuyConfirm = async () => {
    if (!buyTarget) return;
    const qty = Math.max(1, parseInt(buyQuantity, 10) || 1);
    try {
      setBuying(true);
      setError("");
      const order = await createOrder(buyTarget.id, qty);
      refreshBalance();
      if (order.status === "PAID") {
        addToast("Order placed successfully!", "success");
      } else {
        addToast("Order created. Complete payment on the order page.", "info");
      }
      navigate(`/orders/${order.id}`);
    } catch (err) {
      setError(err.message || "Failed to create order");
    } finally {
      setBuying(false);
      setBuyTarget(null);
    }
  };

  const formatPrice = (cents, currency = "USD") => {
    if (cents == null) return "$0.00 USD";
    const sym = currency === "USD" ? "$" : "";
    return `${sym}${(cents / 100).toFixed(2)} ${currency}`;
  };

  const filteredOffers = offers
    .filter((o) => {
      const matchesSearch =
        !search ||
        o.title?.toLowerCase().includes(search.toLowerCase()) ||
        o.description?.toLowerCase().includes(search.toLowerCase());
      if (!matchesSearch) return false;
      const cents = o.priceCents ?? 0;
      const minVal =
        priceMin !== "" && priceMin != null ? parseFloat(priceMin) : NaN;
      const maxVal =
        priceMax !== "" && priceMax != null ? parseFloat(priceMax) : NaN;
      const minCents = Number.isFinite(minVal)
        ? Math.round(minVal * 100)
        : null;
      const maxCents = Number.isFinite(maxVal)
        ? Math.round(maxVal * 100)
        : null;
      if (minCents != null && cents < minCents) return false;
      if (maxCents != null && cents > maxCents) return false;
      return true;
    })
    .slice()
    .sort((a, b) => {
      if (sortBy === "price_asc")
        return (a.priceCents ?? 0) - (b.priceCents ?? 0);
      if (sortBy === "price_desc")
        return (b.priceCents ?? 0) - (a.priceCents ?? 0);
      if (sortBy === "date_desc") {
        const ta = new Date(a.publishedAt ?? a.updatedAt ?? 0).getTime();
        const tb = new Date(b.publishedAt ?? b.updatedAt ?? 0).getTime();
        return tb - ta;
      }
      return 0;
    });

  const buyerId = getBuyerId();
  const totalPages = Math.max(
    1,
    Math.ceil(filteredOffers.length / CATALOG_PAGE_SIZE),
  );
  const paginatedOffers = filteredOffers.slice(
    (page - 1) * CATALOG_PAGE_SIZE,
    page * CATALOG_PAGE_SIZE,
  );

  return (
    <div className="ds-container">
      <OnboardingBanner
        storageKey="buyer-onboarding-dismissed"
        title="Welcome to the Marketplace!"
        steps={[
          "Browse published offers from our partners below.",
          'Click "Buy" to purchase an offer.',
          'Track your orders and payment status in "My Orders".',
        ]}
      />

      <PageHeader
        title="Catalog"
        actions={
          <>
            <span className="ds-helper" style={{ alignSelf: "center" }}>
              Buyer: {buyerId}
            </span>
            <Button variant="success" onClick={() => navigate("/orders")}>
              My Orders
            </Button>
          </>
        }
      />

      {!loading && offers.length > 0 && (
        <div
          className="ds-filter-bar"
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: "var(--space-3)",
            alignItems: "center",
          }}
        >
          <Input
            id="catalog-search"
            placeholder="Search offers…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ maxWidth: 320 }}
          />
          <label
            className="ds-helper"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
            }}
          >
            Sort:
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="ds-input"
              style={{
                width: "auto",
                padding: "var(--space-2) var(--space-3)",
              }}
              aria-label="Sort by"
            >
              <option value="">Default</option>
              <option value="price_asc">Price ↑</option>
              <option value="price_desc">Price ↓</option>
              <option value="date_desc">Date (newest)</option>
            </select>
          </label>
          <label
            className="ds-helper"
            style={{
              display: "flex",
              alignItems: "center",
              gap: "var(--space-2)",
            }}
          >
            Price:
            <Input
              type="number"
              min="0"
              step="0.01"
              placeholder="Min"
              value={priceMin}
              onChange={(e) => setPriceMin(e.target.value)}
              style={{ width: 80 }}
              aria-label="Min price"
            />
            –
            <Input
              type="number"
              min="0"
              step="0.01"
              placeholder="Max"
              value={priceMax}
              onChange={(e) => setPriceMax(e.target.value)}
              style={{ width: 80 }}
              aria-label="Max price"
            />
          </label>
        </div>
      )}

      {error && <Alert variant="error">{error}</Alert>}

      {loading && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "var(--space-5)",
          }}
        >
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <Skeleton
                height={24}
                width="80%"
                style={{ marginBottom: "var(--space-3)" }}
              />
              <Skeleton
                height={16}
                width="100%"
                style={{ marginBottom: "var(--space-2)" }}
              />
              <Skeleton
                height={16}
                width="90%"
                style={{ marginBottom: "var(--space-4)" }}
              />
              <Skeleton height={40} width="100%" />
            </Card>
          ))}
        </div>
      )}

      {!loading && offers.length === 0 && (
        <EmptyState
          title="No published offers"
          description="There are no offers available to buy right now. Check back later!"
          action={
            <Button variant="primary" onClick={loadOffers}>
              Refresh
            </Button>
          }
        />
      )}

      {!loading && offers.length > 0 && filteredOffers.length === 0 && (
        <EmptyState
          title="No matching offers"
          description="Try other words."
          action={
            <Button
              variant="secondary"
              onClick={() => {
                setSearch("");
                setPriceMin("");
                setPriceMax("");
              }}
            >
              Clear filters
            </Button>
          }
        />
      )}

      {!loading && filteredOffers.length > 0 && (
        <>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
              gap: "var(--space-5)",
            }}
          >
            {paginatedOffers.map((offer) => (
              <Card
                key={offer.id}
                style={{
                  display: "flex",
                  flexDirection: "column",
                  height: "100%",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    flex: 1,
                    minHeight: 0,
                  }}
                >
                  <h2
                    className="ds-page-title"
                    style={{
                      fontSize: "var(--text-h2)",
                      marginBottom: "var(--space-2)",
                    }}
                  >
                    {offer.title}
                  </h2>
                  <p
                    style={{
                      color: "var(--color-text-muted)",
                      marginBottom: "var(--space-3)",
                      minHeight: 80,
                      display: "-webkit-box",
                      WebkitLineClamp: 4,
                      WebkitBoxOrient: "vertical",
                      overflow: "hidden",
                    }}
                  >
                    {offer.description ||
                      "No description provided. Contact the seller for details."}
                  </p>
                  {offer.aiTags && offer.aiTags.length > 0 && (
                    <div
                      style={{
                        marginBottom: "var(--space-3)",
                        display: "flex",
                        flexWrap: "wrap",
                        gap: "var(--space-2)",
                      }}
                    >
                      {offer.aiTags.map((tag, idx) => (
                        <span key={idx} className="ds-pill ds-pill--neutral">
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      borderTop: "1px solid var(--color-border)",
                      paddingTop: "var(--space-3)",
                      marginTop: "auto",
                    }}
                  >
                    <strong
                      style={{
                        fontSize: "var(--text-h2)",
                        color: "var(--color-success)",
                      }}
                    >
                      {formatPrice(offer.priceCents, offer.currency)}
                    </strong>
                    <Button
                      variant="primary"
                      onClick={() => {
                        setBuyTarget(offer);
                        setBuyQuantity(1);
                      }}
                      disabled={buying}
                    >
                      Buy
                    </Button>
                  </div>
                </div>
              </Card>
            ))}
          </div>
          {totalPages > 1 && (
            <div style={{ marginTop: "var(--space-4)" }}>
              <Pagination
                page={page}
                totalPages={totalPages}
                onPageChange={(p) => {
                  setPage(p);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              />
            </div>
          )}
        </>
      )}

      <ConfirmDialog
        open={!!buyTarget}
        title="Confirm purchase"
        message={
          buyTarget ? (
            <>
              <p style={{ marginBottom: "var(--space-3)" }}>
                You are about to buy &quot;{buyTarget.title}&quot;.
              </p>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "var(--space-3)",
                  flexWrap: "wrap",
                }}
              >
                <label className="ds-label" style={{ marginBottom: 0 }}>
                  Quantity:
                </label>
                <input
                  type="number"
                  min={1}
                  value={buyQuantity}
                  onChange={(e) => setBuyQuantity(e.target.value)}
                  className="ds-input"
                  style={{ width: 80 }}
                  aria-label="Quantity"
                />
              </div>
              <p style={{ marginTop: "var(--space-3)", marginBottom: 0 }}>
                Total:{" "}
                {formatPrice(
                  (buyTarget.priceCents || 0) *
                    Math.max(1, parseInt(buyQuantity, 10) || 1),
                  buyTarget.currency,
                )}
                . Payment will be processed automatically.
              </p>
            </>
          ) : null
        }
        confirmLabel={
          buyTarget
            ? `Buy for ${formatPrice((buyTarget.priceCents || 0) * Math.max(1, parseInt(buyQuantity, 10) || 1), buyTarget.currency)}`
            : ""
        }
        cancelLabel="Cancel"
        variant="primary"
        onConfirm={handleBuyConfirm}
        onCancel={() => {
          setBuyTarget(null);
          setBuyQuantity(1);
          setBuying(false);
        }}
        loading={buying}
      />
    </div>
  );
}
