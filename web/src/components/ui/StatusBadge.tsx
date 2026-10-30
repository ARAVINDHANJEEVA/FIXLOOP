import React from 'react';
import { ComplaintStatus } from '../../types';

const statusLabels: Record<ComplaintStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_ADMIN_REVIEW: 'Under Admin Review',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  AWAITING_STUDENT_INFO: 'Awaiting Info',
  VENDOR_ASSIGNED: 'Vendor Assigned',
  RESOLVED: 'Resolved',
  REOPENED: 'Reopened',
  CLOSED: 'Closed',
};

interface StatusBadgeProps {
  status: ComplaintStatus;
  showDot?: boolean;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, showDot = true }) => {
  return (
    <span className={`status-badge status-${status}`}>
      {showDot && <span className="status-badge-dot" />}
      {statusLabels[status] || status}
    </span>
  );
};

export default StatusBadge;
