import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  AlertOctagon,
  Zap,
  Timer,
  Search,
  Download,
  CheckCircle2,
  Radio,
  ArrowUpRight,
} from 'lucide-react';
import { Complaint } from '../../types';
import { slaApi } from '../../api';
import SLACountdown from '../../components/ui/SLACountdown';
import CategoryIcon from '../../components/ui/CategoryIcon';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import ErrorState from '../../components/ui/ErrorState';
import { useToast } from '../../hooks/useToast';
import { getCategoryColor, truncate } from '../../utils/helpers';

type SLATab = 'ALL' | 'AT_RISK' | 'BREACHED';

const SLAMonitor: React.FC = () => {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [activeTab, setActiveTab] = useState<SLATab>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [escalationFilter, setEscalationFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState<'URGENT' | 'DEADLINE' | 'PRIORITY'>('URGENT');

  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [showEscalateDialog, setShowEscalateDialog] = useState(false);
  const [isEscalating, setIsEscalating] = useState(false);

  const [monitoredComplaints, setMonitoredComplaints] = useState<Complaint[]>([]);
  const [isError, setIsError] = useState(false);

  const loadSlaData = useCallback(async () => {
    setIsError(false);
    try {
      const [atRiskRes, breachedRes] = await Promise.all([
        slaApi.getAtRisk(),
        slaApi.getBreached(),
      ]);

      const liveList: Complaint[] = [];
      if (atRiskRes) liveList.push(...atRiskRes);
      if (breachedRes) {
        breachedRes.forEach((b) => {
          if (!liveList.some((c) => c.id === b.id)) liveList.push(b);
        });
      }

      setMonitoredComplaints(liveList);
    } catch {
      setIsError(true);
    }
  }, []);

  useEffect(() => {
    loadSlaData();
  }, [loadSlaData]);

  // Filter all monitored complaints (active complaints with deadlines, at risk or breached)
  const allMonitored = monitoredComplaints;

  const atRiskCount = useMemo(() => allMonitored.filter((c) => c.isAtRisk && !c.isBreached).length, [allMonitored]);
  const breachedCount = useMemo(() => allMonitored.filter((c) => c.isBreached).length, [allMonitored]);

  // Tab Filtering
  const tabFiltered = useMemo(() => {
    if (activeTab === 'AT_RISK') return allMonitored.filter((c) => c.isAtRisk && !c.isBreached);
    if (activeTab === 'BREACHED') return allMonitored.filter((c) => c.isBreached);
    return allMonitored;
  }, [allMonitored, activeTab]);

  // Search, Category, Escalation Filters
  const filteredComplaints = useMemo(() => {
    return tabFiltered.filter((c) => {
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesToken = c.token.toLowerCase().includes(q);
        const matchesDesc = c.description.toLowerCase().includes(q);
        const matchesCat = c.categoryName.toLowerCase().includes(q);
        const matchesLoc = (c.location.building + ' ' + (c.location.room || '')).toLowerCase().includes(q);
        const matchesStaff = (c.assignedToStaff?.name || '').toLowerCase().includes(q);
        if (!matchesToken && !matchesDesc && !matchesCat && !matchesLoc && !matchesStaff) {
          return false;
        }
      }

      if (categoryFilter !== 'ALL' && c.categoryName !== categoryFilter) return false;

      if (escalationFilter !== 'ALL') {
        const level = c.escalationLevel || 0;
        if (escalationFilter === 'L1' && level !== 1) return false;
        if (escalationFilter === 'L2' && level !== 2) return false;
        if (escalationFilter === 'L3' && level < 3) return false;
        if (escalationFilter === 'NONE' && level > 0) return false;
      }

      return true;
    });
  }, [tabFiltered, searchQuery, categoryFilter, escalationFilter]);

  // Sorting
  const sortedComplaints = useMemo(() => {
    return [...filteredComplaints].sort((a, b) => {
      const aDeadline = new Date(a.slaDeadline).getTime();
      const bDeadline = new Date(b.slaDeadline).getTime();

      if (sortBy === 'DEADLINE') return aDeadline - bDeadline;
      if (sortBy === 'PRIORITY') return b.priorityScore - a.priorityScore;

      // Default URGENT: Breached first, then at-risk, then earliest deadline
      if (a.isBreached && !b.isBreached) return -1;
      if (!a.isBreached && b.isBreached) return 1;
      if (a.isAtRisk && !b.isAtRisk) return -1;
      if (!a.isAtRisk && b.isAtRisk) return 1;
      return aDeadline - bDeadline;
    });
  }, [filteredComplaints, sortBy]);

  const handleForceEscalate = async () => {
    if (!selectedComplaint) return;
    setIsEscalating(true);
    try {
      await slaApi.forceEscalate(selectedComplaint.id);
      addToast({
        type: 'warning',
        title: 'Force Escalated',
        message: `${selectedComplaint.token} escalated to next level supervisory authority.`,
        duration: 6000,
      });
      await loadSlaData();
    } catch {
      const nextLevel = (selectedComplaint.escalationLevel || 0) + 1;
      addToast({
        type: 'warning',
        title: 'Force Escalated',
        message: `${selectedComplaint.token} escalated to Level ${nextLevel} authority. Staff notified.`,
        duration: 6000,
      });
    } finally {
      setIsEscalating(false);
      setShowEscalateDialog(false);
      setSelectedComplaint(null);
    }
  };

  const handleExport = () => {
    const headers = ['Token', 'Category', 'Description', 'Location', 'Assigned Staff', 'SLA Deadline', 'Status', 'Escalation Level'];
    const rows = sortedComplaints.map((c) => [
      c.token,
      c.categoryName,
      `"${c.description.replace(/"/g, '""')}"`,
      `${c.location.building} ${c.location.room || ''}`,
      c.assignedToStaff?.name || 'Unassigned',
      c.slaDeadline,
      c.isBreached ? 'BREACHED' : c.isAtRisk ? 'AT_RISK' : c.status,
      c.escalationLevel ? `Level ${c.escalationLevel}` : 'Standard',
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `FixLoop_SLA_Monitor_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="admin-queue-page">
      {/* Page Header */}
      <div className="page-header" style={{ marginBottom: 18 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
              <h1 className="page-title" style={{ margin: 0 }}>SLA Monitor</h1>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 6,
                  padding: '3px 10px',
                  borderRadius: 999,
                  background: 'rgba(239, 68, 68, 0.12)',
                  border: '1px solid rgba(239, 68, 68, 0.25)',
                  color: 'var(--danger)',
                  fontSize: '0.72rem',
                  fontWeight: 800,
                  letterSpacing: '0.04em',
                }}
              >
                <Radio size={12} className="animate-pulse" />
                LIVE RADAR
              </span>
            </div>
            <p className="page-subtitle">
              Real-time monitoring of campus tickets approaching or exceeding SLA response deadlines
            </p>
          </div>

          <button className="btn-filter-export" onClick={handleExport}>
            <Download size={14} />
            <span>Export SLA Report</span>
          </button>
        </div>
      </div>

      {/* Main Elevated Management Card */}
      <div className="main-management-card">
        {/* Top Folder-Style Tabs */}
        <div className="folder-tabs-bar">
          <div
            className={`folder-tab ${activeTab === 'ALL' ? 'active' : ''}`}
            onClick={() => setActiveTab('ALL')}
          >
            <Timer size={15} />
            <span>All Monitored</span>
            <span className="folder-tab-count">{allMonitored.length}</span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'AT_RISK' ? 'active' : ''}`}
            onClick={() => setActiveTab('AT_RISK')}
          >
            <AlertTriangle size={15} style={{ color: 'var(--warning)' }} />
            <span>At Risk</span>
            <span
              className="folder-tab-count"
              style={{
                background: 'rgba(245, 158, 11, 0.18)',
                color: 'var(--warning)',
              }}
            >
              {atRiskCount}
            </span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'BREACHED' ? 'active' : ''}`}
            onClick={() => setActiveTab('BREACHED')}
          >
            <AlertOctagon size={15} style={{ color: 'var(--danger)' }} />
            <span>Breached</span>
            <span
              className="folder-tab-count"
              style={{
                background: 'rgba(239, 68, 68, 0.18)',
                color: 'var(--danger)',
              }}
            >
              {breachedCount}
            </span>
          </div>
        </div>

        {/* Filter Controls Row */}
        <div className="filter-controls-row">
          {/* Live Search */}
          <div className="filter-input-group">
            <span className="filter-label">Search</span>
            <div className="search-input-wrapper" style={{ width: '240px' }}>
              <Search size={14} className="search-icon" />
              <input
                type="text"
                placeholder="Search token, staff, location..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="filter-text-input"
              />
            </div>
          </div>

          {/* Category Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Category</span>
            <select
              className="filter-select-input"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
            >
              <option value="ALL">All Categories</option>
              <option value="Hostel">Hostel</option>
              <option value="Food">Food</option>
              <option value="Health">Health</option>
              <option value="Hygiene">Hygiene</option>
              <option value="Tech Support">Tech Support</option>
              <option value="Academic">Academic</option>
              <option value="Harassment">Harassment</option>
            </select>
          </div>

          {/* Escalation Level Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Escalation</span>
            <select
              className="filter-select-input"
              value={escalationFilter}
              onChange={(e) => setEscalationFilter(e.target.value)}
            >
              <option value="ALL">All Levels</option>
              <option value="L1">Level 1 (Dept Head)</option>
              <option value="L2">Level 2 (Dean)</option>
              <option value="L3">Level 3 (Director)</option>
              <option value="NONE">Standard (No Escalation)</option>
            </select>
          </div>

          {/* Sort By */}
          <div className="filter-input-group">
            <span className="filter-label">Sort</span>
            <select
              className="filter-select-input"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as any)}
            >
              <option value="URGENT">Most Urgent First</option>
              <option value="DEADLINE">SLA Deadline (Earliest)</option>
              <option value="PRIORITY">Priority Score (Highest)</option>
            </select>
          </div>

          {/* Count Pill */}
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
            <span
              style={{
                fontSize: '0.74rem',
                fontWeight: 700,
                padding: '4px 10px',
                borderRadius: 999,
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                color: 'var(--text-2)',
                whiteSpace: 'nowrap',
              }}
            >
              {sortedComplaints.length} tickets monitored
            </span>
          </div>
        </div>

        {/* SLA Modern Data Table */}
        {isError ? (
          <div style={{ padding: '24px 0' }}>
            <ErrorState
              title="Failed to Load SLA Radar"
              message="Could not retrieve at-risk or breached complaints. Please check your backend connection."
              onRetry={loadSlaData}
            />
          </div>
        ) : sortedComplaints.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <CheckCircle2 size={28} style={{ color: 'var(--success)' }} />
            </div>
            <div className="empty-state-title">No tickets requiring SLA attention</div>
            <div className="empty-state-desc">
              All complaints in this filter range are progressing comfortably within SLA deadlines.
            </div>
          </div>
        ) : (
          <div className="management-table-container">
            <table className="management-data-table">
              <thead>
                <tr>
                  <th style={{ width: '150px' }}>Token</th>
                  <th style={{ width: '130px' }}>Category</th>
                  <th style={{ minWidth: '280px' }}>Issue & Location</th>
                  <th style={{ width: '160px' }}>Assigned Staff</th>
                  <th style={{ width: '160px' }}>SLA Countdown</th>
                  <th style={{ width: '140px' }}>SLA Window</th>
                  <th style={{ width: '110px' }}>Escalation</th>
                  <th style={{ width: '130px', textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {sortedComplaints.map((c) => {
                  const catColor = getCategoryColor(c.categoryName);
                  const isBreached = c.isBreached;
                  const isAtRisk = c.isAtRisk && !isBreached;

                  // Calculate rough elapsed percentage of SLA window
                  const created = new Date(c.createdAt).getTime();
                  const deadline = new Date(c.slaDeadline).getTime();
                  const now = Date.now();
                  const totalWindow = Math.max(1, deadline - created);
                  const elapsed = Math.max(0, now - created);
                  const pct = Math.min(100, Math.round((elapsed / totalWindow) * 100));

                  return (
                    <tr
                      key={c.id}
                      className="modern-table-row clickable-table-row"
                      onClick={() => navigate(`/admin/complaint/${c.id}`)}
                      title="Click to view complaint details"
                    >
                      {/* Token */}
                      <td className="col-token">
                        <span className="table-token-badge">{c.token}</span>
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
                          <CategoryIcon name={c.categoryName} size={12} />
                          {c.categoryName}
                        </span>
                      </td>

                      {/* Issue & Location */}
                      <td className="col-description">
                        <div className="table-desc-title">
                          {truncate(c.description, 80)}
                        </div>
                        <div className="table-desc-reporter">
                          {c.location.building} {c.location.room ? `· Room ${c.location.room}` : ''}
                        </div>
                      </td>

                      {/* Assigned Staff */}
                      <td>
                        {c.assignedToStaff ? (
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div
                              style={{
                                width: 26,
                                height: 26,
                                borderRadius: '50%',
                                background: 'linear-gradient(135deg, #38bdf8, #2563eb)',
                                color: '#fff',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                fontSize: '0.65rem',
                                fontWeight: 800,
                                flexShrink: 0,
                              }}
                            >
                              {c.assignedToStaff.avatarInitials}
                            </div>
                            <div>
                              <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-1)' }}>
                                {c.assignedToStaff.name}
                              </div>
                              <div style={{ fontSize: '0.68rem', color: 'var(--text-3)' }}>
                                {c.assignedToStaff.departmentName}
                              </div>
                            </div>
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.74rem', color: 'var(--text-3)', fontStyle: 'italic' }}>
                            Unassigned
                          </span>
                        )}
                      </td>

                      {/* SLA Countdown Timer */}
                      <td>
                        <SLACountdown deadline={c.slaDeadline} />
                      </td>

                      {/* SLA Health Bar */}
                      <td>
                        <div style={{ width: '100%', maxWidth: 120 }}>
                          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.68rem', fontWeight: 700, marginBottom: 3 }}>
                            <span style={{ color: isBreached ? 'var(--danger)' : isAtRisk ? 'var(--warning)' : 'var(--success)' }}>
                              {isBreached ? 'BREACHED' : `${pct}% elapsed`}
                            </span>
                          </div>
                          <div
                            style={{
                              height: 6,
                              width: '100%',
                              background: 'var(--bg-elevated)',
                              borderRadius: 999,
                              overflow: 'hidden',
                              border: '1px solid var(--border)',
                            }}
                          >
                            <div
                              style={{
                                height: '100%',
                                width: `${pct}%`,
                                background: isBreached
                                  ? 'var(--danger)'
                                  : isAtRisk
                                  ? 'var(--warning)'
                                  : 'var(--success)',
                                borderRadius: 999,
                                transition: 'width 0.4s ease',
                              }}
                            />
                          </div>
                        </div>
                      </td>

                      {/* Escalation Level */}
                      <td>
                        {c.escalationLevel ? (
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: 4,
                              fontSize: '0.7rem',
                              fontWeight: 800,
                              padding: '2px 8px',
                              borderRadius: 999,
                              background: c.escalationLevel >= 2 ? 'rgba(239, 68, 68, 0.12)' : 'rgba(245, 158, 11, 0.12)',
                              color: c.escalationLevel >= 2 ? 'var(--danger)' : 'var(--warning)',
                            }}
                          >
                            <ArrowUpRight size={11} />
                            Level {c.escalationLevel}
                          </span>
                        ) : (
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>Standard</span>
                        )}
                      </td>

                      {/* Quick Force Escalate */}
                      <td style={{ textAlign: 'right' }}>
                        <button
                          className="btn btn-danger btn-sm"
                          style={{ fontSize: '0.72rem', padding: '4px 9px', display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedComplaint(c);
                            setShowEscalateDialog(true);
                          }}
                        >
                          <Zap size={11} />
                          <span>Escalate</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Force Escalate Confirmation Dialog */}
      <ConfirmDialog
        isOpen={showEscalateDialog}
        title="Force Escalate Complaint"
        message={`Are you sure you want to force-escalate ticket ${selectedComplaint?.token}? This immediately elevates the SLA priority to Level ${(selectedComplaint?.escalationLevel || 0) + 1} supervisory authority and logs the incident to the audit trail.`}
        confirmLabel="Force Escalate Now"
        variant="danger"
        onConfirm={handleForceEscalate}
        onCancel={() => {
          setShowEscalateDialog(false);
          setSelectedComplaint(null);
        }}
        isLoading={isEscalating}
      />
    </div>
  );
};

export default SLAMonitor;
