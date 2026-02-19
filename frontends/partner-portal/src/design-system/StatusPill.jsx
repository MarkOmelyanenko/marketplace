/**
 * Maps known statuses to semantic variants for consistent styling.
 * Offer: DRAFT, ENRICHING, READY, PUBLISH_PENDING, PUBLISHED, PUBLISH_FAILED
 * Order: PENDING_PAYMENT, PAID, CANCELLED
 * Payment: PENDING, CAPTURED, FAILED, etc.
 */
const OFFER_STATUS_VARIANT = {
  DRAFT: "warning",
  ENRICHING: "info",
  READY: "neutral",
  PUBLISH_PENDING: "warning",
  PUBLISHED: "success",
  PUBLISH_FAILED: "error",
};

const ORDER_STATUS_VARIANT = {
  PENDING_PAYMENT: "warning",
  PAID: "success",
  CANCELLED: "error",
};

const PAYMENT_STATUS_VARIANT = {
  PENDING: "warning",
  CAPTURED: "success",
  FAILED: "error",
};

const ALL_STATUS_VARIANTS = {
  ...OFFER_STATUS_VARIANT,
  ...ORDER_STATUS_VARIANT,
  ...PAYMENT_STATUS_VARIANT,
};

function getVariant(status) {
  if (!status) return "neutral";
  const s = String(status).toUpperCase();
  return ALL_STATUS_VARIANTS[s] || "neutral";
}

export default function StatusPill({
  status,
  variant: variantProp,
  className = "",
}) {
  const variant = variantProp || getVariant(status);
  const classes = ["ds-pill", `ds-pill--${variant}`, className]
    .filter(Boolean)
    .join(" ");
  return <span className={classes}>{status}</span>;
}

export { getVariant };
