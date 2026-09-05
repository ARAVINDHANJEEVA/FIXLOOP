import React from 'react';
import { Clock, AlertTriangle, AlertOctagon } from 'lucide-react';
import { useCountdown } from '../../hooks/useCountdown';

interface SLACountdownProps {
  deadline: string;
  compact?: boolean;
}

const SLACountdown: React.FC<SLACountdownProps> = ({ deadline, compact = false }) => {
  const { formatted, urgency } = useCountdown(deadline);

  const Icon =
    urgency === 'overdue' ? AlertOctagon :
    urgency === 'critical' ? AlertTriangle : Clock;

  return (
    <span className={`sla-countdown sla-${urgency}`}>
      <Icon size={12} />
      {!compact && formatted}
      {compact && (urgency === 'overdue' ? 'BREACH' : formatted)}
    </span>
  );
};

export default SLACountdown;
