import React from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { MapPin, Users, Clock, ChevronRight, Layers, AlertTriangle, XOctagon, Wrench, User } from 'lucide-react';
import { Complaint } from '../../types';
import StatusBadge from '../ui/StatusBadge';
import PriorityIndicator from '../ui/PriorityIndicator';
import SLACountdown from '../ui/SLACountdown';
import { formatTimeAgo, formatDateTime, formatReportedDateTime, truncate, getCategoryColor, getPriorityLevel } from '../../utils/helpers';

interface ComplaintCardProps {
  complaint: Complaint;
  role: 'admin' | 'staff';
  index?: number;
}

const cardVariants = {
  hidden: { opacity: 0, y: 14 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.04, type: 'spring' as const, stiffness: 340, damping: 30 },
  }),
};

const ComplaintCard: React.FC<ComplaintCardProps> = ({ complaint, role, index = 0 }) => {
  const navigate = useNavigate();
  const priorityLevel = getPriorityLevel(complaint.priorityScore);
  const catColor = getCategoryColor(complaint.categoryName);

  return (
    <motion.div
      className={`complaint-card priority-${priorityLevel}`}
      onClick={() => navigate(`/${role}/complaint/${complaint.id}`)}
      custom={index}
      variants={cardVariants}
      initial="hidden"
      animate="visible"
    >
      {/* Header row */}
      <div className="complaint-card-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 7, flexWrap: 'wrap' }}>
          <span className="complaint-token">{complaint.token}</span>

          <span
            className="complaint-category-tag"
            style={{ borderColor: `${catColor}28`, color: catColor, background: `${catColor}10` }}
          >
            {complaint.categoryName}
          </span>

          {complaint.incidentCount && complaint.incidentCount > 1 && (
            <span className="cluster-badge">
              <Layers size={9} />
              {complaint.incidentCount}
            </span>
          )}

          {complaint.isAtRisk && !complaint.isBreached && (
            <span className="at-risk-badge">
              <AlertTriangle size={9} />
              At Risk
            </span>
          )}

          {complaint.isBreached && (
            <span className="breached-badge">
              <XOctagon size={9} />
              Breached
            </span>
          )}
        </div>
        <StatusBadge status={complaint.status} />
      </div>

      {/* Description */}
      <p className="complaint-description">{truncate(complaint.description, 120)}</p>

      {/* Meta row */}
      <div className="complaint-meta">
        <PriorityIndicator score={complaint.priorityScore} showBar />

        <span className="complaint-meta-item">
          <MapPin size={11} />
          {complaint.location.building}
        </span>

        <span className="complaint-meta-item">
          <Users size={11} />
          {complaint.studentsAffected}
        </span>

        <span className="complaint-meta-item" title={`Reported on: ${formatReportedDateTime(complaint.createdAt)}`}>
          <Clock size={11} />
          {formatDateTime(complaint.createdAt)} ({formatTimeAgo(complaint.createdAt)})
        </span>

        <span style={{ marginLeft: 'auto' }}>
          <SLACountdown deadline={complaint.slaDeadline} />
        </span>

        <ChevronRight size={13} style={{ color: 'var(--text-3)', flexShrink: 0 }} />
      </div>

      {/* Assigned staff */}
      {complaint.assignedToStaff && (
        <div
          style={{
            marginTop: 10,
            paddingTop: 10,
            borderTop: '1px solid var(--border)',
            display: 'flex',
            alignItems: 'center',
            gap: 6,
            fontSize: '0.74rem',
            color: 'var(--text-3)',
          }}
        >
          <div
            style={{
              width: 20, height: 20, borderRadius: '50%',
              background: 'linear-gradient(135deg, var(--accent), #6366f1)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '0.52rem', fontWeight: 800, color: 'white',
            }}
          >
            {complaint.assignedToStaff.avatarInitials}
          </div>
          <span>
            Assigned to{' '}
            <span style={{ color: 'var(--text-2)', fontWeight: 600 }}>
              {complaint.assignedToStaff.name}
            </span>
          </span>
          <span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 4, color: 'var(--text-3)' }}>
            {complaint.dispatchType === 'EXTERNAL_VENDOR'
              ? <><Wrench size={11} /> Vendor</>
              : <><User size={11} /> Internal</>
            }
          </span>
        </div>
      )}
    </motion.div>
  );
};

export default ComplaintCard;
