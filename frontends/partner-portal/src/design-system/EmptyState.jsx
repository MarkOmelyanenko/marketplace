export default function EmptyState({ title, description, action }) {
  return (
    <div className="ds-empty">
      {title && <div className="ds-empty__title">{title}</div>}
      {description && <div className="ds-empty__desc">{description}</div>}
      {action}
    </div>
  );
}
