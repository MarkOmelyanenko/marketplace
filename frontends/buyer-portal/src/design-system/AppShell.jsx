import { Link, useLocation } from "react-router-dom";

export default function AppShell({ brandLabel = "Buyer Portal", children }) {
  const location = useLocation();
  return (
    <div className="ds-page">
      <nav className="ds-nav">
        <div className="ds-nav__inner">
          <Link to="/catalog" className="ds-nav__brand">
            {brandLabel}
          </Link>
          <div className="ds-nav__links">
            <Link
              to="/catalog"
              className={`ds-nav__link ${location.pathname === "/catalog" ? "ds-nav__link--active" : ""}`}
            >
              Catalog
            </Link>
            <Link
              to="/orders"
              className={`ds-nav__link ${location.pathname === "/orders" ? "ds-nav__link--active" : ""}`}
            >
              My Orders
            </Link>
          </div>
        </div>
      </nav>
      <main>{children}</main>
    </div>
  );
}
