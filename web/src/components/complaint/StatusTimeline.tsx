import React from 'react';
import { motion } from 'framer-motion';
import { StatusHistoryEntry } from '../../types';
import StatusBadge from '../ui/StatusBadge';
import { formatDateTime } from '../../utils/helpers';
import { getStatusColor } from '../../utils/helpers';

interface StatusTimelineProps {
  history: StatusHistoryEntry[];
}

const StatusTimeline: React.FC<StatusTimelineProps> = ({ history }) => {
  const sorted = [...history].sort(
    (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
  );

  return (
    <div className="timeline">
      {sorted.map((entry, i) => {
        const color = getStatusColor(entry.status);
        return (
          <motion.div
            key={entry.id}
            className="timeline-item"
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: i * 0.06 }}
          >
            <div className="timeline-dot" style={{ background: `${color}20`, border: `2px solid ${color}` }}>
              <div style={{ width: 5, height: 5, borderRadius: '50%', background: color }} />
            </div>
            <div className="timeline-header">
              <StatusBadge status={entry.status} showDot={false} />
              <span className="timeline-by">by {entry.changedBy}</span>
              <span style={{ fontSize: '0.65rem', color: 'var(--text-3)', background: 'var(--bg-elevated)', padding: '1px 6px', borderRadius: 4 }}>
                {entry.changedByRole}
              </span>
              <span className="timeline-time">{formatDateTime(entry.timestamp)}</span>
            </div>
            {entry.note && (
              <div className="timeline-note">
                "{entry.note}"
              </div>
            )}
          </motion.div>
        );
      })}
    </div>
  );
};

export default StatusTimeline;
