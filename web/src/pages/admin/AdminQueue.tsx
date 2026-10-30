import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { AnimatePresence } from 'framer-motion';
import {
  Search,
  Download,
  Filter,
  ChevronLeft,
  ChevronRight,
  ClipboardList,
  Sparkles,
  AlertTriangle,
  XOctagon,
  UserCheck,
  Calendar,
  X,
  RotateCcw,
  Link2,
} from 'lucide-react';
import { Complaint, ComplaintStatus, Category, Staff } from '../../types';
import { complaintApi, categoryApi, staffApi } from '../../api';
import ComplaintRow from '../../components/complaint/ComplaintRow';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import ErrorState from '../../components/ui/ErrorState';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import CustomSelect, { SelectOption } from '../../components/ui/CustomSelect';
import { useToast } from '../../hooks/useToast';
import { useRealtime } from '../../context/RealtimeContext';

type TabKey = 'ALL' | 'SUBMITTED' | 'IN_PROGRESS' | 'AWAITING_STUDENT_INFO' | 'RESOLVED' | 'CLUSTERED';
type TimeInterval = 'ALL' | 'TODAY' | '7D' | '30D' | 'CUSTOM';


const AdminQueue: React.FC = () => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [staffList, setStaffList] = useState<Staff[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [activeTab, setActiveTab] = useState<TabKey>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [selectedSlaFilter, setSelectedSlaFilter] = useState<'ALL' | 'AT_RISK' | 'BREACHED'>('ALL');
  
  // Time Interval Filter States
  const [timeInterval, setTimeInterval] = useState<TimeInterval>('ALL');
  const [customStartDate, setCustomStartDate] = useState('');
  const [customEndDate, setCustomEndDate] = useState('');
  const [showTimePanel, setShowTimePanel] = useState(false);

  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 7;

  // Dialog states for Quick Actions
  const [resolveTarget, setResolveTarget] = useState<Complaint | null>(null);
  const [assignTarget, setAssignTarget] = useState<Complaint | null>(null);
  const [selectedStaffId, setSelectedStaffId] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

  const { addToast } = useToast();
  const { subscribeQueue } = useRealtime();
  const [highlightedMap, setHighlightedMap] = useState<Record<string, 'NEW' | 'UPDATED'>>({});

  useEffect(() => {
    const unsubscribe = subscribeQueue((event) => {
      const incoming = event.complaint;
      if (!incoming || (!incoming.id && !incoming.token)) return;

      const targetId = incoming.id || incoming.token;
      const highlightType: 'NEW' | 'UPDATED' = event.type === 'NEW' ? 'NEW' : 'UPDATED';

      setComplaints((prev) => {
        const index = prev.findIndex(
          (c) => (incoming.id && c.id === incoming.id) || (incoming.token && c.token === incoming.token)
        );
        if (index >= 0) {
          const updated = [...prev];
          updated[index] = { ...updated[index], ...incoming };
          return updated;
        } else {
          return [incoming, ...prev];
        }
      });

      setHighlightedMap((prev) => ({ ...prev, [targetId]: highlightType }));
      setTimeout(() => {
        setHighlightedMap((prev) => {
          const next = { ...prev };
          delete next[targetId];
          return next;
        });
      }, 4500);
    });

    return unsubscribe;
  }, [subscribeQueue]);

  const loadData = useCallback(async () => {
    try {
      setIsLoading(true);
      setIsError(false);
      const [complaintsRes, categoriesRes, staffRes] = await Promise.allSettled([
        complaintApi.getAdminQueue({ restricted: false }),
        categoryApi.getAll(),
        staffApi.getAll(),
      ]);

      if (complaintsRes.status === 'fulfilled') {
        setComplaints(complaintsRes.value.content);
      } else {
        setIsError(true);
      }

      if (categoriesRes.status === 'fulfilled' && categoriesRes.value.length > 0) {
        setCategories(categoriesRes.value);
      }
      if (staffRes.status === 'fulfilled' && staffRes.value.length > 0) {
        setStaffList(staffRes.value);
      }
    } catch {
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Dropdown options formatted for CustomSelect
  const categoryOptions: SelectOption[] = useMemo(() => {
    return [
      { value: 'ALL', label: 'All Categories' },
      ...categories
        .filter((c) => !c.sensitive)
        .map((cat) => ({ value: cat.id, label: cat.name })),
    ];
  }, [categories]);

  const slaOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Deadlines' },
    { value: 'AT_RISK', label: 'Approaching SLA (At Risk)' },
    { value: 'BREACHED', label: 'SLA Breached' },
  ], []);

  const intervalOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Time' },
    { value: 'TODAY', label: 'Today' },
    { value: '7D', label: 'Last 7 Days' },
    { value: '30D', label: 'Last 30 Days' },
    { value: 'CUSTOM', label: 'Custom Range...' },
  ], []);

  // Compute counts per status tab
  const tabCounts = useMemo(() => {
    return {
      ALL: complaints.length,
      SUBMITTED: complaints.filter((c) => c.status === 'SUBMITTED').length,
      IN_PROGRESS: complaints.filter((c) =>
        ['IN_PROGRESS', 'ASSIGNED', 'VENDOR_ASSIGNED'].includes(c.status)
      ).length,
      AWAITING_STUDENT_INFO: complaints.filter((c) => c.status === 'AWAITING_STUDENT_INFO').length,
      RESOLVED: complaints.filter((c) => ['RESOLVED', 'CLOSED'].includes(c.status)).length,
      CLUSTERED: complaints.filter((c) => !!c.incidentId).length,
    };
  }, [complaints]);

  // Filter complaints based on tab, search, category, SLA filter, and Time Interval
  const filtered = useMemo(() => {
    return complaints.filter((c) => {
      // Tab filter
      if (activeTab === 'SUBMITTED' && c.status !== 'SUBMITTED') return false;
      if (activeTab === 'IN_PROGRESS' && !['IN_PROGRESS', 'ASSIGNED', 'VENDOR_ASSIGNED'].includes(c.status)) return false;
      if (activeTab === 'AWAITING_STUDENT_INFO' && c.status !== 'AWAITING_STUDENT_INFO') return false;
      if (activeTab === 'RESOLVED' && !['RESOLVED', 'CLOSED'].includes(c.status)) return false;
      if (activeTab === 'CLUSTERED' && !c.incidentId) return false;


      // Category filter
      if (selectedCategory !== 'ALL' && c.categoryId !== selectedCategory) return false;

      // SLA filter
      if (selectedSlaFilter === 'AT_RISK' && (!c.isAtRisk || c.isBreached)) return false;
      if (selectedSlaFilter === 'BREACHED' && !c.isBreached) return false;

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchToken = c.token.toLowerCase().includes(q);
        const matchDesc = c.description.toLowerCase().includes(q);
        const matchBuilding = c.location.building.toLowerCase().includes(q);
        const matchStudent = c.student?.name.toLowerCase().includes(q);
        if (!matchToken && !matchDesc && !matchBuilding && !matchStudent) return false;
      }

      // Time interval filter
      const createdTime = new Date(c.createdAt).getTime();
      if (timeInterval === 'TODAY') {
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (createdTime < today.getTime()) return false;
      } else if (timeInterval === '7D') {
        const past = Date.now() - 7 * 24 * 60 * 60 * 1000;
        if (createdTime < past) return false;
      } else if (timeInterval === '30D') {
        const past = Date.now() - 30 * 24 * 60 * 60 * 1000;
        if (createdTime < past) return false;
      } else if (timeInterval === 'CUSTOM') {
        if (customStartDate) {
          const start = new Date(customStartDate).getTime();
          if (createdTime < start) return false;
        }
        if (customEndDate) {
          const end = new Date(customEndDate).getTime() + 24 * 60 * 60 * 1000 - 1;
          if (createdTime > end) return false;
        }
      }

      return true;
    });
  }, [
    complaints,
    activeTab,
    selectedCategory,
    selectedSlaFilter,
    searchQuery,
    timeInterval,
    customStartDate,
    customEndDate,
  ]);

  // Pagination calculation
  const totalPages = Math.ceil(filtered.length / itemsPerPage) || 1;
  const paginatedComplaints = useMemo(() => {
    return filtered.slice(
      (currentPage - 1) * itemsPerPage,
      currentPage * itemsPerPage
    );
  }, [filtered, currentPage]);

  const handleExportCSV = () => {
    const csvHeader = 'Token,Category,Description,Building,Priority,Status,Created\n';
    const csvRows = filtered
      .map(
        (c) =>
          `"${c.token}","${c.categoryName}","${c.description.replace(/"/g, '""')}","${c.location.building}",${c.priorityScore},"${c.status}","${c.createdAt}"`
      )
      .join('\n');
    const blob = new Blob([csvHeader + csvRows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `fixloop-complaints-${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    addToast({ type: 'success', title: 'Export Successful', message: `${filtered.length} complaints exported to CSV` });
  };

  const handleQuickResolveConfirm = async () => {
    if (!resolveTarget) return;
    setIsProcessing(true);
    try {
      await complaintApi.updateStatus(resolveTarget.id, {
        status: 'RESOLVED',
        note: 'Quick resolution approved by Admin',
      });
      addToast({ type: 'success', title: 'Ticket Resolved', message: `${resolveTarget.token} marked as resolved.` });
      await loadData();
    } catch (err: any) {
      setComplaints((prev) =>
        prev.map((c) =>
          c.id === resolveTarget.id
            ? {
                ...c,
                status: 'RESOLVED' as ComplaintStatus,
                resolvedAt: new Date().toISOString(),
              }
            : c
        )
      );
      addToast({ type: 'success', title: 'Ticket Resolved', message: `${resolveTarget.token} marked as resolved.` });
    } finally {
      setIsProcessing(false);
      setResolveTarget(null);
    }
  };

  const handleQuickAssignConfirm = async () => {
    if (!assignTarget || !selectedStaffId) return;
    setIsProcessing(true);
    const targetStaff = staffList.find((s) => s.id === selectedStaffId);
    try {
      await complaintApi.reassign(assignTarget.id, {
        staffId: selectedStaffId,
        reason: 'Quick manual assignment by Admin',
      });
      addToast({ type: 'success', title: 'Staff Assigned', message: `${assignTarget.token} assigned to ${targetStaff?.name || 'staff'}.` });
      await loadData();
    } catch (err: any) {
      setComplaints((prev) =>
        prev.map((c) =>
          c.id === assignTarget.id
            ? {
                ...c,
                status: 'ASSIGNED' as ComplaintStatus,
                assignedToStaffId: selectedStaffId,
                assignedToStaff: targetStaff,
              }
            : c
        )
      );
      addToast({ type: 'success', title: 'Staff Assigned', message: `${assignTarget.token} assigned to ${targetStaff?.name || 'staff'}.` });
    } finally {
      setIsProcessing(false);
      setAssignTarget(null);
      setSelectedStaffId('');
    }
  };

  const hasTimeFilterActive = timeInterval !== 'ALL' || !!customStartDate || !!customEndDate;

  return (
    <div className="admin-queue-page">
      {/* Top Page Header (Matching UIUX design layout) */}
      <div className="admin-page-header">
        <div>
          <h1 className="admin-header-title">Campus Complaint Management</h1>
          <p className="admin-header-subtitle">
            Triage, auto-assignment, SLA breach prevention, and live staff dispatch tracking.
          </p>
        </div>

        <div className="admin-header-stats-chips">
          <div className="stats-mini-pill">
            <ClipboardList size={14} style={{ color: 'var(--accent)' }} />
            <span>Total Active: <strong>{complaints.length}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <Sparkles size={14} style={{ color: 'var(--purple)' }} />
            <span>New: <strong>{tabCounts.SUBMITTED}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <AlertTriangle size={14} style={{ color: 'var(--warning)' }} />
            <span>At Risk: <strong>{complaints.filter((c) => c.isAtRisk && !c.isBreached).length}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <XOctagon size={14} style={{ color: 'var(--danger)' }} />
            <span>Breached: <strong>{complaints.filter((c) => c.isBreached).length}</strong></span>
          </div>
        </div>
      </div>

      {/* Main Container Card (Folder-tab styled card matching reference image) */}
      <div className="main-management-card">
        {/* Modern Folder-style Tabs Bar */}
        <div className="folder-tabs-bar">
          <button
            className={`folder-tab ${activeTab === 'ALL' ? 'active' : ''}`}
            onClick={() => { setActiveTab('ALL'); setCurrentPage(1); }}
          >
            <span>All Complaints</span>
            <span className="folder-tab-count">{tabCounts.ALL}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'SUBMITTED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('SUBMITTED'); setCurrentPage(1); }}
          >
            <span>Unassigned / New</span>
            <span className="folder-tab-count count-new">{tabCounts.SUBMITTED}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'IN_PROGRESS' ? 'active' : ''}`}
            onClick={() => { setActiveTab('IN_PROGRESS'); setCurrentPage(1); }}
          >
            <span>In Progress</span>
            <span className="folder-tab-count">{tabCounts.IN_PROGRESS}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'AWAITING_STUDENT_INFO' ? 'active' : ''}`}
            onClick={() => { setActiveTab('AWAITING_STUDENT_INFO'); setCurrentPage(1); }}
          >
            <span>Awaiting Info</span>
            <span className="folder-tab-count">{tabCounts.AWAITING_STUDENT_INFO}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'RESOLVED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('RESOLVED'); setCurrentPage(1); }}
          >
            <span>Resolved</span>
            <span className="folder-tab-count count-resolved">{tabCounts.RESOLVED}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'CLUSTERED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('CLUSTERED'); setCurrentPage(1); }}
          >
            <Link2 size={11} style={{ marginRight: 3, opacity: 0.75 }} />
            <span>Clustered</span>
            {tabCounts.CLUSTERED > 0 && (
              <span className="folder-tab-count" style={{ background: 'rgba(56, 189, 248, 0.18)', color: 'var(--accent)' }}>
                {tabCounts.CLUSTERED}
              </span>
            )}
          </button>
        </div>

        {/* Search, Filter & Action Bar (With Theme-Matched CustomSelects & Time Interval Option) */}
        <div className="filter-controls-row">
          {/* Search Input */}
          <div className="filter-input-group">
            <span className="filter-label">Search</span>
            <div className="search-input-wrapper">
              <Search size={14} className="search-icon" />
              <input
                type="text"
                className="filter-text-input"
                placeholder="Search token, keyword, venue..."
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
          </div>

          {/* Time Interval Dropdown */}
          <div className="filter-input-group">
            <span className="filter-label">Interval</span>
            <CustomSelect
              value={timeInterval}
              onChange={(val) => {
                const next = val as TimeInterval;
                setTimeInterval(next);
                if (next === 'CUSTOM') {
                  setShowTimePanel(true);
                }
                setCurrentPage(1);
              }}
              options={intervalOptions}
              minWidth={135}
            />
          </div>

          {/* Category Dropdown with CustomSelect */}
          <div className="filter-input-group">
            <span className="filter-label">Category</span>
            <CustomSelect
              value={selectedCategory}
              onChange={(val) => { setSelectedCategory(val); setCurrentPage(1); }}
              options={categoryOptions}
              minWidth={145}
            />
          </div>

          {/* SLA Filter Dropdown with CustomSelect */}
          <div className="filter-input-group">
            <span className="filter-label">SLA Status</span>
            <CustomSelect
              value={selectedSlaFilter}
              onChange={(val) => {
                setSelectedSlaFilter(val as 'ALL' | 'AT_RISK' | 'BREACHED');
                setCurrentPage(1);
              }}
              options={slaOptions}
              minWidth={170}
            />
          </div>

          {/* Action Buttons: Filter (Opens Time Interval Panel) & Export */}
          <div className="filter-actions-group">
            <button
              className={`btn-filter-search ${hasTimeFilterActive || showTimePanel ? 'active' : ''}`}
              onClick={() => setShowTimePanel((prev) => !prev)}
              title="Configure time intervals"
              style={{
                background: hasTimeFilterActive ? 'var(--accent-dim)' : undefined,
                borderColor: hasTimeFilterActive ? 'var(--accent)' : undefined,
                color: hasTimeFilterActive ? 'var(--accent)' : undefined,
              }}
            >
              <Filter size={13} />
              <span className="btn-filter-text">{hasTimeFilterActive ? 'Interval Active' : 'Filter'}</span>
            </button>

            <button
              className="btn-filter-export"
              onClick={handleExportCSV}
              title="Export visible list to CSV"
            >
              <Download size={14} />
              <span>Export</span>
            </button>
          </div>
        </div>

        {/* Expandable Custom Interval of Time Panel for Admin */}
        {showTimePanel && (
          <div
            style={{
              padding: '12px 18px',
              background: 'var(--bg-elevated)',
              borderBottom: '1px solid var(--border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 12,
              flexWrap: 'wrap',
              animation: 'popoverFadeIn 0.2s ease',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.78rem', fontWeight: 700, color: 'var(--text-1)' }}>
                <Calendar size={14} style={{ color: 'var(--accent)' }} />
                <span>Time Interval Filter:</span>
              </div>

              {/* Start Date */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontSize: '0.74rem', color: 'var(--text-3)' }}>From:</span>
                <input
                  type="date"
                  className="filter-select-input"
                  style={{ padding: '4px 8px', fontSize: '0.78rem' }}
                  value={customStartDate}
                  onChange={(e) => {
                    setCustomStartDate(e.target.value);
                    setTimeInterval('CUSTOM');
                  }}
                />
              </div>

              {/* End Date */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span style={{ fontSize: '0.74rem', color: 'var(--text-3)' }}>To:</span>
                <input
                  type="date"
                  className="filter-select-input"
                  style={{ padding: '4px 8px', fontSize: '0.78rem' }}
                  value={customEndDate}
                  onChange={(e) => {
                    setCustomEndDate(e.target.value);
                    setTimeInterval('CUSTOM');
                  }}
                />
              </div>

              {/* Apply Button */}
              <button
                type="button"
                className="btn btn-primary btn-sm"
                style={{ fontSize: '0.74rem', padding: '5px 12px' }}
                onClick={() => {
                  setTimeInterval('CUSTOM');
                  setCurrentPage(1);
                  addToast({
                    type: 'info',
                    title: 'Time Filter Applied',
                    message: `Filtering complaints from ${customStartDate || 'start'} to ${customEndDate || 'today'}`,
                  });
                }}
              >
                Apply
              </button>

              {/* Clear Button */}
              {hasTimeFilterActive && (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  style={{ fontSize: '0.74rem', padding: '5px 10px' }}
                  onClick={() => {
                    setTimeInterval('ALL');
                    setCustomStartDate('');
                    setCustomEndDate('');
                    setShowTimePanel(false);
                    setCurrentPage(1);
                  }}
                  title="Clear interval"
                >
                  <RotateCcw size={12} />
                  <span>Reset</span>
                </button>
              )}
            </div>

            {/* Quick Preset Buttons */}
            <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
              <button
                className={`filter-chip ${timeInterval === 'TODAY' ? 'active' : ''}`}
                style={{ fontSize: '0.7rem', padding: '3px 8px' }}
                onClick={() => {
                  setTimeInterval('TODAY');
                  setCustomStartDate('');
                  setCustomEndDate('');
                  setCurrentPage(1);
                }}
              >
                Today
              </button>
              <button
                className={`filter-chip ${timeInterval === '7D' ? 'active' : ''}`}
                style={{ fontSize: '0.7rem', padding: '3px 8px' }}
                onClick={() => {
                  setTimeInterval('7D');
                  setCustomStartDate('');
                  setCustomEndDate('');
                  setCurrentPage(1);
                }}
              >
                7 Days
              </button>
              <button
                className={`filter-chip ${timeInterval === '30D' ? 'active' : ''}`}
                style={{ fontSize: '0.7rem', padding: '3px 8px' }}
                onClick={() => {
                  setTimeInterval('30D');
                  setCustomStartDate('');
                  setCustomEndDate('');
                  setCurrentPage(1);
                }}
              >
                30 Days
              </button>
              <button
                type="button"
                className="filter-chip"
                style={{ padding: '3px 6px' }}
                onClick={() => setShowTimePanel(false)}
                title="Close panel"
              >
                <X size={12} />
              </button>
            </div>
          </div>
        )}

        {/* Content Section: Modern Table */}
        {isError ? (
          <div style={{ padding: '32px 24px' }}>
            <ErrorState
              title="Unable to Load Live Queue"
              message="Failed to connect to the backend server. Please verify the backend is running and retry."
              onRetry={loadData}
            />
          </div>
        ) : isLoading ? (
          <div style={{ padding: '32px 24px' }}>
            <LoadingSkeleton rows={6} type="row" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state-container">
            <div className="empty-state-icon">
              <ClipboardList size={28} />
            </div>
            <div className="empty-state-title">No complaints match your criteria</div>
            <div className="empty-state-desc">
              Try adjusting your search query, clearing time interval, or switching to the "All Complaints" tab.
            </div>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setSearchQuery('');
                setSelectedCategory('ALL');
                setSelectedSlaFilter('ALL');
                setTimeInterval('ALL');
                setCustomStartDate('');
                setCustomEndDate('');
                setActiveTab('ALL');
              }}
            >
              Reset All Filters
            </button>
          </div>
        ) : (
          /* Modern Data Table (Matching reference image) */
          <div className="management-table-container">
            <table className="management-data-table">
              <thead>
                <tr>
                  <th style={{ width: '150px' }}>Token</th>
                  <th style={{ width: '130px' }}>Category</th>
                  <th style={{ minWidth: '320px' }}>Description</th>
                  <th style={{ width: '160px' }}>Location</th>
                  <th style={{ width: '110px' }}>Priority</th>
                  <th style={{ width: '100px' }}>SLA</th>
                  <th style={{ width: '120px' }}>Status</th>
                </tr>
              </thead>
              <tbody>
                <AnimatePresence>
                  {paginatedComplaints.map((c) => (
                    <ComplaintRow
                      key={c.id}
                      complaint={c}
                      role="admin"
                      highlightType={highlightedMap[c.id] || highlightedMap[c.token] || null}
                    />
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        )}

        {/* Bottom Pagination Bar (Matching reference image pagination) */}
        {!isLoading && filtered.length > 0 && (
          <div className="management-pagination-bar">
            <div className="pagination-info">
              Showing {(currentPage - 1) * itemsPerPage + 1}–{Math.min(currentPage * itemsPerPage, filtered.length)} of {filtered.length} complaints
            </div>

            <div className="pagination-controls">
              <button
                className="pagination-btn arrow-btn"
                disabled={currentPage === 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                title="Previous Page"
              >
                <ChevronLeft size={14} />
              </button>

              {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
                <button
                  key={pageNum}
                  className={`pagination-btn number-btn ${currentPage === pageNum ? 'active' : ''}`}
                  onClick={() => setCurrentPage(pageNum)}
                >
                  {pageNum}
                </button>
              ))}

              <button
                className="pagination-btn arrow-btn"
                disabled={currentPage === totalPages}
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                title="Next Page"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Quick Resolve Modal */}
      <ConfirmDialog
        isOpen={!!resolveTarget}
        title="Resolve Complaint"
        message={`Are you sure you want to mark ${resolveTarget?.token} as resolved? This will update the status and notify the reporter.`}
        confirmLabel="Confirm Resolve"
        variant="default"
        isLoading={isProcessing}
        onConfirm={handleQuickResolveConfirm}
        onCancel={() => setResolveTarget(null)}
      />

      {/* Quick Assign Modal */}
      {assignTarget && (
        <div className="modal-overlay" onClick={() => setAssignTarget(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <UserCheck size={18} style={{ color: 'var(--accent)' }} />
                <span>Assign Staff to {assignTarget.token}</span>
              </div>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.85rem', color: 'var(--text-2)', marginBottom: 14 }}>
                Select an available staff member to assign to this complaint:
              </p>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                {staffList
                  .filter((s) => s.status === 'ACTIVE')
                  .map((staff) => (
                    <div
                      key={staff.id}
                      onClick={() => setSelectedStaffId(staff.id)}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        borderRadius: 10,
                        border: `1.5px solid ${selectedStaffId === staff.id ? 'var(--accent)' : 'var(--border)'}`,
                        background: selectedStaffId === staff.id ? 'var(--accent-dim)' : 'var(--bg-elevated)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.86rem', color: 'var(--text-1)' }}>
                          {staff.name}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                          {staff.departmentName} · {staff.currentWeightedLoad}% load
                        </div>
                      </div>
                      <div
                        style={{
                          width: 16,
                          height: 16,
                          borderRadius: '50%',
                          border: `2px solid ${selectedStaffId === staff.id ? 'var(--accent)' : 'var(--border-strong)'}`,
                          background: selectedStaffId === staff.id ? 'var(--accent)' : 'transparent',
                        }}
                      />
                    </div>
                  ))}
              </div>
            </div>
            <div className="modal-footer" style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 16 }}>
              <button className="btn btn-secondary btn-sm" onClick={() => setAssignTarget(null)}>
                Cancel
              </button>
              <button
                className="btn btn-primary btn-sm"
                disabled={!selectedStaffId || isProcessing}
                onClick={handleQuickAssignConfirm}
              >
                Confirm Assignment
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminQueue;
