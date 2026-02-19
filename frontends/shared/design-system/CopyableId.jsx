import { useState, useCallback } from 'react';

export default function CopyableId({ id, truncateLength = 8 }) {
  const [copied, setCopied] = useState(false);

  const shortId = id ? `${id.substring(0, truncateLength)}…` : '';

  const handleCopy = useCallback(async () => {
    if (!id) return;
    try {
      await navigator.clipboard.writeText(id);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = id;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }, [id]);

  return (
    <span
      className="ds-copyable"
      onClick={handleCopy}
      title={`Click to copy: ${id}`}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') handleCopy(); }}
    >
      {shortId}
      <span className="ds-copyable__icon" aria-hidden="true">📋</span>
      {copied && <span className="ds-copyable__tooltip">Copied!</span>}
    </span>
  );
}
