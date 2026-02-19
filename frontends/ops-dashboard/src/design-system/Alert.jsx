export default function Alert({ variant = "error", children, className = "" }) {
  return (
    <div
      className={`ds-alert ds-alert--${variant} ${className}`.trim()}
      role="alert"
    >
      {children}
    </div>
  );
}
