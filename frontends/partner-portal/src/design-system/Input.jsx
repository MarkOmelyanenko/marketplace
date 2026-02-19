export default function Input({
  id,
  label,
  error,
  helper,
  className = "",
  ...props
}) {
  return (
    <div className={className}>
      {label && (
        <label htmlFor={id} className="ds-label">
          {label}
        </label>
      )}
      <input
        id={id}
        className="ds-input"
        {...props}
        aria-invalid={!!error}
        aria-describedby={
          error ? `${id}-error` : helper ? `${id}-helper` : undefined
        }
      />
      {error && (
        <div id={`${id}-error`} className="ds-error-msg" role="alert">
          {error}
        </div>
      )}
      {helper && !error && (
        <div id={`${id}-helper`} className="ds-helper">
          {helper}
        </div>
      )}
    </div>
  );
}
