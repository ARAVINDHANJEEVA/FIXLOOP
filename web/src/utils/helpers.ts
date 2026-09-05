import { ComplaintStatus, PriorityLevel } from '../types';

export const getStatusColor = (status: ComplaintStatus): string => {
  const colors: Record<ComplaintStatus, string> = {
    SUBMITTED: 'var(--text-3)',
    UNDER_ADMIN_REVIEW: 'var(--accent)',
    ASSIGNED: 'var(--accent)',
    IN_PROGRESS: 'var(--warning)',
    AWAITING_STUDENT_INFO: 'var(--orange)',
    VENDOR_ASSIGNED: 'var(--purple)',
    RESOLVED: 'var(--success)',
    REOPENED: 'var(--danger)',
    CLOSED: 'var(--text-3)',
  };
  return colors[status] ?? 'var(--text-3)';
};

export const getPriorityLevel = (score: number): PriorityLevel => {
  const s = score <= 1.0 && score > 0 ? score * 100 : score;
  if (s >= 80) return 'CRITICAL';
  if (s >= 60) return 'HIGH';
  if (s >= 35) return 'MEDIUM';
  return 'LOW';
};

export const getPriorityColor = (score: number): string => {
  const level = getPriorityLevel(score);
  const colors: Record<PriorityLevel, string> = {
    CRITICAL: '#ef4444',
    HIGH: '#f97316',
    MEDIUM: '#f59e0b',
    LOW: '#22c55e',
  };
  return colors[level];
};

export const formatTimeAgo = (dateStr: string): string => {
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins !== 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours !== 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays !== 1 ? 's' : ''} ago`;
  } catch {
    return dateStr;
  }
};

export const formatDateTime = (dateStr: string): string => {
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' }) +
      ' ' + date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false });
  } catch {
    return dateStr;
  }
};

export const formatFullDateTime = (dateStr: string): string => {
  try {
    const date = new Date(dateStr);
    return (
      date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      }) +
      ' at ' +
      date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      })
    );
  } catch {
    return dateStr;
  }
};

export const formatReportedDateTime = (dateStr: string): string => {
  try {
    const full = formatFullDateTime(dateStr);
    const ago = formatTimeAgo(dateStr);
    return `${full} (${ago})`;
  } catch {
    return dateStr;
  }
};

export const truncate = (str: string, maxLen: number = 80): string => {
  if (!str || str.length <= maxLen) return str;
  return str.slice(0, maxLen).trimEnd() + '…';
};

export const getCategoryIcon = (name: string): string => {
  const icons: Record<string, string> = {
    Hostel: 'Home',
    Food: 'Utensils',
    Academic: 'BookOpen',
    Health: 'Activity',
    Hygiene: 'Sparkles',
    'Tech Support': 'Laptop',
    Harassment: 'Lock',
    Malpractice: 'AlertTriangle',
    Ragging: 'ShieldAlert',
  };
  return icons[name] ?? 'FileText';
};

export const getCategoryColor = (name: string): string => {
  const colors: Record<string, string> = {
    Hostel: '#6366f1',
    Food: '#f59e0b',
    Academic: '#3b82f6',
    Health: '#ef4444',
    Hygiene: '#22c55e',
    'Tech Support': '#06b6d4',
    Harassment: '#ec4899',
    Malpractice: '#f97316',
    Ragging: '#dc2626',
  };
  return colors[name] ?? '#64748b';
};

export const clsx = (...classes: (string | undefined | null | false)[]): string =>
  classes.filter(Boolean).join(' ');
