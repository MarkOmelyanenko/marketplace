import { Link } from 'react-router-dom';

export default function NotFound({ homePath = '/', homeLabel = 'Go to home' }) {
  return (
    <div className="ds-page">
      <div className="ds-not-found">
        <div className="ds-not-found__code">404</div>
        <h1 className="ds-not-found__title">Page not found</h1>
        <p className="ds-not-found__desc">
          The page you're looking for doesn't exist or has been moved.
        </p>
        <Link to={homePath} className="ds-btn ds-btn--primary">
          {homeLabel}
        </Link>
      </div>
    </div>
  );
}
