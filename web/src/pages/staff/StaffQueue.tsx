import React, { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ClipboardList,
  RefreshCw,
  Search,
  AlertTriangle,
  XOctagon,
  Filter,
  Download,
  ChevronLeft,
  ChevronRight,
  Zap,
  Sparkles,
  X,
} from 'lucide-react';
import { mockComplaints, mockCategories } from '../../data/mockData';
import { Complaint } from '../../types';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import ComplaintRow from '../../components/complaint/ComplaintRow';
import CustomSelect, { SelectOption } from '../../components/ui/CustomSelect';
import { useToast } from '../../hooks/useToast';

const STAFF_ID = 'stf-5';

type TabKey = 'ALL' | 'ASSIGNED' | 'IN_PROGRESS' | 'AWAITING_STUDENT_INFO' | 'VENDOR_ASSIGNED' | 'RESOLVED';

const StaffQueue: React.FC = () => {
  const { addToast } = useToast();

  const [activeTab, setActiveTab] = useState<TabKey>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedSlaFilter, setSelectedSlaFilter] = useState<'ALL' | 'AT_RISK' | 'BREACHED'>('ALL');
  const [selectedPriority, setSelectedPriority] = useState('ALL');
  const [selectedLocation, setSelectedLocation] = useState('ALL');
  const [quickFocus, setQuickFocus] = useState<'ALL' | 'AT_RISK' | 'HIGH_PRIORITY' | 'NEW'>('ALL');
  const [showFilterDrawer, setShowFilterDrawer] = useState(false);

  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 7;

  useEffect(() => {
    const timer = setTimeout(() => {
      setComplaints(mockComplaints.filter((c) => c.assignedToStaffId === STAFF_ID));
      setIsLoading(false);
    }, 350);
    return () => clearTimeout(timer);
  }, []);

  // Periodic assignment simulation
  useEffect(() => {
    const timer = setTimeout(() => {
      addToast({
        type: 'info',
        title: 'New ticket assigned',
        message: 'SNT-2026-000220 — WiFi outage in Lab CS-306 has been assigned to you',
        duration: 5000,
      });
    }, 25000);
    return () => clearTimeout(timer);
  }, [addToast]);

  const categoryOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Categories' },
    ...mockCategories
      .filter((c) => !c.sensitive)
      .map((cat) => ({ value: cat.name, label: cat.name })),
  ], []);

  const slaOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Deadlines' },
    { value: 'AT_RISK', label: 'Approaching SLA (At Risk)' },
    { value: 'BREACHED', label: 'SLA Breached' },
  ], []);

  const priorityOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Priorities' },
    { value: 'HIGH', label: 'High (Score > 70)' },
    { value: 'MEDIUM', label: 'Medium (Score 40–70)' },
    { value: 'LOW', label: 'Low (Score < 40)' },
  ], []);

  // Tab counts
  const tabCounts = useMemo(() => {
    return {
      ALL: complaints.length,
      ASSIGNED: complaints.filter((c) => c.status === 'ASSIGNED').length,
      IN_PROGRESS: complaints.filter((c) => c.status === 'IN_PROGRESS').length,
      AWAITING_STUDENT_INFO: complaints.filter((c) => c.status === 'AWAITING_STUDENT_INFO').length,
      VENDOR_ASSIGNED: complaints.filter((c) => c.status === 'VENDOR_ASSIGNED').length,
      RESOLVED: complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length,
      AT_RISK: complaints.filter((c) => c.isAtRisk && !c.isBreached).length,
      BREACHED: complaints.filter((c) => c.isBreached).length,
    };
  }, [complaints]);

  // Extract unique venues/buildings from complaints
  const venueOptions = useMemo(() => {
    const venues = Array.from(new Set(complaints.map((c) => c.location?.building).filter(Boolean)));
    return ['ALL', ...venues];
  }, [complaints]);

  // Active filters count
  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (searchQuery.trim()) count++;
    if (selectedCategory !== 'ALL') count++;
    if (selectedSlaFilter !== 'ALL') count++;
    if (selectedPriority !== 'ALL') count++;
    if (selectedLocation !== 'ALL') count++;
    if (quickFocus !== 'ALL') count++;
    return count;
  }, [searchQuery, selectedCategory, selectedSlaFilter, selectedPriority, selectedLocation, quickFocus]);

  // Filtered dataset
  const filtered = useMemo(() => {
    return complaints.filter((c) => {
      // Tab filter
      if (activeTab === 'RESOLVED') {
        if (c.status !== 'RESOLVED' && c.status !== 'CLOSED') return false;
      } else if (activeTab !== 'ALL' && c.status !== activeTab) {
        return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matches =
          c.token.toLowerCase().includes(q) ||
          c.description.toLowerCase().includes(q) ||
          (c.location?.building && c.location.building.toLowerCase().includes(q)) ||
          c.categoryName.toLowerCase().includes(q);
        if (!matches) return false;
      }

      // Category filter
      if (selectedCategory !== 'ALL' && c.categoryId !== selectedCategory && c.categoryName !== selectedCategory) {
        return false;
      }

      // SLA filter
      if (selectedSlaFilter === 'AT_RISK' && (!c.isAtRisk || c.isBreached)) return false;
      if (selectedSlaFilter === 'BREACHED' && !c.isBreached) return false;

      // Priority filter
      if (selectedPriority === 'HIGH' && c.priorityScore < 70) return false;
      if (selectedPriority === 'MEDIUM' && (c.priorityScore < 40 || c.priorityScore >= 70)) return false;
      if (selectedPriority === 'LOW' && c.priorityScore >= 40) return false;

      // Location / Venue filter
      if (selectedLocation !== 'ALL' && c.location?.building !== selectedLocation) {
        return false;
      }

      // Quick focus filter
      if (quickFocus === 'AT_RISK' && (!c.isAtRisk || c.isBreached)) return false;
      if (quickFocus === 'HIGH_PRIORITY' && c.priorityScore < 70) return false;
      if (quickFocus === 'NEW' && c.status !== 'ASSIGNED') return false;

      return true;
    });
  }, [complaints, activeTab, searchQuery, selectedCategory, selectedSlaFilter, selectedPriority, selectedLocation, quickFocus]);

  // Pagination calculation
  const totalPages = Math.ceil(filtered.length / itemsPerPage) || 1;
  const paginatedComplaints = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return filtered.slice(start, start + itemsPerPage);
  }, [filtered, currentPage]);

  const handleExportCSV = () => {
    const headers = ['Token', 'Category', 'Description', 'Building', 'Room', 'Priority', 'Status', 'SLA Deadline'];
    const rows = filtered.map((c) => [
      c.token,
      `"${c.categoryName}"`,
      `"${c.description.replace(/"/g, '""')}"`,
      `"${c.location.building}"`,
      `"${c.location.room || ''}"`,
      c.priorityScore,
      c.status,
      c.slaDeadline,
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `FixLoop_Staff_Queue_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    addToast({
      type: 'success',
      title: 'Queue Exported',
      message: `${filtered.length} assigned tickets exported to CSV.`,
    });
  };

  return (
    <div className="admin-queue-page">
      {/* 1. Top Page Header (Matching Admin Queue Design) */}
      <div className="admin-page-header">
        <div>
          <h1 className="admin-header-title">Staff Dispatch & Triage Console</h1>
          <p className="admin-header-subtitle">
            Personal assigned tickets, SLA countdowns, priority triage, and work management.
          </p>
        </div>

        <div className="admin-header-stats-chips">
          <div className="stats-mini-pill">
            <ClipboardList size={14} style={{ color: 'var(--accent)' }} />
            <span>Total Assigned: <strong>{tabCounts.ALL}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <RefreshCw size={14} style={{ color: 'var(--warning)' }} />
            <span>In Progress: <strong>{tabCounts.IN_PROGRESS}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <AlertTriangle size={14} style={{ color: 'var(--orange)' }} />
            <span>At Risk: <strong>{tabCounts.AT_RISK}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <XOctagon size={14} style={{ color: 'var(--danger)' }} />
            <span>Breached: <strong>{tabCounts.BREACHED}</strong></span>
          </div>
        </div>
      </div>

      {/* 2. Main Container Card (Folder-tab styled card matching Admin table) */}
      <div className="main-management-card">
        {/* Modern Folder-style Tabs Bar */}
        <div className="folder-tabs-bar">
          <button
            className={`folder-tab ${activeTab === 'ALL' ? 'active' : ''}`}
            onClick={() => { setActiveTab('ALL'); setCurrentPage(1); }}
          >
            <span>All Assigned</span>
            <span className="folder-tab-count">{tabCounts.ALL}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'ASSIGNED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('ASSIGNED'); setCurrentPage(1); }}
          >
            <span>New Assigned</span>
            <span className="folder-tab-count count-new">{tabCounts.ASSIGNED}</span>
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
            className={`folder-tab ${activeTab === 'VENDOR_ASSIGNED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('VENDOR_ASSIGNED'); setCurrentPage(1); }}
          >
            <span>Vendor Dispatched</span>
            <span className="folder-tab-count">{tabCounts.VENDOR_ASSIGNED}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'RESOLVED' ? 'active' : ''}`}
            onClick={() => { setActiveTab('RESOLVED'); setCurrentPage(1); }}
          >
            <span>Resolved</span>
            <span className="folder-tab-count count-resolved">{tabCounts.RESOLVED}</span>
          </button>
        </div>

        {/* Search, Filter & Action Bar (Clean, no scrollbar, matching Admin Table) */}
        <div className="filter-controls-row">
          {/* Search Input */}
          <div className="filter-input-group">
            <span className="filter-label">Search</span>
            <div className="search-input-wrapper">
              <Search size={14} className="search-icon" />
              <input
                type="text"
                className="filter-text-input"
                placeholder="Enter token, description, venue..."
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setCurrentPage(1); }}
              />
            </div>
          </div>

          {/* Category Dropdown */}
          <div className="filter-input-group">
            <span className="filter-label">Category</span>
            <CustomSelect
              value={selectedCategory}
              onChange={(val) => { setSelectedCategory(val); setCurrentPage(1); }}
              options={categoryOptions}
              minWidth={130}
            />
          </div>

          {/* SLA Filter Dropdown */}
          <div className="filter-input-group">
            <span className="filter-label">SLA Status</span>
            <CustomSelect
              value={selectedSlaFilter}
              onChange={(val) => {
                setSelectedSlaFilter(val as 'ALL' | 'AT_RISK' | 'BREACHED');
                setCurrentPage(1);
              }}
              options={slaOptions}
              minWidth={140}
            />
          </div>

          {/* Priority Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Priority</span>
            <CustomSelect
              value={selectedPriority}
              onChange={(val) => {
                setSelectedPriority(val);
                setCurrentPage(1);
              }}
              options={priorityOptions}
              minWidth={125}
            />
          </div>

          {/* Action Buttons: Filter (Square on nav expand) & Export (Square on nav expand) */}
          <div className="filter-actions-group">
            <button
              className={`btn-filter-search ${showFilterDrawer || activeFiltersCount > 0 ? 'active' : ''}`}
              onClick={() => {
                setShowFilterDrawer((prev) => !prev);
                setCurrentPage(1);
              }}
              title={activeFiltersCount > 0 ? `${activeFiltersCount} active filter(s) — click to toggle options` : 'Toggle advanced filters'}
              aria-label="Filter"
            >
              <Filter size={13} />
              <span className="btn-filter-text">
                {activeFiltersCount > 0 ? `Filter (${activeFiltersCount})` : 'Filter'}
              </span>
              {activeFiltersCount > 0 && <span className="btn-filter-badge">{activeFiltersCount}</span>}
            </button>

            <button
              className="btn-filter-export"
              onClick={handleExportCSV}
              title="Export visible list to CSV"
              aria-label="Export"
            >
              <Download size={14} />
              <span>Export</span>
            </button>
          </div>
        </div>

        {/* Expandable Advanced Filter Drawer for Staff */}
        <AnimatePresence>
          {showFilterDrawer && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.22, ease: 'easeOut' }}
              className="staff-filter-drawer"
            >
              <div className="staff-filter-drawer-content">
                {/* 1. Venue / Building Filter */}
                <div className="drawer-filter-section">
                  <span className="drawer-section-label">Building / Venue:</span>
                  <div className="drawer-chips-list">
                    {venueOptions.map((venue) => (
                      <button
                        key={venue}
                        className={`drawer-filter-chip ${selectedLocation === venue ? 'active' : ''}`}
                        onClick={() => {
                          setSelectedLocation(venue);
                          setCurrentPage(1);
                        }}
                      >
                        {venue === 'ALL' ? 'All Buildings' : venue}
                      </button>
                    ))}
                  </div>
                </div>

                {/* 2. Quick Focus Filters */}
                <div className="drawer-filter-section">
                  <span className="drawer-section-label">Quick Focus:</span>
                  <div className="drawer-chips-list">
                    <button
                      className={`drawer-filter-chip ${quickFocus === 'ALL' ? 'active' : ''}`}
                      onClick={() => { setQuickFocus('ALL'); setCurrentPage(1); }}
                    >
                      All Tickets
                    </button>
                    <button
                      className={`drawer-filter-chip ${quickFocus === 'AT_RISK' ? 'active' : ''}`}
                      onClick={() => { setQuickFocus('AT_RISK'); setCurrentPage(1); }}
                    >
                      <AlertTriangle size={11} /> SLA At Risk (&lt;4h)
                    </button>
                    <button
                      className={`drawer-filter-chip ${quickFocus === 'HIGH_PRIORITY' ? 'active' : ''}`}
                      onClick={() => { setQuickFocus('HIGH_PRIORITY'); setCurrentPage(1); }}
                    >
                      <Zap size={11} /> High Priority (&gt;70)
                    </button>
                    <button
                      className={`drawer-filter-chip ${quickFocus === 'NEW' ? 'active' : ''}`}
                      onClick={() => { setQuickFocus('NEW'); setCurrentPage(1); }}
                    >
                      <Sparkles size={11} /> New Unhandled
                    </button>
                  </div>
                </div>

                {/* Drawer Footer with Result Count & Reset */}
                <div className="drawer-filter-footer">
                  <div className="drawer-filter-count-badge">
                    Matches: <strong>{filtered.length}</strong> of {complaints.length} tickets
                  </div>
                  {activeFiltersCount > 0 && (
                    <button
                      className="drawer-filter-reset-btn"
                      onClick={() => {
                        setSearchQuery('');
                        setSelectedCategory('ALL');
                        setSelectedSlaFilter('ALL');
                        setSelectedPriority('ALL');
                        setSelectedLocation('ALL');
                        setQuickFocus('ALL');
                        setCurrentPage(1);
                        addToast({
                          type: 'info',
                          title: 'Filters Reset',
                          message: 'All filter criteria have been cleared.',
                          duration: 3000,
                        });
                      }}
                    >
                      <X size={12} />
                      <span>Reset Filters</span>
                    </button>
                  )}
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Removable Active Filters Chip Bar */}
        {activeFiltersCount > 0 && (
          <div className="active-filters-bar">
            <span className="active-filters-title">Active Filters:</span>
            {searchQuery.trim() && (
              <span className="active-filter-tag">
                "{searchQuery}"
                <button
                  onClick={() => setSearchQuery('')}
                  title="Remove search filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            {selectedCategory !== 'ALL' && (
              <span className="active-filter-tag">
                Category: {selectedCategory}
                <button
                  onClick={() => setSelectedCategory('ALL')}
                  title="Remove category filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            {selectedSlaFilter !== 'ALL' && (
              <span className="active-filter-tag">
                SLA: {selectedSlaFilter === 'AT_RISK' ? 'At Risk' : 'Breached'}
                <button
                  onClick={() => setSelectedSlaFilter('ALL')}
                  title="Remove SLA filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            {selectedPriority !== 'ALL' && (
              <span className="active-filter-tag">
                Priority: {selectedPriority}
                <button
                  onClick={() => setSelectedPriority('ALL')}
                  title="Remove priority filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            {selectedLocation !== 'ALL' && (
              <span className="active-filter-tag">
                Venue: {selectedLocation}
                <button
                  onClick={() => setSelectedLocation('ALL')}
                  title="Remove venue filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            {quickFocus !== 'ALL' && (
              <span className="active-filter-tag">
                Focus: {quickFocus === 'AT_RISK' ? 'At Risk' : quickFocus === 'HIGH_PRIORITY' ? 'High Priority' : 'New'}
                <button
                  onClick={() => setQuickFocus('ALL')}
                  title="Remove focus filter"
                >
                  <X size={10} />
                </button>
              </span>
            )}
            <button
              className="btn-clear-all-pill"
              onClick={() => {
                setSearchQuery('');
                setSelectedCategory('ALL');
                setSelectedSlaFilter('ALL');
                setSelectedPriority('ALL');
                setSelectedLocation('ALL');
                setQuickFocus('ALL');
                setCurrentPage(1);
              }}
            >
              Clear all
            </button>
          </div>
        )}

        {/* Content Section: Modern High-Contrast Table (Matching Admin Portal) */}
        {isLoading ? (
          <div style={{ padding: '32px 24px' }}>
            <LoadingSkeleton rows={5} type="row" />
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state-container">
            <div className="empty-state-icon">
              <ClipboardList size={28} />
            </div>
            <div className="empty-state-title">No tickets match your filter criteria</div>
            <div className="empty-state-desc">
              Try adjusting your search query, or resetting category/status filters.
            </div>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => {
                setSearchQuery('');
                setSelectedCategory('ALL');
                setSelectedSlaFilter('ALL');
                setSelectedPriority('ALL');
                setActiveTab('ALL');
              }}
            >
              Reset All Filters
            </button>
          </div>
        ) : (
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
                      role="staff"
                    />
                  ))}
                </AnimatePresence>
              </tbody>
            </table>
          </div>
        )}

        {/* Bottom Pagination Bar (Matching Admin Table Pagination) */}
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
    </div>
  );
};

export default StaffQueue;
