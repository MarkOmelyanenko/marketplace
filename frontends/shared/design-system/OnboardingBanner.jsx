import { useState, useEffect } from 'react';

export default function OnboardingBanner({ storageKey, title, steps }) {
  const [dismissed, setDismissed] = useState(true);

  useEffect(() => {
    const wasDismissed = localStorage.getItem(storageKey) === 'true';
    setDismissed(wasDismissed);
  }, [storageKey]);

  const handleDismiss = () => {
    localStorage.setItem(storageKey, 'true');
    setDismissed(true);
  };

  if (dismissed || !steps || steps.length === 0) return null;

  return (
    <div className="ds-onboarding" role="region" aria-label="Getting started">
      <button className="ds-onboarding__dismiss" onClick={handleDismiss} aria-label="Dismiss guide">
        ×
      </button>
      <h2 className="ds-onboarding__title">{title}</h2>
      <ol className="ds-onboarding__steps">
        {steps.map((step, idx) => (
          <li key={idx} className="ds-onboarding__step">
            <span className="ds-onboarding__step-number">{idx + 1}</span>
            <span>{step}</span>
          </li>
        ))}
      </ol>
    </div>
  );
}
