import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, CheckCircle2, AlertCircle, AlertTriangle, Info } from 'lucide-react';
import { useToast, Toast } from '../../hooks/useToast';

const icons: Record<string, React.ReactNode> = {
  success: <CheckCircle2 size={16} style={{ color: 'var(--success)' }} />,
  error: <AlertCircle size={16} style={{ color: 'var(--danger)' }} />,
  warning: <AlertTriangle size={16} style={{ color: 'var(--warning)' }} />,
  info: <Info size={16} style={{ color: 'var(--accent)' }} />,
};

const ToastItem: React.FC<{ toast: Toast; onRemove: (id: string) => void }> = ({ toast, onRemove }) => {
  // Extract token if present (e.g. SNT-2026-000224)
  const tokenMatch = toast.message?.match(/SNT-\d{4}-\d{6}/);
  const token = tokenMatch ? tokenMatch[0] : null;

  const renderMessage = (message?: string) => {
    if (!message) return null;
    if (!token) return message;

    const parts = message.split(token);
    return (
      <>
        {parts[0]}
        <span className="toast-token-chip">{token}</span>
        {parts[1] && parts[1].replace(/^[\s—–-]+/, '')}
      </>
    );
  };

  return (
    <motion.div
      className="toast"
      initial={{ opacity: 0, y: 12, scale: 0.96 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: 8, scale: 0.96 }}
      transition={{ duration: 0.16, ease: 'easeOut' }}
      onClick={() => onRemove(toast.id)}
    >
      <div className="toast-icon-wrapper">
        {icons[toast.type]}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div className="toast-title">{toast.title}</div>
        {toast.message && (
          <div className="toast-message">
            {renderMessage(toast.message)}
          </div>
        )}
      </div>

      <button
        className="toast-close-btn"
        onClick={(e) => {
          e.stopPropagation();
          onRemove(toast.id);
        }}
        title="Dismiss"
      >
        <X size={13} />
      </button>
    </motion.div>
  );
};

const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToast();

  return (
    <div className="toast-container">
      <AnimatePresence>
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onRemove={removeToast} />
        ))}
      </AnimatePresence>
    </div>
  );
};

export default ToastContainer;
