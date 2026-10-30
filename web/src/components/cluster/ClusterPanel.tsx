import React, { useState, useEffect } from 'react';
import {
  Link2,
  Link2Off,
  Plus,
  ChevronDown,
  ChevronUp,
  X,
  AlertCircle,
  RotateCcw,
} from 'lucide-react';
import { Complaint, IncidentSummary } from '../../types';
import { complaintApi, incidentApi } from '../../api';
import { useToast } from '../../hooks/useToast';

interface ClusterPanelProps {
  complaint: Complaint;
  onUpdated: (updated: Complaint) => void;
}

// ─── Score badge ──────────────────────────────────────────────────────────────

const ScoreBadge: React.FC<{ score?: number }> = ({ score }) => {
  if (score === undefined || score === null) return null;
  const pct = Math.round(score * 100);
  const color =
    pct >= 80 ? '#34d399' : pct >= 60 ? '#fbbf24' : '#f87171';
  return (
    <span
      style={{
        fontSize: '0.68rem',
        fontFamily: 'monospace',
        color,
        fontWeight: 700,
        marginLeft: 6,
      }}
    >
      {pct}% match
    </span>
  );
};

// ─── Manual cluster form ──────────────────────────────────────────────────────

interface ManualClusterFormProps {
  complaint: Complaint;
  openIncidents: IncidentSummary[];
  loading: boolean;
  selectedIncidentId: string;
  onSelect: (id: string) => void;
  onCluster: () => void;
  isClustering: boolean;
  onCancel: () => void;
}

const ManualClusterForm: React.FC<ManualClusterFormProps> = ({
  complaint,
  openIncidents,
  loading,
  selectedIncidentId,
  onSelect,
  onCluster,
  isClustering,
  onCancel,
}) => {
  const available = openIncidents.filter((i) => i.id !== complaint.incidentId);

  return (
    <div
      style={{
        marginTop: 10,
        padding: '12px 14px',
        background: 'var(--bg-elevated)',
        borderRadius: 12,
        border: '1px solid rgba(56, 189, 248, 0.2)',
      }}
    >
      <div
        style={{
          fontSize: '0.72rem',
          fontWeight: 700,
          color: 'var(--accent)',
          marginBottom: 8,
          textTransform: 'uppercase',
          letterSpacing: '0.04em',
        }}
      >
        Select Target Incident
      </div>

      {loading ? (
        <div style={{ fontSize: '0.78rem', color: 'var(--text-3)', fontStyle: 'italic' }}>
          Loading open incidents…
        </div>
      ) : available.length === 0 ? (
        <div style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>
          No open incidents found for this category.
        </div>
      ) : (
        <select
          value={selectedIncidentId}
          onChange={(e) => onSelect(e.target.value)}
          style={{
            width: '100%',
            background: 'var(--bg-surface)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            padding: '7px 10px',
            fontSize: '0.78rem',
            color: 'var(--text-1)',
            marginBottom: 8,
            cursor: 'pointer',
            outline: 'none',
          }}
        >
          <option value="">— Select an incident —</option>
          {available.map((inc) => (
            <option key={inc.id} value={inc.id}>
              Incident #{inc.id} · {inc.complaintCount} complaint
              {inc.complaintCount !== 1 ? 's' : ''}
              {inc.summary ? ` · "${inc.summary.slice(0, 45)}…"` : ''}
            </option>
          ))}
        </select>
      )}

      <div style={{ display: 'flex', gap: 8 }}>
        <button
          onClick={onCluster}
          disabled={!selectedIncidentId || isClustering}
          className="btn btn-primary btn-sm"
          style={{ flex: 1, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 5 }}
        >
          <Link2 size={12} />
          {isClustering ? 'Clustering…' : 'Cluster →'}
        </button>
        <button
          onClick={onCancel}
          className="btn btn-ghost btn-sm"
          style={{ padding: '0 14px' }}
        >
          Cancel
        </button>
      </div>
    </div>
  );
};

// ─── Main ClusterPanel component ──────────────────────────────────────────────

