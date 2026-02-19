import { Link, useLocation } from "react-router-dom";

export default function AppShell({ brandLabel = "Ops Dashboard", children }) {
  const location = useLocation();
  return (
    <div className="ds-page">
      <nav className="ds-nav">
        <div className="ds-nav__inner">
          <Link to="/search" className="ds-nav__brand">
            {brandLabel}
          </Link>
          <div className="ds-nav__links">
            <Link
              to="/search"
              className={`ds-nav__link ${location.pathname === "/search" ? "ds-nav__link--active" : ""}`}
            >
              Search payments
            </Link>
          </div>
        </div>
      </nav>
      <main>{children}</main>
    </div>
  );
}
