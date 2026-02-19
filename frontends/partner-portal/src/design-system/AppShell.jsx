import { Link, useLocation } from "react-router-dom";

export default function AppShell({ brandLabel = "Partner Portal", children }) {
  const location = useLocation();
  return (
    <div className="ds-page">
      <nav className="ds-nav">
        <div className="ds-nav__inner">
          <Link to="/offers" className="ds-nav__brand">
            {brandLabel}
          </Link>
          <div className="ds-nav__links">
            <Link
              to="/offers"
              className={`ds-nav__link ${location.pathname.startsWith("/offers") ? "ds-nav__link--active" : ""}`}
            >
              My Offers
            </Link>
            <Link
              to="/offers/new"
              className={`ds-nav__link ${location.pathname === "/offers/new" ? "ds-nav__link--active" : ""}`}
            >
              Create Offer
            </Link>
          </div>
        </div>
      </nav>
      <main>{children}</main>
    </div>
  );
}
