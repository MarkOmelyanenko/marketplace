const ORDER_STATUS_VARIANT = {
  PENDING_PAYMENT: "warning",
  PAID: "success",
  CANCELLED: "error",
};
const ALL = { ...ORDER_STATUS_VARIANT };
function getVariant(status) {
  if (!status) return "neutral";
  return ALL[String(status).toUpperCase()] || "neutral";
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
