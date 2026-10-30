import React, { useState, useMemo, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  ShieldAlert,
  Shield,
  Lock,
  Search,
  Download,
  AlertTriangle,
  EyeOff,
  User,
  FileCheck2,
} from 'lucide-react';
import { Complaint } from '../../types';
import { complaintApi } from '../../api';
import StatusBadge from '../../components/ui/StatusBadge';
import PriorityIndicator from '../../components/ui/PriorityIndicator';
import ErrorState from '../../components/ui/ErrorState';
import { truncate } from '../../utils/helpers';

type RestrictedTab = 'ALL' | 'RAGGING' | 'HARASSMENT' | 'MALPRACTICE';

const RestrictedComplaints: React.FC = () => {
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState<RestrictedTab>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [anonymityFilter, setAnonymityFilter] = useState<'ALL' | 'ANONYMOUS' | 'DISCLOSED'>('ALL');
  const [sortBy, setSortBy] = useState<'PRIORITY' | 'DATE_DESC'>('PRIORITY');
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [isError, setIsError] = useState(false);

  const fetchRestricted = async () => {
    setIsError(false);
    try {
      const res = await complaintApi.getAdminQueue({ restricted: true });
      setComplaints(res.content || []);
    } catch {
      setIsError(true);
    }
  };

  useEffect(() => {
    fetchRestricted();
  }, []);

  const baseRestricted = complaints;

  const raggingCount = useMemo(
    () => baseRestricted.filter((c) => c.categoryName === 'Ragging').length,
    [baseRestricted]
  );
  const harassmentCount = useMemo(
    () => baseRestricted.filter((c) => c.categoryName === 'Harassment').length,
    [baseRestricted]
  );
  const malpracticeCount = useMemo(
    () => baseRestricted.filter((c) => c.categoryName === 'Malpractice').length,
    [baseRestricted]
  );

  // Tab Filtering
  const tabFiltered = useMemo(() => {
    if (activeTab === 'RAGGING') return baseRestricted.filter((c) => c.categoryName === 'Ragging');
    if (activeTab === 'HARASSMENT') return baseRestricted.filter((c) => c.categoryName === 'Harassment');
    if (activeTab === 'MALPRACTICE') return baseRestricted.filter((c) => c.categoryName === 'Malpractice');
    return baseRestricted;
  }, [baseRestricted, activeTab]);

  // Search & Filters
  const filteredComplaints = useMemo(() => {
    return tabFiltered.filter((c) => {
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesToken = c.token.toLowerCase().includes(q);
        const matchesDesc = c.description.toLowerCase().includes(q);
        const matchesLoc = (c.location.building + ' ' + (c.location.room || '')).toLowerCase().includes(q);
        const matchesStaff = (c.assignedToStaff?.name || '').toLowerCase().includes(q);
        const matchesStudent = (c.student?.name || '').toLowerCase().includes(q);
        if (!matchesToken && !matchesDesc && !matchesLoc && !matchesStaff && !matchesStudent) {
          return false;
        }
      }

      if (statusFilter !== 'ALL' && c.status !== statusFilter) return false;

      if (anonymityFilter === 'ANONYMOUS' && !c.isAnonymous) return false;
      if (anonymityFilter === 'DISCLOSED' && c.isAnonymous) return false;

      return true;
    });
  }, [tabFiltered, searchQuery, statusFilter, anonymityFilter]);

  // Sorting
  const sortedComplaints = useMemo(() => {
    return [...filteredComplaints].sort((a, b) => {
      if (sortBy === 'PRIORITY') return b.priorityScore - a.priorityScore;
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });
  }, [filteredComplaints, sortBy]);

  const handleExport = () => {
    const headers = ['Token', 'Classification', 'Description', 'Building', 'Location', 'Reporter Privacy', 'Status', 'Assigned Officer'];
    const rows = sortedComplaints.map((c) => [
      c.token,
      c.categoryName,
      `"${c.description.replace(/"/g, '""')}"`,
      c.location.building,
      c.location.room || '',
      c.isAnonymous ? 'Protected Whistleblower (Anonymous)' : c.student?.name || 'Disclosed',
      c.status,
      c.assignedToStaff?.name || 'Assigned to Committee',
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `FixLoop_Restricted_Complaints_${Date.now()}.csv`);
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
              <h1 className="page-title" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
                <ShieldAlert size={24} style={{ color: 'var(--danger)' }} />
                Restricted Complaints
              </h1>
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
                <Lock size={11} />
                CONFIDENTIAL CLEARANCE
              </span>
            </div>
            <p className="page-subtitle">
              Sensitive grievances regarding Anti-Ragging, Harassment, and Malpractice — protected identity and strictly audited access
            </p>
          </div>

          <button className="btn-filter-export" onClick={handleExport}>
            <Download size={14} />
            <span>Export Audited Log</span>
          </button>
        </div>
      </div>

      {/* Main Elevated Management Card */}
      <div className="main-management-card">
        {/* Security Warning Callout Banner */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: '11px 18px',
            background: 'var(--bg-elevated)',
            borderBottom: '1px solid var(--border)',
            fontSize: '0.78rem',
            color: 'var(--text-2)',
          }}
        >
          <Lock size={15} style={{ color: 'var(--danger)', flexShrink: 0 }} />
          <span>
            <strong style={{ color: 'var(--text-1)' }}>Protected Vault:</strong> All investigations in this area are sealed. Whistleblower identities remain encrypted unless officially de-anonymized by the disciplinary committee.
          </span>
          <span
            style={{
              marginLeft: 'auto',
              fontSize: '0.7rem',
              fontWeight: 700,
              color: 'var(--text-3)',
              whiteSpace: 'nowrap',
            }}
          >
            Audit ID: FXL-SEC-2026
          </span>
        </div>

        {/* Top Folder-Style Tabs */}
        <div className="folder-tabs-bar">
          <div
            className={`folder-tab ${activeTab === 'ALL' ? 'active' : ''}`}
            onClick={() => setActiveTab('ALL')}
          >
            <Shield size={15} />
            <span>All Restricted</span>
            <span className="folder-tab-count">{baseRestricted.length}</span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'RAGGING' ? 'active' : ''}`}
            onClick={() => setActiveTab('RAGGING')}
          >
            <ShieldAlert size={15} style={{ color: '#ef4444' }} />
            <span>Anti-Ragging</span>
            <span
              className="folder-tab-count"
              style={{
                background: 'rgba(239, 68, 68, 0.18)',
                color: '#ef4444',
              }}
            >
              {raggingCount}
            </span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'HARASSMENT' ? 'active' : ''}`}
            onClick={() => setActiveTab('HARASSMENT')}
          >
            <Lock size={15} style={{ color: '#ec4899' }} />
            <span>Harassment</span>
            <span
              className="folder-tab-count"
              style={{
                background: 'rgba(236, 72, 153, 0.18)',
                color: '#ec4899',
              }}
            >
              {harassmentCount}
            </span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'MALPRACTICE' ? 'active' : ''}`}
            onClick={() => setActiveTab('MALPRACTICE')}
          >
            <AlertTriangle size={15} style={{ color: 'var(--orange)' }} />
            <span>Malpractice</span>
            <span
              className="folder-tab-count"
              style={{
                background: 'rgba(249, 115, 22, 0.18)',
                color: 'var(--orange)',
              }}
            >
              {malpracticeCount}
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
                placeholder="Search confidential token..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="filter-text-input"
              />
            </div>
          </div>

          {/* Status Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Status</span>
            <select
              className="filter-select-input"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="UNDER_ADMIN_REVIEW">Under Review</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
            </select>
          </div>

          {/* Identity / Anonymity Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Reporter</span>
            <select
              className="filter-select-input"
              value={anonymityFilter}
              onChange={(e) => setAnonymityFilter(e.target.value as any)}
            >
              <option value="ALL">All Records</option>
              <option value="ANONYMOUS">Anonymous Whistleblower</option>
              <option value="DISCLOSED">Named Reporter</option>
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
              <option value="PRIORITY">Priority Score (Highest)</option>
              <option value="DATE_DESC">Reported (Newest First)</option>
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
              {sortedComplaints.length} confidential files
            </span>
          </div>
        </div>

        {/* Restricted Data Table */}
        {isError ? (
          <div style={{ padding: '24px 0' }}>
            <ErrorState
              title="Failed to Load Restricted Complaints"
              message="Could not fetch restricted complaints from the server. Please verify your clearance and network connection."
              onRetry={fetchRestricted}
            />
          </div>
        ) : sortedComplaints.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <FileCheck2 size={28} style={{ color: 'var(--success)' }} />
            </div>
            <div className="empty-state-title">No restricted complaints found</div>
            <div className="empty-state-desc">
              No anti-ragging, harassment, or malpractice complaints match your current filter selections.
            </div>
          </div>
        ) : (
          <div className="management-table-container">
            <table className="management-data-table">
              <thead>
                <tr>
                  <th style={{ width: '160px' }}>Case Token</th>
                  <th style={{ width: '140px' }}>Classification</th>
                  <th style={{ minWidth: '300px' }}>Incident Summary</th>
                  <th style={{ width: '160px' }}>Location</th>
                  <th style={{ width: '180px' }}>Reporter Identity</th>
                  <th style={{ width: '110px' }}>Priority</th>
                  <th style={{ width: '120px' }}>Status</th>
                  <th style={{ width: '140px' }}>Lead Officer</th>
                </tr>
              </thead>
              <tbody>
                {sortedComplaints.map((c) => {
                  const isHarassment = c.categoryName === 'Harassment';
                  const isRagging = c.categoryName === 'Ragging';
                  const badgeColor = isRagging ? '#ef4444' : isHarassment ? '#ec4899' : 'var(--orange)';
                  const badgeBg = isRagging ? 'rgba(239, 68, 68, 0.12)' : isHarassment ? 'rgba(236, 72, 153, 0.12)' : 'rgba(249, 115, 22, 0.12)';
                  const badgeBorder = isRagging ? 'rgba(239, 68, 68, 0.3)' : isHarassment ? 'rgba(236, 72, 153, 0.3)' : 'rgba(249, 115, 22, 0.3)';

                  return (
                    <tr
                      key={c.id}
                      className="modern-table-row clickable-table-row"
                      onClick={() => navigate(`/admin/complaint/${c.id}`)}
                      title="Click to access restricted case file"
                    >
                      {/* Token with Lock */}
                      <td className="col-token">
                        <span
                          className="table-token-badge"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 5,
                            border: '1px solid rgba(239, 68, 68, 0.25)',
                            background: 'rgba(239, 68, 68, 0.08)',
                            color: 'var(--danger)',
                          }}
                        >
                          <Lock size={11} />
                          {c.token}
                        </span>
                      </td>

                      {/* Classification Badge */}
                      <td>
                        <span
                          className="table-category-pill"
                          style={{
                            background: badgeBg,
                            borderColor: badgeBorder,
                            color: badgeColor,
                            fontWeight: 700,
                          }}
                        >
                          {isRagging ? <ShieldAlert size={11} /> : isHarassment ? <Lock size={11} /> : <AlertTriangle size={11} />}
                          {c.categoryName}
                        </span>
                      </td>

                      {/* Incident Summary */}
                      <td className="col-description">
                        <div className="table-desc-title">
                          {truncate(c.description, 85)}
                        </div>
                        <div className="table-desc-reporter" style={{ color: 'var(--text-3)' }}>
                          Reported on {new Date(c.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                        </div>
                      </td>

                      {/* Location */}
                      <td className="col-location">
                        <div className="table-location-text">{c.location.building}</div>
                        <div className="table-location-sub">
                          {c.location.room || c.location.floor || 'Confidential Venue'}
                        </div>
                      </td>

                      {/* Reporter Identity */}
                      <td>
                        {c.isAnonymous ? (
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: 5,
                              fontSize: '0.72rem',
                              fontWeight: 700,
                              padding: '3px 8px',
                              borderRadius: 999,
                              background: 'var(--bg-elevated)',
                              border: '1px solid var(--border)',
                              color: 'var(--text-2)',
                            }}
                            title="Identity protected by whistleblower protection policy"
                          >
                            <EyeOff size={11} style={{ color: 'var(--warning)' }} />
                            Protected Anonymous
                          </span>
                        ) : (
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <User size={13} style={{ color: 'var(--accent)' }} />
                            <div>
                              <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-1)' }}>
                                {c.student?.name || 'Student'}
                              </div>
                              <div style={{ fontSize: '0.67rem', color: 'var(--text-3)' }}>
                                {c.student?.rollNo || 'Disclosed'}
                              </div>
                            </div>
                          </div>
                        )}
                      </td>

                      {/* Priority Indicator */}
                      <td>
                        <PriorityIndicator score={c.priorityScore} />
                      </td>

                      {/* Status */}
                      <td>
                        <StatusBadge status={c.status} />
                      </td>

                      {/* Lead Officer */}
                      <td>
                        {c.assignedToStaff ? (
                          <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-1)' }}>
                            {c.assignedToStaff.name}
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.74rem', color: 'var(--text-3)', fontStyle: 'italic' }}>
                            Special Committee
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default RestrictedComplaints;
