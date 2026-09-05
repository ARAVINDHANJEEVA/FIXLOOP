import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Calculator, Shield, Users, RotateCcw, Clock } from 'lucide-react';
import { Complaint } from '../../types';
import { calculateComplaintPriority } from '../../utils/priority';
import { getPriorityColor, getPriorityLevel } from '../../utils/helpers';

interface PriorityFormulaModalProps {
  complaint: Complaint;
  isOpen: boolean;
  onClose: () => void;
}

const PriorityFormulaModal: React.FC<PriorityFormulaModalProps> = ({
  complaint,
  isOpen,
  onClose,
}) => {
  if (!isOpen) return null;

  const result = calculateComplaintPriority(complaint);
  const color = getPriorityColor(complaint.priorityScore);
  const level = getPriorityLevel(complaint.priorityScore);

  const factorCards = [
    {
      title: 'Severity Norm',
      icon: <Shield size={16} />,
      color: '#3b82f6',
      weight: '48% (w1 = 0.48)',
      formula: 'severity / 5',
      input: `Severity: ${complaint.severity} / 5`,
      normalized: result.severityNorm.toFixed(4),
      contribution: result.severityComponent.toFixed(4),
      points: (result.severityComponent * 100).toFixed(1),
    },
    {
      title: 'Scale Norm',
      icon: <Users size={16} />,
      color: '#6366f1',
      weight: '21% (w2 = 0.21)',
      formula: 'ln(1 + students) / ln(1 + 500)',
      input: `Students: ${complaint.studentsAffected} (cap 500)`,
      normalized: result.scaleNorm.toFixed(4),
      contribution: result.scaleComponent.toFixed(4),
      points: (result.scaleComponent * 100).toFixed(1),
    },
    {
      title: 'Recurrence Norm',
      icon: <RotateCcw size={16} />,
      color: '#22c55e',
      weight: '8% (w3 = 0.08)',
      formula: 'min(recurrence_90d, 10) / 10',
      input: `90d Cluster Incidents: ${complaint.recurrenceCountLast90Days ?? complaint.incidentCount ?? (complaint.siblingTokens ? complaint.siblingTokens.length : 0)}`,
      normalized: result.recurrenceNorm.toFixed(4),
      contribution: result.recurrenceComponent.toFixed(4),
      points: (result.recurrenceComponent * 100).toFixed(1),
    },
    {
      title: 'Time Urgency Norm',
      icon: <Clock size={16} />,
      color: '#f59e0b',
      weight: '23% (w4 = 0.23)',
      formula: 'min(hours_elapsed / sla_hours, 1.0)',
      input: `Elapsed vs SLA window`,
      normalized: result.timeUrgencyNorm.toFixed(4),
      contribution: result.timeUrgencyComponent.toFixed(4),
      points: (result.timeUrgencyComponent * 100).toFixed(1),
    },
  ];

  return (
    <AnimatePresence>
      <div
        className="modal-overlay"
        onClick={onClose}
        style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(0, 0, 0, 0.65)',
          backdropFilter: 'blur(4px)',
          zIndex: 9999,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 20,
        }}
      >
        <motion.div
          className="modal-card"
          onClick={(e) => e.stopPropagation()}
          initial={{ opacity: 0, scale: 0.95, y: 15 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: 15 }}
          style={{
            background: 'var(--bg-surface)',
            border: '1px solid var(--border-strong)',
            borderRadius: 18,
            width: '100%',
            maxWidth: 680,
            maxHeight: '90vh',
            overflowY: 'auto',
            boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.35)',
            padding: 24,
          }}
        >
          {/* Header */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 18, borderBottom: '1px solid var(--border)', paddingBottom: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 10,
                  background: 'rgba(99, 102, 241, 0.15)',
                  color: 'var(--accent)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <Calculator size={20} />
              </div>
              <div>
                <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 800, color: 'var(--text-1)' }}>
                  Dynamic Priority Scoring Breakdown
                </h3>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginTop: 2 }}>
                  Mathematical formula audit for <span style={{ fontFamily: 'monospace', fontWeight: 700, color: 'var(--accent-text)' }}>{complaint.token}</span>
                </div>
              </div>
            </div>
            <button
              onClick={onClose}
              style={{
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--text-3)',
                padding: 6,
                borderRadius: 8,
              }}
            >
              <X size={18} />
            </button>
          </div>

          {/* Formula Specification Code Box */}
          <div
            style={{
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border)',
              borderRadius: 12,
              padding: '12px 16px',
              fontFamily: 'JetBrains Mono, monospace',
              fontSize: '0.74rem',
              color: 'var(--text-2)',
              lineHeight: 1.6,
              marginBottom: 16,
            }}
          >
            <div style={{ color: 'var(--text-3)', fontSize: '0.68rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: 2 }}>
              Active Scoring Model
            </div>
            <span style={{ color: '#3b82f6' }}>severity_norm</span> = severity / 5<br />
            <span style={{ color: '#6366f1' }}>scale_norm</span> = ln(1 + students) / ln(1 + 500)&nbsp;&nbsp;<span style={{ color: 'var(--text-3)' }}>{"// cap 500"}</span><br />
            <span style={{ color: '#22c55e' }}>recurrence_norm</span> = min(recurrence_last_90d, 10) / 10<br />
            <span style={{ color: '#f59e0b' }}>time_urgency_norm</span> = min(hours_created / sla_hours, 1.0)<br />
            <span style={{ color: 'var(--accent)', fontWeight: 700 }}>priority_score</span> = (0.48 × severity_norm) + (0.21 × scale_norm) + (0.08 × recurrence_norm) + (0.23 × time_urgency_norm)
          </div>

          {/* Score Hero Banner */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border)',
              borderRadius: 14,
              padding: '16px 20px',
              marginBottom: 16,
            }}
          >
            <div>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', textTransform: 'uppercase', fontWeight: 700 }}>
                Synthesized Priority Score
              </div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginTop: 4 }}>
                <span style={{ fontSize: '2.4rem', fontWeight: 900, color, letterSpacing: '-0.03em' }}>
                  {complaint.priorityScore.toFixed(1)}
                </span>
                <span style={{ fontSize: '0.9rem', color: 'var(--text-3)', fontWeight: 600 }}>/ 100</span>
                <span
                  style={{
                    fontSize: '0.74rem',
                    fontWeight: 800,
                    padding: '3px 10px',
                    borderRadius: 999,
                    background: `${color}18`,
                    color,
                    border: `1px solid ${color}35`,
                  }}
                >
                  {level}
                </span>
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>Raw Formula Result (0.0 – 1.0)</div>
              <div style={{ fontFamily: 'monospace', fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-1)', marginTop: 2 }}>
                {result.rawScore.toFixed(4)}
              </div>
              <div style={{ fontSize: '0.68rem', color: 'var(--text-3)', marginTop: 2 }}>
                Weighted sum of all 4 normalized tiers
              </div>
            </div>
          </div>

          {/* Proportional Contribution Bar */}
          <div style={{ marginBottom: 18 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.72rem', color: 'var(--text-3)', marginBottom: 6 }}>
              <span>Contribution Breakdown</span>
              <span>Total: {complaint.priorityScore.toFixed(1)} pts</span>
            </div>
            <div style={{ display: 'flex', height: 10, borderRadius: 999, overflow: 'hidden', background: 'var(--border)' }}>
              {factorCards.map((card, idx) => {
                const widthPct = complaint.priorityScore > 0 ? (parseFloat(card.points) / complaint.priorityScore) * 100 : 25;
                return (
                  <div
                    key={idx}
                    title={`${card.title}: +${card.points} pts`}
                    style={{
                      width: `${widthPct}%`,
                      background: card.color,
                      transition: 'width 0.4s ease',
                    }}
                  />
                );
              })}
            </div>
          </div>

          {/* Factor Cards Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
            {factorCards.map((card, idx) => (
              <div
                key={idx}
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 12,
                  padding: 14,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ color: card.color }}>{card.icon}</span>
                    <span style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--text-1)' }}>
                      {card.title}
                    </span>
                  </div>
                  <span style={{ fontSize: '0.68rem', fontWeight: 700, color: 'var(--text-3)', background: 'var(--bg-surface)', padding: '2px 6px', borderRadius: 4 }}>
                    {card.weight}
                  </span>
                </div>

                <div style={{ fontSize: '0.7rem', color: 'var(--text-3)', marginBottom: 4 }}>
                  {card.input}
                </div>
                <div style={{ fontSize: '0.68rem', color: 'var(--text-3)', fontFamily: 'monospace', marginBottom: 8 }}>
                  Norm: <strong style={{ color: 'var(--text-2)' }}>{card.normalized}</strong>
                </div>

                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', borderTop: '1px solid var(--border)', paddingTop: 8 }}>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>Contribution:</span>
                  <span style={{ fontSize: '0.94rem', fontWeight: 800, color: card.color }}>
                    +{card.points} <span style={{ fontSize: '0.68rem', color: 'var(--text-3)', fontWeight: 500 }}>pts</span>
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Close Button */}
          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <button
              onClick={onClose}
              style={{
                padding: '8px 20px',
                borderRadius: 8,
                background: 'var(--accent)',
                color: '#fff',
                border: 'none',
                fontWeight: 700,
                fontSize: '0.82rem',
                cursor: 'pointer',
              }}
            >
              Close Breakdown
            </button>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
};

export default PriorityFormulaModal;
