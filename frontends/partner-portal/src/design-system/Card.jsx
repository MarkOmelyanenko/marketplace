export default function Card({ children, className = "", style }) {
  return (
    <div className={`ds-card ${className}`.trim()} style={style}>
      {children}
    </div>
  );
}
