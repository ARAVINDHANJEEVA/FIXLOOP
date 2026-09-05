import React, { ReactNode } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { X, AlertTriangle } from 'lucide-react';

interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warning' | 'default';
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
  children?: ReactNode;
}

const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'default',
  onConfirm,
  onCancel,
  isLoading,
  children,
}) => {
  const confirmClass =
    variant === 'danger' ? 'btn btn-danger' :
    variant === 'warning' ? 'btn btn-warning' : 'btn btn-primary';

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          className="modal-overlay"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onCancel}
        >
          <motion.div
            className="modal"
            style={{ maxWidth: '440px' }}
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.9, opacity: 0 }}
            transition={{ type: 'spring', stiffness: 400, damping: 30 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <div className="flex items-center gap-2">
                <span
                  style={{
                    width: 32, height: 32, borderRadius: 8,
                    background: variant === 'danger' ? 'rgba(239,68,68,0.15)' : variant === 'warning' ? 'rgba(245,158,11,0.15)' : 'rgba(59,130,246,0.15)',
                    color: variant === 'danger' ? '#ef4444' : variant === 'warning' ? '#f59e0b' : '#3b82f6',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}
                >
                  <AlertTriangle size={16} />
                </span>
                <span className="modal-title">{title}</span>
              </div>
              <button className="btn btn-ghost btn-icon" onClick={onCancel}>
                <X size={16} />
              </button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.875rem', color: 'var(--text-2)', lineHeight: 1.6, marginBottom: children ? 16 : 0 }}>
                {message}
              </p>
              {children}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={onCancel} disabled={isLoading}>
                {cancelLabel}
              </button>
              <button className={confirmClass} onClick={onConfirm} disabled={isLoading}>
                {isLoading ? 'Processing...' : confirmLabel}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};

export default ConfirmDialog;
