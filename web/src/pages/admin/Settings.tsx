import React, { useState } from 'react';
import {
  Clock,
  Sliders,
  Tag,
  Save,
  ShieldAlert,
  CheckCircle2,
  AlertTriangle,
  Zap,
  Percent,
} from 'lucide-react';
import { mockCategories, mockSLARules } from '../../data/mockData';
import { SLARule } from '../../types';
import { useToast } from '../../hooks/useToast';
import CategoryIcon from '../../components/ui/CategoryIcon';
import { getCategoryColor } from '../../utils/helpers';
import { calculatePriorityScore, DEFAULT_PRIORITY_WEIGHTS } from '../../utils/priority';

const Settings: React.FC = () => {
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState<'sla' | 'categories' | 'weights'>('sla');
  const [slaRules, setSlaRules] = useState<SLARule[]>(mockSLARules);
  const [weights, setWeights] = useState({
    w1: DEFAULT_PRIORITY_WEIGHTS.w1, // 0.48
    w2: DEFAULT_PRIORITY_WEIGHTS.w2, // 0.21
    w3: DEFAULT_PRIORITY_WEIGHTS.w3, // 0.08
    w4: DEFAULT_PRIORITY_WEIGHTS.w4, // 0.23
  });

  // Simulated Case: Severity 4, 10 affected students, 5 hours elapsed, SLA 24h, Recurrence 2
  const sampleCalc = calculatePriorityScore(
    {
      severity: 4,
      studentsAffected: 10,
      recurrenceCountLast90Days: 2,
      hoursSinceCreated: 5,
      slaHours: 24,
    },
    weights
  );

  const handleSLAChange = (
    id: string,
    field: 'resolutionHours' | 'escalationHours',
    val: number
  ) => {
    setSlaRules((prev) =>
      prev.map((r) => (r.id === id ? { ...r, [field]: val } : r))
    );
  };

  const handleSave = () => {
    addToast({
      type: 'success',
      title: 'Settings saved',
      message: 'Dynamic priority scoring weights and SLA configuration updated',
    });
  };

  const WeightSlider = ({
    label,
    weightKey,
    description,
    multiplierCode,
  }: {
    label: string;
    weightKey: 'w1' | 'w2' | 'w3' | 'w4';
    description: string;
    multiplierCode: string;
  }) => (
    <div
      style={{
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: '16px 18px',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: 8,
        }}
      >
        <div>
          <div style={{ fontWeight: 700, fontSize: '0.86rem', color: 'var(--text-1)', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span>{label}</span>
            <code style={{ fontSize: '0.72rem', background: 'var(--bg-surface)', padding: '2px 6px', borderRadius: 4, color: 'var(--accent)' }}>
              {multiplierCode}
            </code>
          </div>
          <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginTop: 2 }}>
            {description}
          </div>
        </div>
        <span
          style={{
            fontSize: '1.1rem',
            fontWeight: 800,
            color: 'var(--accent)',
            minWidth: 70,
            textAlign: 'right',
            fontFamily: 'monospace',
          }}
        >
          {(weights[weightKey] * 100).toFixed(0)}%
          <span style={{ fontSize: '0.74rem', color: 'var(--text-3)', fontWeight: 500, marginLeft: 4 }}>
            ({weights[weightKey].toFixed(2)})
          </span>
        </span>
      </div>
      <input
        type="range"
        min="0"
        max="1"
        step="0.01"
        value={weights[weightKey]}
        onChange={(e) =>
          setWeights((w) => ({ ...w, [weightKey]: parseFloat(e.target.value) }))
        }
        style={{ width: '100%', accentColor: 'var(--accent)', cursor: 'pointer' }}
      />
    </div>
  );

  return (
    <div className="admin-queue-page">
      {/* Page Header */}
      <div className="page-header">
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <h1 className="page-title">System & SLA Settings</h1>
            <p className="page-subtitle">
              Configure SLA resolution targets, complaint category rules, and priority calculation weights
            </p>
          </div>
          <button className="btn btn-primary" onClick={handleSave}>
            <Save size={15} />
            <span>Save All Settings</span>
          </button>
        </div>
      </div>

      {/* Main Elevated Card Container */}
      <div className="main-management-card">
        {/* Top Folder Tabs */}
        <div className="folder-tabs-bar">
          <div
            className={`folder-tab ${activeTab === 'sla' ? 'active' : ''}`}
            onClick={() => setActiveTab('sla')}
          >
            <Clock size={15} />
            <span>SLA Deadlines</span>
            <span className="folder-tab-badge">{slaRules.length}</span>
          </div>
          <div
            className={`folder-tab ${activeTab === 'categories' ? 'active' : ''}`}
            onClick={() => setActiveTab('categories')}
          >
            <Tag size={15} />
            <span>Category Rules</span>
            <span className="folder-tab-badge">{mockCategories.length}</span>
          </div>
          <div
            className={`folder-tab ${activeTab === 'weights' ? 'active' : ''}`}
            onClick={() => setActiveTab('weights')}
          >
            <Sliders size={15} />
            <span>Priority Algorithm</span>
          </div>
        </div>

        {/* Tab 1: SLA Rules */}
        {activeTab === 'sla' && (
          <div>
            <div className="management-table-container">
              <table className="management-data-table">
                <thead>
                  <tr>
                    <th style={{ width: '220px' }}>Category</th>
                    <th style={{ width: '160px' }}>Severity Tier</th>
                    <th style={{ width: '200px' }}>Resolution Target</th>
                    <th style={{ width: '200px' }}>Escalation Threshold</th>
                    <th>Auto-Action</th>
                  </tr>
                </thead>
                <tbody>
                  {slaRules.map((rule) => {
                    const catColor = getCategoryColor(rule.categoryName);
                    return (
                      <tr key={rule.id} className="modern-table-row">
                        {/* Category */}
                        <td>
                          <span
                            className="table-category-pill"
                            style={{
                              borderColor: `${catColor}30`,
                              color: catColor,
                              background: `${catColor}12`,
                            }}
                          >
                            <CategoryIcon name={rule.categoryName} size={13} />
                            {rule.categoryName}
                          </span>
                        </td>

                        {/* Severity */}
                        <td>
                          <span
                            style={{
                              fontSize: '0.72rem',
                              fontWeight: 800,
                              padding: '3px 10px',
                              borderRadius: 999,
                              background:
                                rule.severityTier === 'HIGH'
                                  ? 'rgba(239, 68, 68, 0.15)'
                                  : rule.severityTier === 'MEDIUM'
                                  ? 'rgba(245, 158, 11, 0.15)'
                                  : 'rgba(34, 197, 94, 0.15)',
                              color:
                                rule.severityTier === 'HIGH'
                                  ? '#f87171'
                                  : rule.severityTier === 'MEDIUM'
                                  ? '#fbbf24'
                                  : '#4ade80',
                            }}
                          >
                            {rule.severityTier}
                          </span>
                        </td>

                        {/* Resolution Hours */}
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input
                              type="number"
                              min="1"
                              max="168"
                              value={rule.resolutionHours}
                              onChange={(e) =>
                                handleSLAChange(
                                  rule.id,
                                  'resolutionHours',
                                  parseInt(e.target.value) || 1
                                )
                              }
                              style={{
                                width: 75,
                                padding: '6px 10px',
                                background: 'var(--bg-elevated)',
                                border: '1px solid var(--border)',
                                borderRadius: 8,
                                color: 'var(--text-1)',
                                fontSize: '0.84rem',
                                fontWeight: 700,
                              }}
                            />
                            <span style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>
                              hours
                            </span>
                          </div>
                        </td>

                        {/* Escalation Hours */}
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <input
                              type="number"
                              min="1"
                              max="240"
                              value={rule.escalationHours}
                              onChange={(e) =>
                                handleSLAChange(
                                  rule.id,
                                  'escalationHours',
                                  parseInt(e.target.value) || 1
                                )
                              }
                              style={{
                                width: 75,
                                padding: '6px 10px',
                                background: 'var(--bg-elevated)',
                                border: '1px solid var(--border)',
                                borderRadius: 8,
                                color: 'var(--text-1)',
                                fontSize: '0.84rem',
                                fontWeight: 700,
                              }}
                            />
                            <span style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>
                              hours
                            </span>
                          </div>
                        </td>

                        {/* Auto-Action */}
                        <td style={{ fontSize: '0.78rem', color: 'var(--text-2)' }}>
                          Auto-escalates to Department Head at 80% SLA window
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Tab 2: Categories */}
        {activeTab === 'categories' && (
          <div>
            <div className="management-table-container">
              <table className="management-data-table">
                <thead>
                  <tr>
                    <th style={{ width: '220px' }}>Category</th>
                    <th style={{ width: '160px' }}>Base Severity</th>
                    <th style={{ width: '130px' }}>Default SLA</th>
                    <th style={{ minWidth: '220px' }}>Required Skills</th>
                    <th style={{ width: '140px' }}>Restricted / Sensitive</th>
                  </tr>
                </thead>
                <tbody>
                  {mockCategories.map((cat) => {
                    const catColor = getCategoryColor(cat.name);
                    return (
                      <tr key={cat.id} className="modern-table-row">
                        {/* Name */}
                        <td>
                          <span
                            className="table-category-pill"
                            style={{
                              borderColor: `${catColor}30`,
                              color: catColor,
                              background: `${catColor}12`,
                            }}
                          >
                            <CategoryIcon name={cat.name} size={14} />
                            {cat.name}
                          </span>
                        </td>

                        {/* Base Severity Dots */}
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                            {Array.from({ length: 5 }).map((_, i) => (
                              <div
                                key={i}
                                style={{
                                  width: 9,
                                  height: 9,
                                  borderRadius: '50%',
                                  background:
                                    i < cat.baseSeverityWeight ? cat.color : 'var(--border)',
                                }}
                              />
                            ))}
                            <span
                              style={{
                                marginLeft: 6,
                                fontSize: '0.75rem',
                                fontWeight: 700,
                                color: 'var(--text-3)',
                              }}
                            >
                              {cat.baseSeverityWeight}/5
                            </span>
                          </div>
                        </td>

                        {/* SLA */}
                        <td style={{ fontWeight: 700, color: 'var(--text-1)' }}>
                          {cat.slaHours}h
                        </td>

                        {/* Skills */}
                        <td>
                          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                            {cat.requiredSkillTags.map((tag) => (
                              <span
                                key={tag}
                                style={{
                                  fontSize: '0.68rem',
                                  fontWeight: 600,
                                  padding: '2px 8px',
                                  background: 'var(--bg-elevated)',
                                  border: '1px solid var(--border)',
                                  borderRadius: 999,
                                  color: 'var(--text-2)',
                                }}
                              >
                                {tag}
                              </span>
                            ))}
                          </div>
                        </td>

                        {/* Sensitive Flag */}
                        <td>
                          {cat.sensitive ? (
                            <span
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 4,
                                fontSize: '0.74rem',
                                fontWeight: 700,
                                color: '#ec4899',
                                background: 'rgba(236, 72, 153, 0.12)',
                                padding: '2px 8px',
                                borderRadius: 999,
                              }}
                            >
                              <ShieldAlert size={12} />
                              Restricted
                            </span>
                          ) : (
                            <span
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: 4,
                                fontSize: '0.74rem',
                                fontWeight: 700,
                                color: 'var(--success)',
                              }}
                            >
                              <CheckCircle2 size={12} />
                              Standard
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Tab 3: Priority Weights Algorithm */}
        {activeTab === 'weights' && (
          <div style={{ padding: '24px 28px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 28 }}>
              {/* Sliders Column */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div>
                  <h3 style={{ fontSize: '0.96rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 4 }}>
                    Dynamic Priority Scoring Formula
                  </h3>
                  <p style={{ fontSize: '0.78rem', color: 'var(--text-3)' }}>
                    Multi-factor normalized model balancing severity, student population reach, campus recurrence, and SLA urgency.
                  </p>
                </div>

                <div
                  style={{
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                    padding: '16px 20px',
                    fontFamily: 'JetBrains Mono, monospace',
                    fontSize: '0.78rem',
                    color: 'var(--text-2)',
                    lineHeight: 1.7,
                  }}
                >
                  <div style={{ color: 'var(--text-3)', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>
                    {"// 1. Component Normalization"}
                  </div>
                  <span style={{ color: '#3b82f6' }}>severity_norm</span>&nbsp;&nbsp;&nbsp;&nbsp;= severity / 5<br />
                  <span style={{ color: '#6366f1' }}>scale_norm</span>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;= ln(1 + students_affected) / ln(1 + 500)&nbsp;&nbsp;<span style={{ color: 'var(--text-3)' }}>{"// cap 500"}</span><br />
                  <span style={{ color: '#22c55e' }}>recurrence_norm</span>&nbsp;&nbsp;= min(recurrence_count_last_90_days, 10) / 10<br />
                  <span style={{ color: '#f59e0b' }}>time_urgency_norm</span>= min(hours_since_created / sla_hours, 1.0)<br /><br />
                  <div style={{ color: 'var(--text-3)', fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', marginBottom: 4 }}>
                    {"// 2. Weighted Priority Score"}
                  </div>
                  <span style={{ color: 'var(--accent)', fontWeight: 700 }}>priority_score</span> =<br />
                  &nbsp;&nbsp;({weights.w1.toFixed(2)} × severity_norm)<br />
                  &nbsp;&nbsp;+ ({weights.w2.toFixed(2)} × scale_norm)<br />
                  &nbsp;&nbsp;+ ({weights.w3.toFixed(2)} × recurrence_norm)<br />
                  &nbsp;&nbsp;+ ({weights.w4.toFixed(2)} × time_urgency_norm)
                </div>

                {/* Weight Balance Strip */}
                {(() => {
                  const total = weights.w1 + weights.w2 + weights.w3 + weights.w4;
                  const isBalanced = Math.abs(total - 1.0) < 0.01;
                  return (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        background: isBalanced ? 'rgba(34, 197, 94, 0.08)' : 'rgba(245, 158, 11, 0.08)',
                        border: `1px solid ${isBalanced ? 'rgba(34, 197, 94, 0.25)' : 'rgba(245, 158, 11, 0.25)'}`,
                        borderRadius: 10,
                        fontSize: '0.78rem',
                      }}
                    >
                      <span style={{ color: 'var(--text-2)', display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Percent size={14} style={{ color: isBalanced ? 'var(--success)' : 'var(--warning)' }} />
                        Total Normalized Weights:
                      </span>
                      <span style={{ fontWeight: 800, color: isBalanced ? 'var(--success)' : 'var(--warning)' }}>
                        {(total * 100).toFixed(0)}% ({total.toFixed(2)}) {isBalanced ? '✓ 100% Balanced' : '⚠️ Deviates from 1.00'}
                      </span>
                    </div>
                  );
                })()}

                <WeightSlider
                  weightKey="w1"
                  label="W1 — Severity Weight"
                  multiplierCode="severity / 5"
                  description="Base complaint severity on 1 to 5 scale (default 0.48 / 48%)"
                />
                <WeightSlider
                  weightKey="w2"
                  label="W2 — Population Reach Weight"
                  multiplierCode="ln(1+N) / ln(501)"
                  description="Logarithmic impact scale capped at 500 affected students (default 0.21 / 21%)"
                />
                <WeightSlider
                  weightKey="w3"
                  label="W3 — Recurrence Weight"
                  multiplierCode="min(rec, 10) / 10"
                  description="Unresolved recurring cluster incidents in last 90 days (default 0.08 / 8%)"
                />
                <WeightSlider
                  weightKey="w4"
                  label="W4 — Time Urgency Weight"
                  multiplierCode="min(elapsed / SLA, 1.0)"
                  description="Progressive SLA window consumption urgency factor (default 0.23 / 23%)"
                />
              </div>

              {/* Real-Time Live Preview */}
              <div>
                <h3 style={{ fontSize: '0.96rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 14 }}>
                  Real-Time Calculation Simulation
                </h3>

                <div
                  style={{
                    background: 'var(--bg-surface)',
                    border: '1px solid var(--border-strong)',
                    borderRadius: 18,
                    padding: 24,
                    boxShadow: 'var(--card-shadow)',
                  }}
                >
                  <div style={{ fontSize: '0.76rem', color: 'var(--text-3)', marginBottom: 16 }}>
                    Simulated Case: Severity 4/5, 10 affected students, 5h elapsed, SLA 24h, Recurrence 2
                  </div>

                  <div style={{ textAlign: 'center', padding: '16px 0 20px 0' }}>
                    <div
                      style={{
                        fontSize: '3.4rem',
                        fontWeight: 900,
                        color:
                          sampleCalc.scaledScore >= 80
                            ? '#ef4444'
                            : sampleCalc.scaledScore >= 60
                            ? '#f97316'
                            : sampleCalc.scaledScore >= 35
                            ? '#f59e0b'
                            : '#22c55e',
                        letterSpacing: '-0.04em',
                      }}
                    >
                      {sampleCalc.scaledScore.toFixed(1)}
                    </div>
                    <div style={{ fontSize: '0.76rem', color: 'var(--text-3)', marginBottom: 8 }}>
                      Raw Formula Output: <code style={{ color: 'var(--accent)', fontWeight: 700 }}>{sampleCalc.rawScore.toFixed(4)}</code> (0.0 to 1.0)
                    </div>
                    <div
                      style={{
                        fontSize: '0.82rem',
                        fontWeight: 800,
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '4px 12px',
                        borderRadius: 999,
                        background:
                          sampleCalc.scaledScore >= 80
                            ? 'rgba(239, 68, 68, 0.12)'
                            : sampleCalc.scaledScore >= 60
                            ? 'rgba(249, 115, 22, 0.12)'
                            : sampleCalc.scaledScore >= 35
                            ? 'rgba(245, 158, 11, 0.12)'
                            : 'rgba(34, 197, 94, 0.12)',
                        color:
                          sampleCalc.scaledScore >= 80
                            ? '#ef4444'
                            : sampleCalc.scaledScore >= 60
                            ? '#f97316'
                            : sampleCalc.scaledScore >= 35
                            ? '#f59e0b'
                            : '#22c55e',
                      }}
                    >
                      {sampleCalc.scaledScore >= 80 ? (
                        <>
                          <AlertTriangle size={14} />
                          <span>CRITICAL PRIORITY TICKET</span>
                        </>
                      ) : sampleCalc.scaledScore >= 60 ? (
                        <>
                          <AlertTriangle size={14} />
                          <span>HIGH PRIORITY TICKET</span>
                        </>
                      ) : sampleCalc.scaledScore >= 35 ? (
                        <>
                          <Zap size={14} />
                          <span>MEDIUM PRIORITY DISPATCH</span>
                        </>
                      ) : (
                        <>
                          <CheckCircle2 size={14} />
                          <span>LOW PRIORITY QUEUE</span>
                        </>
                      )}
                    </div>
                  </div>

                  <div
                    style={{
                      display: 'grid',
                      gridTemplateColumns: '1fr 1fr',
                      gap: 10,
                      marginTop: 16,
                    }}
                  >
                    {[
                      {
                        title: 'Severity Norm',
                        formula: '4 / 5 = 0.80',
                        weightText: `${(weights.w1 * 100).toFixed(0)}% × 0.80`,
                        value: sampleCalc.severityComponent.toFixed(3),
                        color: '#3b82f6',
                      },
                      {
                        title: 'Scale Norm',
                        formula: 'ln(11)/ln(501) = 0.386',
                        weightText: `${(weights.w2 * 100).toFixed(0)}% × 0.386`,
                        value: sampleCalc.scaleComponent.toFixed(3),
                        color: '#6366f1',
                      },
                      {
                        title: 'Recurrence Norm',
                        formula: 'min(2, 10)/10 = 0.20',
                        weightText: `${(weights.w3 * 100).toFixed(0)}% × 0.20`,
                        value: sampleCalc.recurrenceComponent.toFixed(3),
                        color: '#22c55e',
                      },
                      {
                        title: 'Time Urgency Norm',
                        formula: 'min(5/24, 1) = 0.208',
                        weightText: `${(weights.w4 * 100).toFixed(0)}% × 0.208`,
                        value: sampleCalc.timeUrgencyComponent.toFixed(3),
                        color: '#f59e0b',
                      },
                    ].map((row) => (
                      <div
                        key={row.title}
                        style={{
                          background: 'var(--bg-elevated)',
                          padding: '10px 14px',
                          borderRadius: 10,
                          border: '1px solid var(--border)',
                        }}
                      >
                        <div
                          style={{
                            fontSize: '0.7rem',
                            color: 'var(--text-3)',
                            marginBottom: 2,
                            fontWeight: 600,
                          }}
                        >
                          {row.title}
                        </div>
                        <div style={{ fontSize: '0.66rem', color: 'var(--text-3)', fontFamily: 'monospace' }}>
                          {row.formula}
                        </div>
                        <div
                          style={{
                            display: 'flex',
                            alignItems: 'baseline',
                            justifyContent: 'space-between',
                            marginTop: 4,
                          }}
                        >
                          <span style={{ fontSize: '0.67rem', color: 'var(--text-2)' }}>
                            {row.weightText}
                          </span>
                          <span
                            style={{
                              fontWeight: 800,
                              fontSize: '0.96rem',
                              color: row.color,
                            }}
                          >
                            +{row.value}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Settings;
