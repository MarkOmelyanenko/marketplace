import { Link, useLocation, useNavigate } from "react-router-dom";

export default function AppShell({ children }) {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("opsToken");
    navigate("/login");
  };

  return (
    <div className="ds-page">
      <nav className="ds-nav">
        <div className="ds-nav__inner">
          <Link to="/search" className="ds-nav__brand">
            Ops Dashboard
          </Link>
          <div className="ds-nav__links">
            <Link
              to="/search"
              className={`ds-nav__link ${location.pathname === "/search" ? "ds-nav__link--active" : ""}`}
            >
              Search payments
            </Link>
            <Link
              to="/refund-requests"
              className={`ds-nav__link ${location.pathname === "/refund-requests" ? "ds-nav__link--active" : ""}`}
            >
              Refund requests
            </Link>
            <button className="ds-nav__logout" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </div>
      </nav>
      <main>{children}</main>
    </div>
  );
}
