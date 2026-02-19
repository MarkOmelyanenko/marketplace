/**
 * Unified status-to-variant and human-readable label mapping.
 * Covers: Offer, Order, and Payment statuses across all portals.
 */
const STATUS_CONFIG = {
  /* Offer statuses */
  DRAFT:           { variant: 'warning', label: 'Draft' },
  ENRICHING:       { variant: 'info',    label: 'Enriching…' },
  READY:           { variant: 'neutral', label: 'Ready' },
  PUBLISH_PENDING: { variant: 'warning', label: 'Publishing…' },
  PUBLISHED:       { variant: 'success', label: 'Published' },
  PUBLISH_FAILED:  { variant: 'error',   label: 'Publish failed' },

  /* Order statuses */
  PENDING_PAYMENT: { variant: 'warning', label: 'Awaiting payment' },
  PAID:            { variant: 'success', label: 'Paid' },
  CANCELLED:       { variant: 'error',   label: 'Cancelled' },

  /* Payment statuses */
  PENDING:         { variant: 'warning', label: 'Pending' },
  CAPTURED:        { variant: 'success', label: 'Captured' },
  FAILED:          { variant: 'error',   label: 'Failed' },

  /* Refund request statuses */
  APPROVED:        { variant: 'success', label: 'Approved' },
  REJECTED:        { variant: 'error',   label: 'Rejected' },
};

function getConfig(status) {
  if (!status) return { variant: 'neutral', label: status || '' };
  const s = String(status).toUpperCase();
  return STATUS_CONFIG[s] || { variant: 'neutral', label: status };
}

export default function StatusPill({ status, variant: variantProp, label: labelProp, className = '' }) {
  const config = getConfig(status);
  const variant = variantProp || config.variant;
  const displayLabel = labelProp || config.label;

  const classes = ['ds-pill', `ds-pill--${variant}`, className].filter(Boolean).join(' ');
  return (
    <span className={classes} title={status}>
      {displayLabel}
    </span>
  );
}

export { getConfig, STATUS_CONFIG };
