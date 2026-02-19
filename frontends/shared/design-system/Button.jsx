import { forwardRef } from 'react';

const Button = forwardRef(function Button(
  {
    children,
    variant = 'primary',
    size = 'default',
    type = 'button',
    disabled = false,
    className = '',
    ...props
  },
  ref,
) {
  const classes = [
    'ds-btn',
    `ds-btn--${variant}`,
    size === 'small' && 'ds-btn--small',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <button ref={ref} type={type} className={classes} disabled={disabled} {...props}>
      {children}
    </button>
  );
});

export default Button;
