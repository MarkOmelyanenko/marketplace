import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getOffer,
  updateOffer,
  applyAi,
  publishOffer,
  deleteOffer,
} from "../api";
import { useWallet } from "../WalletContext";
import {
  Button,
  Input,
  TextArea,
  Card,
  Alert,
  StatusPill,
  Skeleton,
  ConfirmDialog,
  CopyableId,
  useToast,
} from "@design-system";

const EDITABLE_STATUSES = ["DRAFT", "READY", "PUBLISH_FAILED", "PUBLISHED"];

export default function OfferDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useToast();
  const { balanceCents, refreshBalance } = useWallet();
  const LISTING_FEE_CENTS = 199; // $1.99
  const canAffordPublish =
    balanceCents != null && balanceCents >= LISTING_FEE_CENTS;
  const topUpAmount =
    balanceCents != null && balanceCents < LISTING_FEE_CENTS
      ? ((LISTING_FEE_CENTS - balanceCents) / 100).toFixed(2)
      : "0.00";

  const [offer, setOffer] = useState(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priceCents, setPriceCents] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [applyingAi, setApplyingAi] = useState(false);
  const [error, setError] = useState("");
  const [enrichmentTimedOut, setEnrichmentTimedOut] = useState(false);
  const [isDirty, setIsDirty] = useState(false);

  const [showPublishConfirm, setShowPublishConfirm] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const pollIntervalRef = useRef(null);
  const pollStartTimeRef = useRef(null);

  useEffect(() => {
    loadOffer();
    return () => stopPolling();
  }, [id]);

  // Unsaved changes warning
  useEffect(() => {
    if (!isDirty) return;
    const handler = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [isDirty]);

  const loadOffer = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await getOffer(id);
      setOffer(data);
      setTitle(data.title);
      setDescription(data.description);
      setPriceCents(data.priceCents != null ? data.priceCents : 499);
      setIsDirty(false);
      setEnrichmentTimedOut(false);
      if (data.status === "ENRICHING" || data.status === "PUBLISH_PENDING")
        startPolling();
      else stopPolling();
    } catch (err) {
      setError(err.message || "Failed to load offer");
    } finally {
      setLoading(false);
    }
  };

  const startPolling = () => {
    stopPolling();
    pollStartTimeRef.current = Date.now();
    const maxPollDuration = 30000;
    pollIntervalRef.current = setInterval(async () => {
      if (Date.now() - pollStartTimeRef.current >= maxPollDuration) {
        stopPolling();
        setEnrichmentTimedOut(true);
        return;
      }
      try {
        const data = await getOffer(id);
        setOffer(data);
        if (!isDirty) {
          setTitle(data.title);
          setDescription(data.description);
          setPriceCents(data.priceCents != null ? data.priceCents : 499);
        }
        if (data.status !== "ENRICHING" && data.status !== "PUBLISH_PENDING") {
          stopPolling();
          if (data.status === "PUBLISHED") {
            refreshBalance();
            addToast("Offer published successfully!", "success");
          } else if (data.status === "READY") {
            addToast(
              "AI enrichment complete — review the suggestions below.",
              "info",
            );
          } else if (data.status === "PUBLISH_FAILED") {
            addToast("Publishing failed. Please try again.", "error");
          }
        }
      } catch (err) {
        console.error("Polling error:", err);
      }
    }, 2000);
  };

  const stopPolling = () => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  };

  const isEditable = offer && EDITABLE_STATUSES.includes(offer.status);

  const handleTitleChange = (e) => {
    setTitle(e.target.value);
    setError("");
    setIsDirty(true);
  };

  const handleDescriptionChange = (e) => {
    setDescription(e.target.value);
    setError("");
    setIsDirty(true);
  };

  const handlePriceChange = (e) => {
    const dollars = parseFloat(e.target.value);
    if (e.target.value === "" || (!Number.isNaN(dollars) && dollars >= 0)) {
      setPriceCents(e.target.value === "" ? 0 : Math.round(dollars * 100));
      setError("");
      setIsDirty(true);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim() || !description.trim()) {
      setError("Title and description are required");
      return;
    }
    if (title.length > 140) {
      setError("Title must not exceed 140 characters");
      return;
    }
    if (priceCents == null || priceCents < 1) {
      setError("Please enter a valid price (at least $0.01)");
      return;
    }
    try {
      setSaving(true);
      setError("");
      const updated = await updateOffer(
        id,
        title.trim(),
        description.trim(),
        priceCents,
        offer.currency || "USD",
      );
      setOffer(updated);
      setTitle(updated.title);
      setDescription(updated.description);
      setPriceCents(updated.priceCents != null ? updated.priceCents : 499);
      setIsDirty(false);
      addToast("Changes saved successfully.", "success");
    } catch (err) {
      setError(err.message || "Failed to update offer");
    } finally {
      setSaving(false);
    }
  };

  const handleApplyAi = async (useTitle, useDescription) => {
    try {
      setApplyingAi(true);
      setError("");
      const updated = await applyAi(id, useTitle, useDescription);
      setOffer(updated);
      setTitle(updated.title);
      setDescription(updated.description);
      setIsDirty(false);
      addToast(
        useTitle ? "AI title applied." : "AI description applied.",
        "success",
      );
    } catch (err) {
      setError(err.message || "Failed to apply AI suggestion");
    } finally {
      setApplyingAi(false);
    }
  };

  const handlePublish = async () => {
    try {
      setPublishing(true);
      setError("");
      const updated = await publishOffer(id);
      setOffer(updated);
      setTitle(updated.title);
      setDescription(updated.description);
      setShowPublishConfirm(false);
      startPolling();
      refreshBalance();
      addToast("Publish started — processing payment…", "info");
    } catch (err) {
      setError(err.message || "Failed to publish offer");
      setShowPublishConfirm(false);
    } finally {
      setPublishing(false);
    }
  };

  const handleDelete = async () => {
    try {
      setDeleting(true);
      await deleteOffer(id);
      addToast("Offer deleted.", "success");
      navigate("/offers");
    } catch (err) {
      addToast(err.message || "Failed to delete offer", "error");
      setShowDeleteConfirm(false);
    } finally {
      setDeleting(false);
    }
  };

  const handleBack = useCallback(() => {
    if (isDirty && !window.confirm("You have unsaved changes. Leave anyway?"))
      return;
    navigate("/offers");
  }, [isDirty, navigate]);

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

  if (error && !offer) {
    return (
      <div className="ds-container ds-container--narrow">
        <Alert variant="error">{error}</Alert>
        <Button variant="primary" onClick={() => navigate("/offers")}>
          Back to offers
        </Button>
      </div>
    );
  }

  return (
    <div className="ds-container ds-container--narrow">
      {/* Header */}
      <div style={{ marginBottom: "var(--space-5)" }}>
        <Button
          variant="secondary"
          size="small"
          onClick={handleBack}
          style={{ marginBottom: "var(--space-4)" }}
        >
          ← Back to offers
        </Button>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            flexWrap: "wrap",
            gap: "var(--space-3)",
          }}
        >
          <h1 className="ds-page-title">Offer details</h1>
          {offer && (
            <Button
              size="small"
              variant="danger"
              onClick={() => setShowDeleteConfirm(true)}
            >
              Delete offer
            </Button>
          )}
        </div>
      </div>

      {error && <Alert variant="error">{error}</Alert>}

      {/* Status card */}
      {offer && (
        <Card style={{ marginBottom: "var(--space-5)" }}>
          <div style={{ marginBottom: "var(--space-3)" }}>
            <strong>ID:</strong> <CopyableId id={offer.id} />
          </div>
          <div style={{ marginBottom: "var(--space-3)" }}>
            <strong>Status:</strong> <StatusPill status={offer.status} />
            {offer.status === "ENRICHING" && (
              <div style={{ marginTop: "var(--space-2)" }}>
                {!enrichmentTimedOut ? (
                  <div className="ds-helper">
                    AI is generating suggestions — this usually takes a few
                    seconds…
                  </div>
                ) : (
                  <div>
                    <div
                      className="ds-helper"
                      style={{ marginBottom: "var(--space-2)" }}
                    >
                      Enrichment is taking longer than usual. The backend may
                      still be processing.
                    </div>
                    <Button
                      size="small"
                      variant="secondary"
                      onClick={loadOffer}
                    >
                      Refresh status
                    </Button>
                  </div>
                )}
              </div>
            )}
            {offer.status === "PUBLISH_PENDING" && (
              <div
                className="ds-helper"
                style={{ marginTop: "var(--space-2)" }}
              >
                Payment is being processed. This usually completes within a few
                seconds.
              </div>
            )}
            {offer.status === "PUBLISH_FAILED" && (
              <div style={{ marginTop: "var(--space-2)" }}>
                <Alert variant="error">
                  Publishing failed — the listing fee could not be charged (e.g.
                  insufficient balance). Top up your balance via Deposit in the
                  header, then try publishing again.
                </Alert>
              </div>
            )}
          </div>
          <div style={{ marginBottom: "var(--space-3)" }}>
            <strong>Created:</strong> {formatDate(offer.createdAt)}
          </div>
          <div>
            <strong>Updated:</strong> {formatDate(offer.updatedAt)}
          </div>
        </Card>
      )}

      {/* AI suggestions */}
      {offer && isEditable && offer.aiTitle && (
        <Card
          style={{
            marginBottom: "var(--space-5)",
            borderLeft: "4px solid var(--color-info)",
          }}
        >
          <h2
            className="ds-page-title"
            style={{
              fontSize: "var(--text-h2)",
              marginBottom: "var(--space-4)",
            }}
          >
            AI suggestions
          </h2>
          {offer.aiTitle && (
            <div style={{ marginBottom: "var(--space-5)" }}>
              <div
                className="ds-label"
                style={{ marginBottom: "var(--space-2)" }}
              >
                Suggested title
              </div>
              <div
                style={{
                  padding: "var(--space-3)",
                  marginBottom: "var(--space-2)",
                  background: "var(--color-bg)",
                  borderRadius: "var(--radius-md)",
                }}
              >
                {offer.aiTitle}
              </div>
              <Button
                size="small"
                variant="success"
                onClick={() => handleApplyAi(true, false)}
                disabled={applyingAi}
              >
                {applyingAi ? "Applying…" : "Apply AI title"}
              </Button>
            </div>
          )}
          {offer.aiDescription && (
            <div style={{ marginBottom: "var(--space-5)" }}>
              <div
                className="ds-label"
                style={{ marginBottom: "var(--space-2)" }}
              >
                Suggested description
              </div>
              <div
                style={{
                  padding: "var(--space-3)",
                  marginBottom: "var(--space-2)",
                  whiteSpace: "pre-wrap",
                  background: "var(--color-bg)",
                  borderRadius: "var(--radius-md)",
                }}
              >
                {offer.aiDescription}
              </div>
              <Button
                size="small"
                variant="success"
                onClick={() => handleApplyAi(false, true)}
                disabled={applyingAi}
              >
                {applyingAi ? "Applying…" : "Apply AI description"}
              </Button>
            </div>
          )}
          {offer.aiTags && offer.aiTags.length > 0 && (
            <div>
              <div
                className="ds-label"
                style={{ marginBottom: "var(--space-2)" }}
              >
                Suggested tags
              </div>
              <div
                style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "var(--space-2)",
                }}
              >
                {offer.aiTags.map((tag, index) => (
                  <span key={index} className="ds-pill ds-pill--info">
                    {tag}
                  </span>
                ))}
              </div>
            </div>
          )}
        </Card>
      )}

      {/* Publish card */}
      {offer &&
        (offer.status === "READY" || offer.status === "PUBLISH_FAILED") && (
          <Card
            style={{
              marginBottom: "var(--space-5)",
              borderLeft: "4px solid var(--color-success)",
            }}
          >
            <h2
              className="ds-page-title"
              style={{
                fontSize: "var(--text-h2)",
                marginBottom: "var(--space-2)",
              }}
            >
              {offer.status === "PUBLISH_FAILED"
                ? "Retry publishing"
                : "Publish offer"}
            </h2>
            <p className="ds-helper" style={{ marginBottom: "var(--space-2)" }}>
              {offer.status === "PUBLISH_FAILED"
                ? "The previous attempt failed. Click below to try again. A listing fee of $1.99 USD will be charged."
                : "Publish this offer to make it available to buyers. A listing fee of $1.99 USD will be charged."}
            </p>
            <p className="ds-helper" style={{ marginBottom: "var(--space-4)" }}>
              Listing fee: $1.99. Your balance:{" "}
              {balanceCents != null
                ? `$${(balanceCents / 100).toFixed(2)}`
                : "—"}
            </p>
            {!canAffordPublish && balanceCents != null && (
              <Alert variant="error" style={{ marginBottom: "var(--space-4)" }}>
                You need to top up your wallet by ${topUpAmount}.
              </Alert>
            )}
            <Button
              variant="success"
              onClick={() => setShowPublishConfirm(true)}
              disabled={!canAffordPublish}
            >
              {offer.status === "PUBLISH_FAILED"
                ? "Retry publish"
                : "Publish offer"}
            </Button>
          </Card>
        )}

      {/* Edit form */}
      {offer && (
        <Card>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "var(--space-4)",
            }}
          >
            <h2
              className="ds-page-title"
              style={{ fontSize: "var(--text-h2)", margin: 0 }}
            >
              {isEditable ? "Edit offer" : "Offer content"}
            </h2>
            {isDirty && (
              <span
                className="ds-helper"
                style={{ color: "var(--color-warning)" }}
              >
                Unsaved changes
              </span>
            )}
          </div>
          <form onSubmit={handleSubmit}>
            <div style={{ marginBottom: "var(--space-5)" }}>
              <Input
                id="title"
                label="Title"
                value={title}
                onChange={handleTitleChange}
                maxLength={140}
                required
                disabled={!isEditable}
                helper={`${title.length}/140 characters`}
              />
            </div>
            <div style={{ marginBottom: "var(--space-5)" }}>
              <TextArea
                id="description"
                label="Description"
                value={description}
                onChange={handleDescriptionChange}
                required
                rows={6}
                disabled={!isEditable}
                helper={`${description.length} characters`}
              />
            </div>
            <div style={{ marginBottom: "var(--space-5)" }}>
              <Input
                id="price"
                type="number"
                step="0.01"
                min="0.01"
                label="Price (USD)"
                value={priceCents != null ? (priceCents / 100).toFixed(2) : "0"}
                onChange={handlePriceChange}
                disabled={!isEditable}
                helper="Price buyers will pay for this offer."
              />
            </div>
            {isEditable && (
              <Button
                type="submit"
                variant="primary"
                disabled={saving || !isDirty}
              >
                {saving ? "Saving…" : "Save changes"}
              </Button>
            )}
          </form>
        </Card>
      )}

      {/* Publish confirmation */}
      <ConfirmDialog
        open={showPublishConfirm}
        title="Publish this offer?"
        message="Publishing will charge a listing fee of $1.99 USD and make the offer visible to all buyers. This cannot be undone."
        confirmLabel="Publish & pay $1.99"
        cancelLabel="Cancel"
        variant="success"
        onConfirm={handlePublish}
        onCancel={() => setShowPublishConfirm(false)}
        loading={publishing}
      />

      {/* Delete confirmation */}
      <ConfirmDialog
        open={showDeleteConfirm}
        title="Delete this offer?"
        message={`Are you sure you want to delete "${offer?.title}"? This action cannot be undone.`}
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDelete}
        onCancel={() => setShowDeleteConfirm(false)}
        loading={deleting}
      />
    </div>
  );
}
