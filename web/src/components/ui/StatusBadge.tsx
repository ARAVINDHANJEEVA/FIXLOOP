import React from 'react';
import { ComplaintStatus } from '../../types';
import { statusLabels } from '../../data/mockData';

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
