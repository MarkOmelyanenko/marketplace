export default function PageHeader({ title, actions }) {
  return (
    <div className="ds-page-header">
      <h1 className="ds-page-title">{title}</h1>
      {actions && (
        <div
          style={{ display: "flex", gap: "var(--space-2)", flexWrap: "wrap" }}
        >
          {actions}
        </div>
      )}
    </div>
  );
}
