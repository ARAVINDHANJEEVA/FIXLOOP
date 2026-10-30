import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { AnimatePresence } from 'framer-motion';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Complaint, Category } from '../../types';
import { complaintApi, staffApi, categoryApi, WorkloadData } from '../../api';
import { useAuth } from '../../hooks/useAuth';
import {
  Briefcase,
  CheckCircle,
  Clock,
  Activity,
  Search,
  Download,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  Award,
} from 'lucide-react';
import ComplaintRow from '../../components/complaint/ComplaintRow';
import CustomSelect, { SelectOption } from '../../components/ui/CustomSelect';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import ErrorState from '../../components/ui/ErrorState';
import { useToast } from '../../hooks/useToast';

type WorkloadTab = 'OVERVIEW' | 'ACTIVE_TICKETS' | 'RESOLVED_HISTORY';

const Workload: React.FC = () => {
  const { user } = useAuth();
  const { addToast } = useToast();
  const [activeTab, setActiveTab] = useState<WorkloadTab>('OVERVIEW');
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [workloadData, setWorkloadData] = useState<WorkloadData | null>(null);
  const [categories, setCategories] = useState<Category[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedPriority, setSelectedPriority] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 6;

  const loadWorkload = useCallback(async () => {
    try {
      setIsLoading(true);
      setIsError(false);
      const [queueRes, workloadRes, catsRes] = await Promise.all([
        complaintApi.getStaffQueue({ size: 100 }),
        staffApi.getWorkload(),
        categoryApi.getAll(),
      ]);

      setComplaints(queueRes.content || []);
      if (workloadRes) {
        setWorkloadData(workloadRes);
      }
      setCategories(catsRes || []);
    } catch {
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadWorkload();
  }, [loadWorkload]);

  const categoryOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Categories' },
    ...categories
      .filter((c) => !c.sensitive)
      .map((cat) => ({ value: cat.name, label: cat.name })),
  ], [categories]);

  const priorityOptions: SelectOption[] = useMemo(() => [
    { value: 'ALL', label: 'All Priorities' },
    { value: 'HIGH', label: 'High (Score > 70)' },
    { value: 'MEDIUM', label: 'Medium (Score 40–70)' },
    { value: 'LOW', label: 'Low (Score < 40)' },
  ], []);

  const myStaff = useMemo(() => {
    return {
      id: user?.id || '0',
      name: workloadData?.name || user?.name || 'Technician',
      role: 'Staff',
      departmentId: '1',
      departmentName: 'Campus Operations',
      skillTags: [],
      status: 'ACTIVE' as const,
      currentWeightedLoad: Number(workloadData?.currentWeightedLoad) || 0,
      avatarInitials: user?.avatarInitials || 'ST',
      email: user?.email || '',
    };
  }, [user, workloadData]);

  const openTickets = useMemo(
    () => complaints.filter((c) => !['RESOLVED', 'CLOSED'].includes(c.status)),
    [complaints]
  );
  const resolvedTickets = useMemo(
    () => complaints.filter((c) => ['RESOLVED', 'CLOSED'].includes(c.status)),
    [complaints]
  );

  const avgResolutionHours = workloadData?.avgResolutionHours && workloadData.avgResolutionHours > 0
    ? workloadData.avgResolutionHours.toFixed(1)
    : '0.0';

  const statusChartData = [
    { status: 'Assigned', count: complaints.filter((c) => c.status === 'ASSIGNED').length, color: '#10b981' },
    { status: 'In Progress', count: complaints.filter((c) => c.status === 'IN_PROGRESS').length, color: '#fbbf24' },
    { status: 'Awaiting Info', count: complaints.filter((c) => c.status === 'AWAITING_STUDENT_INFO').length, color: '#fb923c' },
    { status: 'Vendor Dispatched', count: complaints.filter((c) => c.status === 'VENDOR_ASSIGNED').length, color: '#a855f7' },
    { status: 'Resolved', count: complaints.filter((c) => c.status === 'RESOLVED').length, color: '#22c55e' },
  ];

  // Active tickets filter
  const filteredActive = useMemo(() => {
    return openTickets.filter((c) => {
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matches =
          c.token.toLowerCase().includes(q) ||
          c.description.toLowerCase().includes(q) ||
          c.location.building.toLowerCase().includes(q) ||
          c.categoryName.toLowerCase().includes(q);
        if (!matches) return false;
      }
      if (selectedCategory !== 'ALL' && c.categoryId !== selectedCategory && c.categoryName !== selectedCategory) {
        return false;
      }
      if (selectedPriority === 'HIGH' && c.priorityScore < 70) return false;
      if (selectedPriority === 'MEDIUM' && (c.priorityScore < 40 || c.priorityScore >= 70)) return false;
      if (selectedPriority === 'LOW' && c.priorityScore >= 40) return false;

      return true;
    });
  }, [openTickets, searchQuery, selectedCategory, selectedPriority]);

  // Resolved tickets filter
  const filteredResolved = useMemo(() => {
    return resolvedTickets.filter((c) => {
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matches =
          c.token.toLowerCase().includes(q) ||
          c.description.toLowerCase().includes(q) ||
          c.location.building.toLowerCase().includes(q) ||
          c.categoryName.toLowerCase().includes(q);
        if (!matches) return false;
      }
      if (selectedCategory !== 'ALL' && c.categoryId !== selectedCategory && c.categoryName !== selectedCategory) {
        return false;
      }
      return true;
    });
  }, [resolvedTickets, searchQuery, selectedCategory]);

  const currentDataset = activeTab === 'ACTIVE_TICKETS' ? filteredActive : filteredResolved;
  const totalPages = Math.ceil(currentDataset.length / itemsPerPage) || 1;
  const paginatedComplaints = useMemo(() => {
    const start = (currentPage - 1) * itemsPerPage;
    return currentDataset.slice(start, start + itemsPerPage);
  }, [currentDataset, currentPage]);

  const handleExportCSV = () => {
    const dataToExport = activeTab === 'ACTIVE_TICKETS' ? filteredActive : filteredResolved;
    const headers = ['Token', 'Category', 'Description', 'Building', 'Room', 'Priority', 'Status', 'SLA Deadline'];
    const rows = dataToExport.map((c) => [
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
    link.setAttribute('download', `FixLoop_Workload_${activeTab}_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    addToast({
      type: 'success',
      title: 'Data Exported',
      message: `${dataToExport.length} tickets exported to CSV.`,
    });
  };

  return (
    <div className="admin-queue-page">
      {/* 1. Header (Matching Admin & Staff Queue Design) */}
      <div className="admin-page-header">
        <div>
          <h1 className="admin-header-title">Technician Workload & Performance</h1>
          <p className="admin-header-subtitle">
            Capacity tracking, assigned ticket triage, SLA resolution speed, and department benchmark metrics.
          </p>
        </div>

        <div className="admin-header-stats-chips">
          <div className="stats-mini-pill">
            <Briefcase size={14} style={{ color: 'var(--accent)' }} />
            <span>Open Tickets: <strong>{openTickets.length}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <Activity size={14} style={{ color: 'var(--warning)' }} />
            <span>Capacity Load: <strong>{myStaff.currentWeightedLoad}%</strong></span>
          </div>
          <div className="stats-mini-pill">
            <CheckCircle size={14} style={{ color: 'var(--success)' }} />
            <span>Resolved: <strong>{resolvedTickets.length}</strong></span>
          </div>
          <div className="stats-mini-pill">
            <Clock size={14} style={{ color: 'var(--purple)' }} />
            <span>Avg Speed: <strong>{avgResolutionHours}h</strong></span>
          </div>
        </div>
      </div>

      {/* 2. Unified Management Card with Folder Tabs */}
      <div className="main-management-card">
        {/* Modern Folder-style Tabs Bar */}
        <div className="folder-tabs-bar">
          <button
            className={`folder-tab ${activeTab === 'OVERVIEW' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('OVERVIEW');
              setCurrentPage(1);
            }}
          >
            <span>Workload Overview</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'ACTIVE_TICKETS' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('ACTIVE_TICKETS');
              setCurrentPage(1);
            }}
          >
            <span>Active Assignments</span>
            <span className="folder-tab-count count-new">{openTickets.length}</span>
          </button>

          <button
            className={`folder-tab ${activeTab === 'RESOLVED_HISTORY' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('RESOLVED_HISTORY');
              setCurrentPage(1);
            }}
          >
            <span>Resolved Tickets</span>
            <span className="folder-tab-count count-resolved">{resolvedTickets.length}</span>
          </button>
        </div>

        {/* Error State */}
        {isError && (
          <div style={{ padding: '32px 24px' }}>
            <ErrorState
              title="Failed to Load Workload"
              message="Could not load your staff workload metrics and assigned complaints from the server. Please check your network connection."
              onRetry={loadWorkload}
            />
          </div>
        )}

        {/* Tab 1: Workload Overview */}
        {!isError && activeTab === 'OVERVIEW' && (
          <div style={{ padding: '22px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Quick Metrics Strip */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14 }}>
              {[
                {
                  label: 'Active Assigned Tickets',
                  value: openTickets.length,
                  delta: 'Requires resolution',
                  icon: <Briefcase size={16} />,
                  color: 'var(--accent)',
                },
                {
                  label: 'Weighted Capacity Load',
                  value: `${myStaff.currentWeightedLoad}%`,
                  delta: 'Safe operating zone (<85%)',
                  icon: <Activity size={16} />,
                  color: 'var(--warning)',
                },
                {
                  label: 'Resolved This Month',
                  value: resolvedTickets.length,
                  delta: 'Target: 20 tickets',
                  icon: <CheckCircle size={16} />,
                  color: 'var(--success)',
                },
                {
                  label: 'Avg Resolution Speed',
                  value: `${avgResolutionHours}h`,
                  delta: 'Dept benchmark: 18.4h',
                  icon: <Clock size={16} />,
                  color: 'var(--purple)',
                },
              ].map((m) => (
                <div
                  key={m.label}
                  style={{
                    padding: '16px 18px',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    borderRadius: 14,
                    boxShadow: 'var(--card-shadow)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                    <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                      {m.label}
                    </span>
                    <div style={{ color: m.color }}>{m.icon}</div>
                  </div>
                  <div style={{ fontSize: '1.55rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 3 }}>
                    {m.value}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <TrendingUp size={12} style={{ color: 'var(--success)' }} />
                    <span>{m.delta}</span>
                  </div>
                </div>
              ))}
            </div>

            {/* Charts Grid */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: 20 }}>
              {/* Tickets by Status Bar Chart */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 16,
                  padding: '20px 22px',
                  boxShadow: 'var(--card-shadow)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                  <div style={{ fontSize: '0.88rem', fontWeight: 800, color: 'var(--text-1)' }}>
                    Assigned Tickets by Status
                  </div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-3)', fontWeight: 600 }}>Live Distribution</span>
                </div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginBottom: 16 }}>
                  Current breakdown across lifecycle pipeline stages
                </div>
                <ResponsiveContainer width="100%" height={230}>
                  <BarChart data={statusChartData} margin={{ top: 8, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
                    <XAxis
                      dataKey="status"
                      tick={{ fill: 'var(--text-3)', fontSize: 11 }}
                      axisLine={{ stroke: 'var(--border)' }}
                      tickLine={false}
                    />
                    <YAxis
                      tick={{ fill: 'var(--text-3)', fontSize: 11 }}
                      axisLine={false}
                      tickLine={false}
                      allowDecimals={false}
                    />
                    <Tooltip
                      contentStyle={{
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border-strong)',
                        borderRadius: 10,
                        fontSize: 12,
                        boxShadow: 'var(--card-shadow)',
                      }}
                      labelStyle={{ color: 'var(--text-1)', fontWeight: 700 }}
                    />
                    <Bar dataKey="count" name="Tickets" radius={[6, 6, 0, 0]}>
                      {statusChartData.map((entry, i) => (
                        <Cell key={i} fill={entry.color} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* Performance Metrics vs Dept Average */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 16,
                  padding: '20px 22px',
                  boxShadow: 'var(--card-shadow)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                  <div style={{ fontSize: '0.88rem', fontWeight: 800, color: 'var(--text-1)' }}>
                    Personal KPIs vs Department Benchmarks
                  </div>
                  <Award size={16} style={{ color: 'var(--accent)' }} />
                </div>
                <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginBottom: 18 }}>
                  Staff velocity evaluation against IT Operations standard benchmarks
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                  {[
                    { label: 'SLA Target Compliance', mine: 82, avg: 87, color: 'var(--warning)', unit: '%' },
                    { label: 'Avg Turnaround Speed', mine: Number(avgResolutionHours), avg: 18.4, color: 'var(--success)', unit: 'h' },
                    { label: 'Monthly Closed Volume', mine: 19, avg: 22, color: 'var(--accent)', unit: ' tickets' },
                    { label: 'Current Capacity Load', mine: myStaff.currentWeightedLoad, avg: 30, color: 'var(--orange)', unit: '%' },
                  ].map((metric) => {
                    const myPct = Math.min(100, (metric.mine / (metric.avg * 1.4)) * 100);
                    const avgPct = Math.min(100, (metric.avg / (metric.avg * 1.4)) * 100);

                    return (
                      <div key={metric.label}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', marginBottom: 5 }}>
                          <span style={{ color: 'var(--text-2)', fontWeight: 600 }}>{metric.label}</span>
                          <span style={{ fontWeight: 700, color: 'var(--text-1)' }}>
                            {metric.mine}{metric.unit}
                          </span>
                        </div>
                        <div style={{ position: 'relative', height: 7, background: 'var(--border)', borderRadius: 999 }}>
                          <div
                            style={{
                              position: 'absolute',
                              height: '100%',
                              width: `${myPct}%`,
                              background: metric.color,
                              borderRadius: 999,
                              transition: 'width 0.6s ease',
                            }}
                          />
                          <div
                            style={{
                              position: 'absolute',
                              top: -3,
                              width: 2,
                              height: 13,
                              background: 'var(--text-1)',
                              borderRadius: 1,
                              left: `${avgPct}%`,
                            }}
                            title={`Dept Average: ${metric.avg}${metric.unit}`}
                          />
                        </div>
                        <div style={{ fontSize: '0.68rem', color: 'var(--text-3)', marginTop: 4 }}>
                          Dept Target Average: {metric.avg}{metric.unit}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Active Assignments (Full Themed Table matching Admin Portal) */}
        {!isError && activeTab === 'ACTIVE_TICKETS' && (
          <>
            {/* Filter Bar */}
            <div className="filter-controls-row">
              <div className="filter-input-group">
                <span className="filter-label">Search</span>
                <div className="search-input-wrapper">
                  <Search size={14} className="search-icon" />
                  <input
                    type="text"
                    className="filter-text-input"
                    placeholder="Search active tickets..."
                    value={searchQuery}
                    onChange={(e) => {
                      setSearchQuery(e.target.value);
                      setCurrentPage(1);
                    }}
                  />
                </div>
              </div>

              <div className="filter-input-group">
                <span className="filter-label">Category</span>
                <CustomSelect
                  value={selectedCategory}
                  onChange={(val) => {
                    setSelectedCategory(val);
                    setCurrentPage(1);
                  }}
                  options={categoryOptions}
                  minWidth={140}
                />
              </div>

              <div className="filter-input-group">
                <span className="filter-label">Priority</span>
                <CustomSelect
                  value={selectedPriority}
                  onChange={(val) => {
                    setSelectedPriority(val);
                    setCurrentPage(1);
                  }}
                  options={priorityOptions}
                  minWidth={140}
                />
              </div>

              <div className="filter-actions-group">
                <button
                  className="btn-filter-export"
                  onClick={handleExportCSV}
                  title="Export active tickets"
                >
                  <Download size={14} />
                  <span>Export</span>
                </button>
              </div>
            </div>

            {/* Table */}
            {isLoading ? (
              <div style={{ padding: '24px' }}>
                <LoadingSkeleton rows={5} type="card" />
              </div>
            ) : filteredActive.length === 0 ? (
              <div className="empty-state-container">
                <div className="empty-state-icon">
                  <CheckCircle size={28} style={{ color: 'var(--success)' }} />
                </div>
                <div className="empty-state-title">No active tickets pending</div>
                <p className="empty-state-desc">Your active queue is completely clear!</p>
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
                        <ComplaintRow key={c.id} complaint={c} role="staff" />
                      ))}
                    </AnimatePresence>
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            {filteredActive.length > 0 && (
              <div className="management-pagination-bar">
                <div className="pagination-info">
                  Showing {(currentPage - 1) * itemsPerPage + 1}–{Math.min(currentPage * itemsPerPage, filteredActive.length)} of {filteredActive.length} active tickets
                </div>
                <div className="pagination-controls">
                  <button
                    className="pagination-btn arrow-btn"
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
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
                  >
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}

        {/* Tab 3: Resolved Tickets (Full Themed Table matching Admin Portal) */}
        {!isError && activeTab === 'RESOLVED_HISTORY' && (
          <>
            {/* Filter Bar */}
            <div className="filter-controls-row">
              <div className="filter-input-group">
                <span className="filter-label">Search</span>
                <div className="search-input-wrapper">
                  <Search size={14} className="search-icon" />
                  <input
                    type="text"
                    className="filter-text-input"
                    placeholder="Search resolved tickets..."
                    value={searchQuery}
                    onChange={(e) => {
                      setSearchQuery(e.target.value);
                      setCurrentPage(1);
                    }}
                  />
                </div>
              </div>

              <div className="filter-input-group">
                <span className="filter-label">Category</span>
                <CustomSelect
                  value={selectedCategory}
                  onChange={(val) => {
                    setSelectedCategory(val);
                    setCurrentPage(1);
                  }}
                  options={categoryOptions}
                  minWidth={140}
                />
              </div>

              <div className="filter-actions-group">
                <button
                  className="btn-filter-export"
                  onClick={handleExportCSV}
                  title="Export resolved tickets"
                >
                  <Download size={14} />
                  <span>Export</span>
                </button>
              </div>
            </div>

            {/* Table */}
            {isLoading ? (
              <div style={{ padding: '24px' }}>
                <LoadingSkeleton rows={5} type="card" />
              </div>
            ) : filteredResolved.length === 0 ? (
              <div className="empty-state-container">
                <div className="empty-state-icon">
                  <Clock size={28} />
                </div>
                <div className="empty-state-title">No resolved tickets found</div>
                <p className="empty-state-desc">Completed tickets will appear in this history log.</p>
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
                        <ComplaintRow key={c.id} complaint={c} role="staff" />
                      ))}
                    </AnimatePresence>
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination */}
            {filteredResolved.length > 0 && (
              <div className="management-pagination-bar">
                <div className="pagination-info">
                  Showing {(currentPage - 1) * itemsPerPage + 1}–{Math.min(currentPage * itemsPerPage, filteredResolved.length)} of {filteredResolved.length} resolved tickets
                </div>
                <div className="pagination-controls">
                  <button
                    className="pagination-btn arrow-btn"
                    disabled={currentPage === 1}
                    onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
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
                  >
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default Workload;
