import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CheckCircle2,
  Calendar,
  Search,
  Download,
  AlertTriangle,
  RotateCcw,
} from 'lucide-react';
import { Complaint, Category } from '../../types';
import { complaintApi, categoryApi } from '../../api';
import { useAuth } from '../../hooks/useAuth';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import CategoryIcon from '../../components/ui/CategoryIcon';
import ErrorState from '../../components/ui/ErrorState';
import { getCategoryColor, truncate } from '../../utils/helpers';

interface ResolutionHistoryProps {
  role: 'admin' | 'staff';
}

const ResolutionHistory: React.FC<ResolutionHistoryProps> = ({ role }) => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);

  // Filters State — Custom Date Window only
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedSla, setSelectedSla] = useState<'ALL' | 'MET' | 'BREACHED'>('ALL');
  const [sortBy, setSortBy] = useState<'DATE_DESC' | 'DATE_ASC' | 'TIME_FAST' | 'PRIORITY_HIGH'>('DATE_DESC');

  const loadHistory = useCallback(async () => {
    try {
      setIsLoading(true);
      setIsError(false);
      const [queueRes, catsRes] = await Promise.all([
        role === 'admin'
          ? complaintApi.getAdminQueue({ size: 100 })
          : complaintApi.getStaffQueue({ size: 100 }),
        categoryApi.getAll(),
      ]);

      setComplaints(queueRes.content || []);
      setCategories(catsRes || []);
    } catch {
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, [role]);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  // 1. Filter out only resolved/closed complaints, scoped to staff member if in staff view
  const baseResolved = useMemo(() => {
    return complaints.filter((c) => {
      const isResolved = c.status === 'RESOLVED' || c.status === 'CLOSED';
      if (!isResolved) return false;
      if (role === 'staff') {
        const staffId = user?.id || 'stf-5';
        return c.assignedToStaffId === staffId || !c.assignedToStaffId;
      }
      return true;
    });
  }, [complaints, role, user]);

  // 2. Filter by search, category, SLA, and custom date range
  const filteredComplaints = useMemo(() => {
    return baseResolved.filter((c) => {
      const resolvedDateStr = c.resolvedAt || c.createdAt;
      const resDate = new Date(resolvedDateStr);

      // Custom Date Range Filtering
      if (fromDate) {
        const from = new Date(fromDate);
        from.setHours(0, 0, 0, 0);
        if (resDate < from) return false;
      }
      if (toDate) {
        const to = new Date(toDate);
        to.setHours(23, 59, 59, 999);
        if (resDate > to) return false;
      }

      // Search Query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesToken = c.token.toLowerCase().includes(q);
        const matchesDesc = c.description.toLowerCase().includes(q);
        const matchesCat = c.categoryName.toLowerCase().includes(q);
        const matchesLoc = (c.location.building + ' ' + (c.location.room || '')).toLowerCase().includes(q);
        const matchesStudent = (c.student?.name || '').toLowerCase().includes(q);
        const matchesStaff = (c.assignedToStaff?.name || '').toLowerCase().includes(q);
        if (!matchesToken && !matchesDesc && !matchesCat && !matchesLoc && !matchesStudent && !matchesStaff) {
          return false;
        }
      }

      // Category Filter
      if (selectedCategory !== 'ALL' && c.categoryName !== selectedCategory) {
        return false;
      }

      // SLA Filter
      if (selectedSla === 'MET') {
        const deadline = new Date(c.slaDeadline).getTime();
        const resolved = new Date(resolvedDateStr).getTime();
        if (resolved > deadline || c.isBreached) return false;
      } else if (selectedSla === 'BREACHED') {
        const deadline = new Date(c.slaDeadline).getTime();
        const resolved = new Date(resolvedDateStr).getTime();
        if (resolved <= deadline && !c.isBreached) return false;
      }

      return true;
    });
  }, [baseResolved, fromDate, toDate, searchQuery, selectedCategory, selectedSla]);

  // 3. Sorting
  const sortedComplaints = useMemo(() => {
    return [...filteredComplaints].sort((a, b) => {
      const aResolved = new Date(a.resolvedAt || a.createdAt).getTime();
      const bResolved = new Date(b.resolvedAt || b.createdAt).getTime();
      const aCreated = new Date(a.createdAt).getTime();
      const bCreated = new Date(b.createdAt).getTime();
      const aDuration = aResolved - aCreated;
      const bDuration = bResolved - bCreated;

      if (sortBy === 'DATE_DESC') return bResolved - aResolved;
      if (sortBy === 'DATE_ASC') return aResolved - bResolved;
      if (sortBy === 'TIME_FAST') return aDuration - bDuration;
      if (sortBy === 'PRIORITY_HIGH') return b.priorityScore - a.priorityScore;
      return 0;
    });
  }, [filteredComplaints, sortBy]);

  // Export to CSV
  const handleExportCSV = () => {
    const headers = [
      'Token',
      'Category',
      'Description',
      'Building',
      'Room',
      'Reported At',
      'Resolved At',
      'SLA Deadline',
      'Met SLA',
      'Staff Assigned',
      'Rating',
    ];

    const rows = sortedComplaints.map((c) => {
      const resolved = new Date(c.resolvedAt || c.createdAt).getTime();
      const deadline = new Date(c.slaDeadline).getTime();
      const metSla = resolved <= deadline && !c.isBreached ? 'Yes' : 'No';

      return [
        c.token,
        c.categoryName,
        `"${c.description.replace(/"/g, '""')}"`,
        c.location.building,
        c.location.room || '',
        c.createdAt,
        c.resolvedAt || '',
        c.slaDeadline,
        metSla,
        c.assignedToStaff?.name || 'Unassigned',
        c.review?.rating || 'N/A',
      ];
    });

    const csvContent =
      'data:text/csv;charset=utf-8,' +
      [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute(
      'download',
      `FixLoop_Resolution_History_${role}_${Date.now()}.csv`
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleResetFilters = () => {
    setFromDate('');
    setToDate('');
    setSearchQuery('');
    setSelectedCategory('ALL');
    setSelectedSla('ALL');
    setSortBy('DATE_DESC');
  };

  const hasActiveFilters = fromDate || toDate || searchQuery || selectedCategory !== 'ALL' || selectedSla !== 'ALL';

  return (
    <div className="admin-queue-page">
      {/* Clean Page Header */}
      <div className="page-header" style={{ marginBottom: 18 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
          <div>
            <h1 className="page-title">
              {role === 'admin' ? 'Campus Resolution History' : 'My Resolution History'}
            </h1>
            <p className="page-subtitle">
              {role === 'admin'
                ? 'Historical record of resolved complaints and SLA turnaround metrics across all departments'
                : 'Historical record of tickets resolved by you, turnaround times, and student feedback'}
            </p>
          </div>
          <button className="btn-filter-export" onClick={handleExportCSV}>
            <Download size={14} />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Main Clean Elevated Management Card */}
      <div className="main-management-card">
        {/* Single-Line Clean Filter Controls Row */}
        <div className="filter-controls-row">
          {/* Live Search */}
          <div className="search-input-wrapper" style={{ width: 220 }}>
            <Search size={14} className="search-icon" />
            <input
              type="text"
              placeholder="Search token, description..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="filter-text-input"
            />
          </div>

          {/* Custom Date Window: From Date */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: '0.74rem', fontWeight: 600, color: 'var(--text-3)' }}>From:</span>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              className="filter-select-input"
              style={{ minWidth: 130 }}
            />
          </div>

          {/* Custom Date Window: To Date */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ fontSize: '0.74rem', fontWeight: 600, color: 'var(--text-3)' }}>To:</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              className="filter-select-input"
              style={{ minWidth: 130 }}
            />
          </div>

          {/* Category Filter */}
          <select
            className="filter-select-input"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <option value="ALL">All Categories</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.name}>
                {cat.name}
              </option>
            ))}
          </select>

          {/* SLA Filter */}
          <select
            className="filter-select-input"
            value={selectedSla}
            onChange={(e) => setSelectedSla(e.target.value as any)}
          >
            <option value="ALL">All SLA Statuses</option>
            <option value="MET">Resolved Within SLA</option>
            <option value="BREACHED">SLA Breached</option>
          </select>

          {/* Sort By */}
          <select
            className="filter-select-input"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as any)}
          >
            <option value="DATE_DESC">Resolved: Newest First</option>
            <option value="DATE_ASC">Resolved: Oldest First</option>
            <option value="TIME_FAST">Turnaround: Fastest First</option>
            <option value="PRIORITY_HIGH">Priority: Highest First</option>
          </select>

          {/* Reset Filters */}
          {hasActiveFilters && (
            <button
              className="btn btn-ghost btn-sm"
              onClick={handleResetFilters}
              title="Reset all filters"
              style={{ display: 'inline-flex', alignItems: 'center', gap: 4, fontSize: '0.74rem' }}
            >
              <RotateCcw size={12} />
              <span>Reset</span>
            </button>
          )}

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
              {sortedComplaints.length} tickets
            </span>
          </div>
        </div>

        {/* Clean Unified Data Table */}
        {isLoading ? (
          <div style={{ padding: '24px' }}>
            <LoadingSkeleton rows={5} type="card" />
          </div>
        ) : isError ? (
          <div style={{ padding: '24px' }}>
            <ErrorState
              title="Failed to Load Resolution History"
              message="Could not load historical resolved tickets from the server. Please check your network connection."
              onRetry={loadHistory}
            />
          </div>
        ) : sortedComplaints.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">
              <Calendar size={26} />
            </div>
            <div className="empty-state-title">No resolution history found</div>
            <div className="empty-state-desc">
              No resolved complaints match your selected date window and filter settings.
            </div>
            {hasActiveFilters && (
              <button
                className="btn btn-secondary btn-sm"
                onClick={handleResetFilters}
                style={{ marginTop: 12 }}
              >
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          <div className="management-table-container">
            <table className="management-data-table">
              <thead>
                <tr>
                  <th style={{ width: '150px' }}>Token</th>
                  <th style={{ width: '130px' }}>Category</th>
                  <th style={{ minWidth: '300px' }}>Description</th>
                  <th style={{ width: '150px' }}>Location</th>
                  <th style={{ width: '130px' }}>Resolved Date</th>
                  <th style={{ width: '130px' }}>Turnaround</th>
                  {role === 'admin' && <th style={{ width: '160px' }}>Staff Assigned</th>}
                  <th style={{ width: '110px', textAlign: 'center' }}>Review</th>
                </tr>
              </thead>
              <tbody>
                {sortedComplaints.map((c) => {
                  const catColor = getCategoryColor(c.categoryName);
                  const created = new Date(c.createdAt).getTime();
                  const resolved = new Date(c.resolvedAt || c.createdAt).getTime();
                  const deadline = new Date(c.slaDeadline).getTime();
                  const diffHours = Math.max(0.5, (resolved - created) / (1000 * 60 * 60)).toFixed(1);
                  const isSlaMet = resolved <= deadline && !c.isBreached;

                  return (
                    <tr
                      key={c.id}
                      className="modern-table-row clickable-table-row"
                      onClick={() => navigate(`/${role}/complaint/${c.id}`)}
                      title="Click to view full complaint details"
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

                      {/* Description */}
                      <td className="col-description">
                        <div className="table-desc-title">
                          {truncate(c.description, 95)}
                        </div>
                        <div className="table-desc-reporter">
                          Reported by {c.isAnonymous ? 'Anonymous Student' : (c.student?.name || 'Student')}
                        </div>
                      </td>

                      {/* Location */}
                      <td className="col-location">
                        <div className="table-location-text">{c.location.building}</div>
                        <div className="table-location-sub">
                          {c.location.room || c.location.floor || 'Campus'}
                        </div>
                      </td>

                      {/* Resolved Date */}
                      <td style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-1)', whiteSpace: 'nowrap' }}>
                        {new Date(c.resolvedAt || c.createdAt).toLocaleDateString('en-US', {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </td>

                      {/* Turnaround / SLA */}
                      <td>
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                          <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-1)' }}>
                            {diffHours} hrs
                          </span>
                          <span
                            style={{
                              fontSize: '0.64rem',
                              fontWeight: 800,
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: 3,
                              color: isSlaMet ? 'var(--success)' : 'var(--danger)',
                            }}
                          >
                            {isSlaMet ? <CheckCircle2 size={10} /> : <AlertTriangle size={10} />}
                            {isSlaMet ? 'Within SLA' : 'Breached'}
                          </span>
                        </div>
                      </td>

                      {/* Resolved By (Admin View) */}
                      {role === 'admin' && (
                        <td>
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
                              {c.assignedToStaff?.avatarInitials || 'ST'}
                            </div>
                            <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-1)' }}>
                              {c.assignedToStaff?.name || 'Staff Member'}
                            </span>
                          </div>
                        </td>
                      )}

                      {/* Student Review */}
                      <td style={{ textAlign: 'center' }}>
                        {c.review ? (
                          <span
                            style={{
                              fontSize: '0.74rem',
                              fontWeight: 700,
                              color: 'var(--warning)',
                              background: 'rgba(245, 158, 11, 0.12)',
                              padding: '2px 7px',
                              borderRadius: 999,
                            }}
                            title={c.review.comment}
                          >
                            {c.review.rating} ★
                          </span>
                        ) : (
                          <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>—</span>
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

export default ResolutionHistory;
