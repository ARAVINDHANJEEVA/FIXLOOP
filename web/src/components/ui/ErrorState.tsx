import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
}

const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Unable to Load Live Data',
  message = 'Failed to connect to the live Fixloop backend. Please check that the server is running and try again.',
  onRetry,
}) => (
  <div
    className="empty-state-container"
    style={{
      padding: '56px 24px',
      textAlign: 'center',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
    }}
  >
    <div
      style={{
        width: 52,
        height: 52,
        borderRadius: 14,
        background: 'rgba(239, 68, 68, 0.12)',
        color: '#ef4444',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
      }}
    >
      <AlertTriangle size={26} />
    </div>
    <div style={{ fontSize: '1.05rem', fontWeight: 700, color: 'var(--text-1)', marginBottom: 6 }}>
      {title}
    </div>
    <p
      style={{
        fontSize: '0.84rem',
        color: 'var(--text-3)',
        maxWidth: 440,
        margin: '0 auto 20px',
        lineHeight: 1.5,
      }}
    >
      {message}
    </p>
    {onRetry && (
      <button
        type="button"
        className="btn btn-secondary btn-sm"
        onClick={onRetry}
        style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
      >
        <RefreshCw size={13} />
        <span>Retry Connection</span>
      </button>
    )}
  </div>
);

export default ErrorState;