const ClusterPanel: React.FC<ClusterPanelProps> = ({ complaint, onUpdated }) => {
  const { addToast } = useToast();

  const [expanded, setExpanded] = useState(false);

  // Uncluster state
  const [showUncluster, setShowUncluster] = useState(false);
  const [unclusterReason, setUnclusterReason] = useState('');
  const [isUnclustering, setIsUnclustering] = useState(false);

  // Manual cluster state
  const [showCluster, setShowCluster] = useState(false);
  const [openIncidents, setOpenIncidents] = useState<IncidentSummary[]>([]);
  const [loadingIncidents, setLoadingIncidents] = useState(false);
  const [selectedIncidentId, setSelectedIncidentId] = useState('');
  const [isClustering, setIsClustering] = useState(false);

  const isClustered = !!complaint.incident;
  const isExcluded = complaint.clusterExcluded;

  // Load open incidents when manual-cluster panel opens
  useEffect(() => {
    if (!showCluster) return;
    setLoadingIncidents(true);
    incidentApi
      .getOpenByCategory(complaint.categoryId)
      .then(setOpenIncidents)
      .catch(() => setOpenIncidents([]))
      .finally(() => setLoadingIncidents(false));
  }, [showCluster, complaint.categoryId]);

  const handleUncluster = async () => {
    setIsUnclustering(true);
    try {
      const updated = await complaintApi.uncluster(complaint.id, { reason: unclusterReason });
      addToast({ type: 'success', title: 'Unclustered', message: 'Complaint removed from cluster.' });
      setShowUncluster(false);
      setUnclusterReason('');
      onUpdated(updated);
    } catch {
      addToast({ type: 'error', title: 'Failed', message: 'Could not remove complaint from cluster.' });
    } finally {
      setIsUnclustering(false);
    }
  };

  const handleCluster = async () => {
    if (!selectedIncidentId) return;
    setIsClustering(true);
    try {
      const updated = await complaintApi.clusterManually(complaint.id, {
        targetIncidentId: Number(selectedIncidentId),
      });
      addToast({ type: 'success', title: 'Clustered', message: `Merged into Incident #${selectedIncidentId}.` });
      setShowCluster(false);
      setSelectedIncidentId('');
      onUpdated(updated);
    } catch {
      addToast({ type: 'error', title: 'Failed', message: 'Could not cluster complaint.' });
    } finally {
      setIsClustering(false);
    }
  };

  // ─── State 1: Clustered ────────────────────────────────────────────────────

  if (isClustered && complaint.incident) {
    const { incident } = complaint;
    const allIds = incident.members.map((m) => `#${m.id}`).join(', ');

    return (
      <div
        style={{
          padding: '14px 16px',
          background: 'rgba(56, 189, 248, 0.05)',
          border: '1px solid rgba(56, 189, 248, 0.2)',
          borderRadius: 12,
        }}
      >
        {/* Header row */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <Link2 size={13} style={{ color: 'var(--accent)' }} />
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--accent)' }}>
              Incident #{incident.id}
            </span>
            <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
              · {incident.complaintCount} complaint{incident.complaintCount !== 1 ? 's' : ''}
            </span>
            <ScoreBadge score={complaint.clusterScore} />
          </div>
          <button
            onClick={() => setShowUncluster(true)}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 4,
              padding: '3px 9px',
              borderRadius: 8,
              fontSize: '0.7rem',
              fontWeight: 600,
              color: '#f87171',
              background: 'rgba(239, 68, 68, 0.08)',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              cursor: 'pointer',
            }}
          >
            <Link2Off size={10} />
            Uncluster
          </button>
        </div>

        {/* Complaint IDs bar */}
        <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', marginBottom: 6 }}>
          <span style={{ color: 'var(--text-3)' }}>Complaint IDs: </span>
          <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--text-1)' }}>
            {allIds}
          </span>
        </div>

        {/* Expand / collapse members */}
        {incident.members.length > 0 && (
          <>
            <button
              onClick={() => setExpanded(!expanded)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                fontSize: '0.72rem',
                color: 'var(--text-3)',
                cursor: 'pointer',
                background: 'none',
                border: 'none',
                padding: 0,
              }}
            >
              {expanded ? <ChevronUp size={11} /> : <ChevronDown size={11} />}
              {expanded ? 'Hide' : 'Show'} cluster details
            </button>

            {expanded && (
              <div style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 6 }}>
                {incident.members.map((m) => {
                  const isSelf = m.id === complaint.id;
                  return (
                    <div
                      key={m.id}
                      style={{
                        padding: '8px 10px',
                        borderRadius: 8,
                        border: `1px solid ${isSelf ? 'rgba(56, 189, 248, 0.35)' : 'var(--border)'}`,
                        background: isSelf ? 'rgba(56, 189, 248, 0.07)' : 'var(--bg-elevated)',
                        fontSize: '0.75rem',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 3 }}>
                        <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--text-1)' }}>
                          #{m.id} · {m.token}
                        </span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          {isSelf && (
                            <span style={{ fontSize: '0.62rem', color: 'var(--accent)', fontWeight: 700 }}>
                              current
                            </span>
                          )}
                          {!isSelf && <ScoreBadge score={m.clusterScore} />}
                        </div>
                      </div>
                      <p
                        style={{
                          color: 'var(--text-2)',
                          lineHeight: 1.4,
                          margin: 0,
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                          overflow: 'hidden',
                        }}
                      >
                        {m.description || <em>No description</em>}
                      </p>
                    </div>
                  );
                })}
              </div>
            )}
          </>
        )}

        {/* Uncluster confirm dialog */}
        {showUncluster && (
          <div
            style={{
              marginTop: 10,
              padding: '12px 14px',
              background: 'rgba(239, 68, 68, 0.06)',
              border: '1px solid rgba(239, 68, 68, 0.2)',
              borderRadius: 12,
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                fontSize: '0.76rem',
                fontWeight: 700,
                color: '#f87171',
                marginBottom: 6,
              }}
            >
              <AlertCircle size={12} />
              Confirm Uncluster
            </div>
            <p style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginBottom: 8 }}>
              This complaint will be removed from Incident #{incident.id} and marked as cluster-excluded.
              It will not be auto-clustered again.
            </p>
            <textarea
              value={unclusterReason}
              onChange={(e) => setUnclusterReason(e.target.value)}
              placeholder="Reason (optional)"
              rows={2}
              style={{
                width: '100%',
                boxSizing: 'border-box',
                background: 'var(--bg-surface)',
                border: '1px solid var(--border)',
                borderRadius: 8,
                padding: '7px 10px',
                fontSize: '0.78rem',
                color: 'var(--text-1)',
                resize: 'none',
                outline: 'none',
                marginBottom: 8,
              }}
            />
            <div style={{ display: 'flex', gap: 8 }}>
              <button
                onClick={handleUncluster}
                disabled={isUnclustering}
                className="btn btn-sm"
                style={{
                  flex: 1,
                  background: '#dc2626',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 8,
                  padding: '7px 0',
                  fontSize: '0.78rem',
                  fontWeight: 600,
                  cursor: isUnclustering ? 'not-allowed' : 'pointer',
                  opacity: isUnclustering ? 0.6 : 1,
                }}
              >
                {isUnclustering ? 'Removing…' : 'Remove from Cluster'}
              </button>
              <button
                onClick={() => setShowUncluster(false)}
                className="btn btn-ghost btn-sm"
                style={{ padding: '0 14px' }}
              >
                Cancel
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ─── State 3: Manually excluded ───────────────────────────────────────────

  if (isExcluded) {
    return (
      <div
        style={{
          padding: '12px 14px',
          background: 'rgba(251, 191, 36, 0.05)',
          border: '1px solid rgba(251, 191, 36, 0.2)',
          borderRadius: 12,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
          <X size={13} style={{ color: '#fbbf24' }} />
          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#fbbf24' }}>
            Cluster Excluded
          </span>
        </div>
        <p style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginBottom: 8 }}>
          This complaint was manually removed from a cluster and will not be auto-clustered again.
          You can still manually assign it to an incident below.
        </p>
        <button
          onClick={() => setShowCluster(!showCluster)}
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 5,
            padding: '5px 12px',
            borderRadius: 8,
            fontSize: '0.75rem',
            fontWeight: 600,
            color: 'var(--text-2)',
            background: 'var(--bg-elevated)',
            border: '1px solid var(--border)',
            cursor: 'pointer',
          }}
        >
          <RotateCcw size={11} />
          Manually assign to incident
        </button>

        {showCluster && (
          <ManualClusterForm
            complaint={complaint}
            openIncidents={openIncidents}
            loading={loadingIncidents}
            selectedIncidentId={selectedIncidentId}
            onSelect={setSelectedIncidentId}
            onCluster={handleCluster}
            isClustering={isClustering}
            onCancel={() => setShowCluster(false)}
          />
        )}
      </div>
    );
  }

  // ─── State 2: Standalone (not clustered) ─────────────────────────────────

  return (
    <div
      style={{
        padding: '12px 14px',
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border)',
        borderRadius: 12,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4 }}>
        <Plus size={13} style={{ color: 'var(--text-3)' }} />
        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-2)' }}>
          Not Clustered
        </span>
      </div>
      <p style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginBottom: 8 }}>
        This complaint is standalone. You can manually assign it to an existing open incident.
      </p>
      <button
        onClick={() => setShowCluster(!showCluster)}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 5,
          padding: '5px 12px',
          borderRadius: 8,
          fontSize: '0.75rem',
          fontWeight: 600,
          color: 'var(--accent)',
          background: 'var(--accent-dim)',
          border: '1px solid rgba(56, 189, 248, 0.2)',
          cursor: 'pointer',
        }}
      >
        <Link2 size={11} />
        Manually Cluster
      </button>

      {showCluster && (
        <ManualClusterForm
          complaint={complaint}
          openIncidents={openIncidents}
          loading={loadingIncidents}
          selectedIncidentId={selectedIncidentId}
          onSelect={setSelectedIncidentId}
          onCluster={handleCluster}
          isClustering={isClustering}
          onCancel={() => setShowCluster(false)}
        />
      )}
    </div>
  );
};

export default ClusterPanel;
