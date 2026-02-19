const PAYMENT_STATUS_VARIANT = {
  PENDING: "warning",
  CAPTURED: "success",
  FAILED: "error",
};
function getVariant(status) {
  if (!status) return "neutral";
  return PAYMENT_STATUS_VARIANT[String(status).toUpperCase()] || "neutral";
}
export default function StatusPill({
  status,
  variant: variantProp,
  className = "",
}) {
  const variant = variantProp || getVariant(status);
  return (
    <span className={`ds-pill ds-pill--${variant} ${className}`.trim()}>
      {status}
    </span>
  );
}
