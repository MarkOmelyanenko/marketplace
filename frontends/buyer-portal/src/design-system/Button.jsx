export default function Button({
  children,
  variant = "primary",
  size = "default",
  type = "button",
  disabled = false,
  className = "",
  ...props
}) {
  const classes = [
    "ds-btn",
    `ds-btn--${variant}`,
    size === "small" && "ds-btn--small",
    className,
  ]
    .filter(Boolean)
    .join(" ");
  return (
    <button type={type} className={classes} disabled={disabled} {...props}>
      {children}
    </button>
  );
}
