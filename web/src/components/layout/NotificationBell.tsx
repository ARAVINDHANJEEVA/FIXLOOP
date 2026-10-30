import React, { useState, useRef, useEffect } from 'react';
import { Bell, AlertOctagon, Zap, CheckCircle, Info, AlertTriangle, Check } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Notification } from '../../types';
import { formatTimeAgo } from '../../utils/helpers';

const typeIcons: Record<string, React.ReactNode> = {
  SLA_ALERT: <AlertOctagon size={13} />,
  NEW_COMPLAINT: <Zap size={13} />,
  STATUS_CHANGE: <CheckCircle size={13} />,
  ASSIGNMENT: <Info size={13} />,
  ESCALATION: <AlertTriangle size={13} />,
};

const typeColors: Record<string, string> = {
  SLA_ALERT: 'var(--danger)',
  NEW_COMPLAINT: 'var(--accent)',
  STATUS_CHANGE: 'var(--success)',
  ASSIGNMENT: 'var(--purple)',
  ESCALATION: 'var(--warning)',
};

const NotificationBell: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const ref = useRef<HTMLDivElement>(null);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllRead = () =>
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));

  const toggleRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setIsOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  return (
    <div ref={ref} className="notif-bell-wrapper">
      <button
        className="notif-bell-btn"
        onClick={() => setIsOpen((p) => !p)}
        title="Notifications"
      >
        <Bell size={17} />
        {unreadCount > 0 && (
          <span className="notif-bell-badge">{unreadCount}</span>
        )}
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            className="notif-dropdown"
            initial={{ opacity: 0, y: 8, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 8, scale: 0.96 }}
            transition={{ duration: 0.16 }}
          >
            <div className="notif-dropdown-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span className="notif-header-title">Notifications</span>
                {unreadCount > 0 && (
                  <span className="notif-unread-pill">{unreadCount} new</span>
                )}
              </div>
              {unreadCount > 0 && (
                <button className="notif-mark-read-btn" onClick={markAllRead}>
                  <Check size={12} />
                  <span>Mark all read</span>
                </button>
              )}
            </div>

            <div className="notif-dropdown-list">
              {notifications.length === 0 ? (
                <div className="notif-empty">No notifications</div>
              ) : (
                notifications.slice(0, 5).map((n) => (
                  <div
                    key={n.id}
                    className={`notif-item ${!n.read ? 'unread' : ''}`}
                    onClick={() => toggleRead(n.id)}
                  >
                    <div
                      className="notif-icon-circle"
                      style={{
                        background: `${typeColors[n.type] || 'var(--accent)'}15`,
                        color: typeColors[n.type] || 'var(--accent)',
                      }}
                    >
                      {typeIcons[n.type] || <Bell size={13} />}
                    </div>

                    <div className="notif-content">
                      <div className="notif-title">{n.title}</div>
                      <div className="notif-message">{n.message}</div>
                      <div className="notif-time">{formatTimeAgo(n.timestamp)}</div>
                    </div>

                    {!n.read && <span className="notif-unread-dot" />}
                  </div>
                ))
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default NotificationBell;
