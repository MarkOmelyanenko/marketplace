import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { createOffer } from "../api";
import {
  Button,
  Input,
  TextArea,
  PageHeader,
  Alert,
  Card,
  useToast,
} from "@design-system";

function parsePriceDollars(value) {
  const n = parseFloat(String(value).replace(/[^0-9.]/g, ""));
  return Number.isFinite(n) && n >= 0 ? Math.round(n * 100) : null;
}

export default function CreateOffer() {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priceDollars, setPriceDollars] = useState("0.00");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const { addToast } = useToast();

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
    const priceCents = parsePriceDollars(priceDollars);
    if (priceCents == null || priceCents < 1) {
      setError("Please enter a valid price (e.g. 4.99)");
      return;
    }
    try {
      setLoading(true);
      setError("");
      const idempotencyKey = `create-${Date.now()}-${Math.random()}`;
      const offer = await createOffer(
        title.trim(),
        description.trim(),
        priceCents,
        "USD",
        idempotencyKey,
      );
      addToast(
        "Offer created successfully! AI enrichment has started.",
        "success",
      );
      navigate(`/offers/${offer.id}`);
    } catch (err) {
      setError(err.message || "Failed to create offer");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ds-container ds-container--narrow">
      <PageHeader
        title="Create new offer"
        actions={
          <Button variant="secondary" onClick={() => navigate("/offers")}>
            Cancel
          </Button>
        }
      />

      {error && <Alert variant="error">{error}</Alert>}

      <Card>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: "var(--space-5)" }}>
            <Input
              id="title"
              label="Title"
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                setError("");
              }}
              maxLength={140}
              required
              placeholder="Enter offer title"
              helper={`${title.length}/140 characters`}
            />
          </div>
          <div style={{ marginBottom: "var(--space-5)" }}>
            <TextArea
              id="description"
              label="Description"
              value={description}
              onChange={(e) => {
                setDescription(e.target.value);
                setError("");
              }}
              required
              rows={6}
              placeholder="Enter offer description"
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
              value={priceDollars}
              onChange={(e) => {
                setPriceDollars(e.target.value);
                setError("");
              }}
              placeholder="e.g. 4.99"
              helper="Buyers will see this price in the catalog."
            />
          </div>
          <div style={{ display: "flex", gap: "var(--space-2)" }}>
            <Button type="submit" variant="success" disabled={loading}>
              {loading ? "Creating…" : "Create offer"}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate("/offers")}
            >
              Cancel
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
