import React from 'react';

interface LoadingSkeletonProps {
  rows?: number;
  type?: 'card' | 'row' | 'stat' | 'chart';
}

const SkeletonLine: React.FC<{ width?: string; height?: number }> = ({ width = '100%', height = 14 }) => (
  <div className="skeleton" style={{ width, height, borderRadius: 6 }} />
);

const CardSkeleton: React.FC = () => (
  <div className="complaint-card" style={{ cursor: 'default' }}>
    <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 12 }}>
      <SkeletonLine width="120px" height={12} />
      <SkeletonLine width="80px" height={20} />
    </div>
    <SkeletonLine width="100%" height={12} />
    <div style={{ marginTop: 6 }}>
      <SkeletonLine width="80%" height={12} />
    </div>
    <div style={{ marginTop: 14, display: 'flex', gap: 12 }}>
      <SkeletonLine width="60px" height={10} />
      <SkeletonLine width="80px" height={10} />
      <SkeletonLine width="70px" height={10} />
    </div>
  </div>
);

const RowSkeleton: React.FC = () => (
  <tr>
    {[1, 2, 3, 4, 5, 6].map((i) => (
      <td key={i} style={{ padding: '12px 14px' }}>
        <SkeletonLine width={i === 1 ? '100px' : i === 2 ? '140px' : '80px'} height={12} />
      </td>
    ))}
  </tr>
);

const StatSkeleton: React.FC = () => (
  <div className="stat-card">
    <SkeletonLine width="40px" height={40} />
    <SkeletonLine width="60px" height={32} />
    <SkeletonLine width="80px" height={12} />
  </div>
);

const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({ rows = 4, type = 'card' }) => {
  if (type === 'stat') {
    return (
      <div className="stats-grid">
        {Array.from({ length: rows }).map((_, i) => <StatSkeleton key={i} />)}
      </div>
    );
  }

  if (type === 'row') {
    return (
      <>
        {Array.from({ length: rows }).map((_, i) => <RowSkeleton key={i} />)}
      </>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {Array.from({ length: rows }).map((_, i) => <CardSkeleton key={i} />)}
    </div>
  );
};

export default LoadingSkeleton;
