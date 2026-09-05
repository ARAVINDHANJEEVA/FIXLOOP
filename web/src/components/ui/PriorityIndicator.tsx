import React from 'react';
import { getPriorityColor, getPriorityLevel } from '../../utils/helpers';

interface PriorityIndicatorProps {
  score: number;
  showBar?: boolean;
  showLabel?: boolean;
}

const PriorityIndicator: React.FC<PriorityIndicatorProps> = ({
  score,
  showBar = true,
  showLabel = false,
}) => {
  const color = getPriorityColor(score);
  const level = getPriorityLevel(score);
  const score100 = score <= 1.0 && score > 0 ? score * 100 : score;
  const barWidth = Math.min(100, Math.max(0, score100));

  return (
    <div className="priority-indicator">
      <span
        className="priority-dot"
        style={{ background: color, boxShadow: `0 0 6px ${color}60` }}
      />
      {showBar && (
        <div className="priority-bar-track">
          <div
            className="priority-bar-fill"
            style={{ width: `${barWidth}%`, background: color }}
          />
        </div>
      )}
      <span className="priority-score-text" style={{ color }}>
        {showLabel ? level : score100.toFixed(1)}
      </span>
    </div>
  );
};

export default PriorityIndicator;
