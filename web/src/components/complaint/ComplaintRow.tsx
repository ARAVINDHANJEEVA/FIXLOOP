import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Layers, AlertTriangle, XOctagon } from 'lucide-react';
import { Complaint } from '../../types';
import StatusBadge from '../ui/StatusBadge';
import PriorityIndicator from '../ui/PriorityIndicator';
import SLACountdown from '../ui/SLACountdown';
import CategoryIcon from '../ui/CategoryIcon';
import { truncate, getCategoryColor, formatDateTime, formatTimeAgo, formatReportedDateTime } from '../../utils/helpers';

interface ComplaintRowProps {
  complaint: Complaint;
  role: 'admin' | 'staff';
}

const ComplaintRow: React.FC<ComplaintRowProps> = ({
  complaint,
  role,
}) => {
  const navigate = useNavigate();
  const catColor = getCategoryColor(complaint.categoryName);

  return (
    <tr
      className="modern-table-row clickable-table-row"
      onClick={() => navigate(`/${role}/complaint/${complaint.id}`)}
      title="Click to view complaint details"
    >
      {/* Token & Badges */}
      <td className="col-token">
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          <span className="table-token-badge">
            {complaint.token}
          </span>
          <div style={{ display: 'flex', gap: 4, alignItems: 'center', flexWrap: 'wrap' }}>
            {complaint.incidentCount && complaint.incidentCount > 1 && (
              <span className="cluster-badge" style={{ fontSize: '0.62rem' }}>
                <Layers size={9} />
                {complaint.incidentCount}
              </span>
            )}
            {complaint.isAtRisk && !complaint.isBreached && (
              <span className="at-risk-badge" style={{ fontSize: '0.62rem' }}>
                <AlertTriangle size={9} /> Risk
              </span>
            )}
            {complaint.isBreached && (
              <span className="breached-badge" style={{ fontSize: '0.62rem' }}>
                <XOctagon size={9} /> Breach
              </span>
            )}
          </div>
        </div>
      </td>

      {/* Category */}
      <td className="col-category">
        <span
          className="table-category-pill"
          style={{
            borderColor: `${catColor}30`,
            color: catColor,
            background: `${catColor}12`,
          }}
        >
          <CategoryIcon name={complaint.categoryName} size={12} />
          {complaint.categoryName}
        </span>
      </td>

      {/* Description - Generous breathing room */}
      <td className="col-description">
        <div className="table-desc-title">
          {truncate(complaint.description, 95)}
        </div>
        <div className="table-desc-reporter" style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
          <span>{complaint.isAnonymous ? 'Anonymous Student' : (complaint.student?.name || 'Student')}</span>
          <span style={{ color: 'var(--text-3)', fontSize: '0.68rem' }}>•</span>
          <span style={{ color: 'var(--text-3)', fontSize: '0.70rem' }} title={`Reported on: ${formatReportedDateTime(complaint.createdAt)}`}>
            Reported {formatDateTime(complaint.createdAt)} ({formatTimeAgo(complaint.createdAt)})
          </span>
        </div>
      </td>

      {/* Location */}
      <td className="col-location">
        <div className="table-location-text">
          {complaint.location.building}
        </div>
        <div className="table-location-sub">
          {complaint.location.room || complaint.location.floor || 'Campus'}
        </div>
      </td>

      {/* Priority */}
      <td className="col-priority">
        <PriorityIndicator score={complaint.priorityScore} showBar={true} />
      </td>

      {/* SLA Countdown */}
      <td className="col-sla">
        <SLACountdown deadline={complaint.slaDeadline} compact={true} />
      </td>

      {/* Status Badge */}
      <td className="col-status">
        <StatusBadge status={complaint.status} />
      </td>
    </tr>
  );
};

export default ComplaintRow;
