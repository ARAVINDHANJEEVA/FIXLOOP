import React, { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  AreaChart,
  Area,
  LineChart,
  Line,
} from 'recharts';
import {
  BarChart3,
  TrendingUp,
  Users,
  ClipboardCheck,
  Clock,
  Target,
  Award,
  PieChart as PieChartIcon,
  ShieldAlert,
  Download,
  MapPin,
  CheckCircle2,
  Zap,
  Search,
  Building,
  Sparkles,
} from 'lucide-react';
import {
  mockHotspotData,
  mockSLATrends,
  mockCategoryVolume,
  mockStaffPerformance,
  mockDepartments,
} from '../../data/mockData';
import { useToast } from '../../hooks/useToast';

type AnalyticsTab = 'OVERVIEW' | 'HOTSPOTS' | 'STAFF_VELOCITY' | 'SLA_DEEP_DIVE';
type DateRangePreset = '7d' | '30d' | '90d' | 'ytd' | 'custom';
type ChartType = 'area' | 'bar' | 'line';
type MetricFocus = 'volume' | 'resolutionTime' | 'slaCompliance';

const RADIAN = Math.PI / 180;
const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent }: any) => {
  if (percent < 0.06) return null;
  const radius = innerRadius + (outerRadius - innerRadius) * 0.55;
  const x = cx + radius * Math.cos(-midAngle * RADIAN);
  const y = cy + radius * Math.sin(-midAngle * RADIAN);
  return (
    <text x={x} y={y} fill="#ffffff" textAnchor="middle" dominantBaseline="central" fontSize={11} fontWeight={800}>
      {`${(percent * 100).toFixed(0)}%`}
    </text>
  );
};

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div
      style={{
        background: 'var(--bg-elevated)',
        border: '1px solid var(--border)',
        borderRadius: 10,
        padding: '10px 14px',
        boxShadow: 'var(--card-shadow)',
        fontSize: '0.8rem',
      }}
    >
      <div style={{ fontSize: '0.8rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 6 }}>
        {label}
      </div>
      {payload.map((p: any) => (
        <div key={p.name} style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.76rem', color: 'var(--text-2)', marginBottom: 3 }}>
          <span style={{ width: 8, height: 8, borderRadius: '50%', background: p.color || p.fill || p.stroke }} />
          <span>{p.name}:</span>
          <strong style={{ color: 'var(--text-1)', marginLeft: 'auto' }}>
            {p.value} {p.unit || ''}
          </strong>
        </div>
      ))}
    </div>
  );
};

const Analytics: React.FC = () => {
  const { addToast } = useToast();

  // Primary Navigation
  const [activeTab, setActiveTab] = useState<AnalyticsTab>('OVERVIEW');

  // Customization & Filter States
  const [dateRange, setDateRange] = useState<DateRangePreset>('30d');
  const [startDate, setStartDate] = useState('2026-08-01');
  const [endDate, setEndDate] = useState('2026-09-03');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedDepartment, setSelectedDepartment] = useState('ALL');
  const [chartType, setChartType] = useState<ChartType>('area');
  const [metricFocus, setMetricFocus] = useState<MetricFocus>('volume');
  const [staffSearch, setStaffSearch] = useState('');
  const [staffSort, setStaffSort] = useState<'compliance' | 'hours' | 'resolved'>('compliance');

  // Multiplier based on date range to simulate dynamic recalculations
  const rangeMultiplier = useMemo(() => {
    switch (dateRange) {
      case '7d':
        return 0.35;
      case '30d':
        return 1.0;
      case '90d':
        return 2.8;
      case 'ytd':
        return 6.5;
      case 'custom':
        return 1.2;
      default:
        return 1.0;
    }
  }, [dateRange]);

  // Scaled summary metrics
  const summaryKPIs = useMemo(() => {
    return {
      total: Math.round(144 * rangeMultiplier),
      resolved: Math.round(118 * rangeMultiplier),
      avgTime: rangeMultiplier < 1 ? '12.4h' : '15.8h',
      compliance: rangeMultiplier > 2 ? '89.2%' : '91.4%',
      csat: '4.7 ★',
      firstTimeFix: '78.5%',
    };
  }, [rangeMultiplier]);

  // Category distribution data adjusted by category selection
  const filteredCategoryData = useMemo(() => {
    if (selectedCategory === 'ALL') return mockCategoryVolume;
    return mockCategoryVolume.filter((c) => c.name === selectedCategory);
  }, [selectedCategory]);

  // SLA Trend data
  const trendData = useMemo(() => {
    return mockSLATrends.map((item) => ({
      ...item,
      resolved: Math.round(item.resolved * rangeMultiplier),
      atRisk: Math.round(item.atRisk * (rangeMultiplier * 0.9)),
      breached: Math.round(item.breached * (rangeMultiplier * 0.8)),
    }));
  }, [rangeMultiplier]);

  // Filtered staff performance
  const filteredStaff = useMemo(() => {
    let list = mockStaffPerformance.filter((s) => {
      const matchesSearch = s.staffName.toLowerCase().includes(staffSearch.toLowerCase());
      const matchesDept = selectedDepartment === 'ALL' || s.department === selectedDepartment;
      return matchesSearch && matchesDept;
    });

    return list.sort((a, b) => {
      if (staffSort === 'compliance') return b.slaCompliancePercent - a.slaCompliancePercent;
      if (staffSort === 'hours') return a.avgResolutionHours - b.avgResolutionHours;
      return b.resolvedThisMonth - a.resolvedThisMonth;
    });
  }, [staffSearch, selectedDepartment, staffSort]);

  // Export analytics data to CSV
  const handleExportCSV = () => {
    const headers = ['Metric/Staff', 'Category/Dept', 'Value', 'Compliance/Status', 'Period'];
    const rows = filteredStaff.map((s) => [
      `"${s.staffName}"`,
      `"${s.department}"`,
      `${s.resolvedThisMonth} resolved`,
      `${s.slaCompliancePercent}% SLA`,
      dateRange,
    ]);

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows.map((e) => e.join(','))].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `FixLoop_Analytics_Report_${dateRange}_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    addToast({
      type: 'success',
      title: 'Report Downloaded',
      message: `Analytics data exported for period: ${dateRange.toUpperCase()}`,
    });
  };

  const handleFlagInspection = (location: string, count: number) => {
    addToast({
      type: 'warning',
      title: 'Preventive Inspection Scheduled',
      message: `Campus inspection request dispatched for ${location} (${count} recent incidents). Maintenance team alerted.`,
      duration: 6000,
    });
  };

  return (
    <div className="admin-queue-page">
      {/* 1. Page Header with Title & Action Controls */}
      <div className="page-header" style={{ marginBottom: 18 }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
              <h1 className="page-title" style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
                <BarChart3 size={24} style={{ color: 'var(--accent)' }} />
                Analytics & Campus Intelligence
              </h1>
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: 5,
                  padding: '3px 9px',
                  borderRadius: 999,
                  background: 'var(--accent-dim)',
                  border: '1px solid rgba(37, 99, 235, 0.25)',
                  color: 'var(--accent)',
                  fontSize: '0.72rem',
                  fontWeight: 800,
                }}
              >
                <Sparkles size={11} />
                REAL-TIME TELEMETRY
              </span>
            </div>
            <p className="page-subtitle">
              Comprehensive operational velocity, SLA compliance metrics, hotspot forecasts, and technician performance
            </p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            <button className="btn-filter-export" onClick={handleExportCSV}>
              <Download size={14} />
              <span>Export CSV Report</span>
            </button>
          </div>
        </div>
      </div>

      {/* 2. Interactive KPI Scorecard Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: 12,
          marginBottom: 18,
        }}
      >
        {[
          {
            label: 'Total Complaints',
            value: summaryKPIs.total,
            delta: '+12% vs last cycle',
            positive: false,
            icon: <ClipboardCheck size={16} />,
            color: 'var(--accent)',
          },
          {
            label: 'Total Resolved',
            value: summaryKPIs.resolved,
            delta: '+16% completion rate',
            positive: true,
            icon: <Award size={16} />,
            color: 'var(--success)',
          },
          {
            label: 'Mean Time to Resolution',
            value: summaryKPIs.avgTime,
            delta: '-2.4h turnaround',
            positive: true,
            icon: <Clock size={16} />,
            color: 'var(--warning)',
          },
          {
            label: 'SLA Compliance Rate',
            value: summaryKPIs.compliance,
            delta: '+3.1% threshold',
            positive: true,
            icon: <Target size={16} />,
            color: '#a855f7',
          },
          {
            label: 'Student Rating (CSAT)',
            value: summaryKPIs.csat,
            delta: '94% positive sentiment',
            positive: true,
            icon: <CheckCircle2 size={16} />,
            color: '#f59e0b',
          },
          {
            label: 'First-Touch Resolution',
            value: summaryKPIs.firstTimeFix,
            delta: 'No repeat escalation',
            positive: true,
            icon: <Zap size={16} />,
            color: '#06b6d4',
          },
        ].map((kpi, i) => (
          <motion.div
            key={kpi.label}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.04 }}
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border)',
              borderRadius: 14,
              padding: '14px 16px',
              boxShadow: 'var(--card-shadow)',
              position: 'relative',
              overflow: 'hidden',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
              <span style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                {kpi.label}
              </span>
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 8,
                  background: `${kpi.color}15`,
                  color: kpi.color,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                {kpi.icon}
              </div>
            </div>

            <div style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--text-1)', letterSpacing: '-0.02em', marginBottom: 4 }}>
              {kpi.value}
            </div>

            <div
              style={{
                fontSize: '0.7rem',
                fontWeight: 700,
                color: kpi.positive ? 'var(--success)' : 'var(--warning)',
                display: 'flex',
                alignItems: 'center',
                gap: 4,
              }}
            >
              <TrendingUp size={11} />
              {kpi.delta}
            </div>
          </motion.div>
        ))}
      </div>

      {/* 3. Main Unified Management Card with Customization System */}
      <div className="main-management-card">
        {/* Interactive Folder Tabs */}
        <div className="folder-tabs-bar">
          <div
            className={`folder-tab ${activeTab === 'OVERVIEW' ? 'active' : ''}`}
            onClick={() => setActiveTab('OVERVIEW')}
          >
            <TrendingUp size={15} />
            <span>Executive Overview</span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'HOTSPOTS' ? 'active' : ''}`}
            onClick={() => setActiveTab('HOTSPOTS')}
          >
            <MapPin size={15} style={{ color: 'var(--orange)' }} />
            <span>Campus Hotspots</span>
            <span className="folder-tab-badge">4 zones</span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'STAFF_VELOCITY' ? 'active' : ''}`}
            onClick={() => setActiveTab('STAFF_VELOCITY')}
          >
            <Users size={15} style={{ color: 'var(--accent)' }} />
            <span>Staff & Team Velocity</span>
            <span className="folder-tab-badge">{filteredStaff.length}</span>
          </div>

          <div
            className={`folder-tab ${activeTab === 'SLA_DEEP_DIVE' ? 'active' : ''}`}
            onClick={() => setActiveTab('SLA_DEEP_DIVE')}
          >
            <Clock size={15} style={{ color: 'var(--purple)' }} />
            <span>SLA & Breach Forecast</span>
          </div>
        </div>

        {/* Dynamic Multi-Option Customization Toolbar */}
        <div className="filter-controls-row" style={{ flexWrap: 'wrap', gap: 10 }}>
          {/* Preset Date Range Buttons */}
          <div className="filter-input-group">
            <span className="filter-label">Timeframe</span>
            <div style={{ display: 'flex', gap: 4 }}>
              {[
                { id: '7d', label: '7D' },
                { id: '30d', label: '30D' },
                { id: '90d', label: '90D' },
                { id: 'ytd', label: 'YTD' },
                { id: 'custom', label: 'Custom' },
              ].map((p) => (
                <button
                  key={p.id}
                  className={`btn btn-sm ${dateRange === p.id ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ fontSize: '0.72rem', padding: '4px 9px', borderRadius: 8 }}
                  onClick={() => setDateRange(p.id as DateRangePreset)}
                >
                  {p.label}
                </button>
              ))}
            </div>
          </div>

          {/* Custom Date Range Pickers (Shows if Custom selected) */}
          {dateRange === 'custom' && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <div className="filter-input-group">
                <span className="filter-label">From</span>
                <input
                  type="date"
                  className="filter-date-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
              </div>
              <div className="filter-input-group">
                <span className="filter-label">To</span>
                <input
                  type="date"
                  className="filter-date-input"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
              </div>
            </div>
          )}

          {/* Category Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Category</span>
            <select
              className="filter-select-input"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="ALL">All Categories</option>
              <option value="Hostel">Hostel</option>
              <option value="Food">Food</option>
              <option value="Tech Support">Tech Support</option>
              <option value="Hygiene">Hygiene</option>
              <option value="Academic">Academic</option>
              <option value="Health">Health</option>
              <option value="Harassment">Harassment</option>
            </select>
          </div>

          {/* Department Filter */}
          <div className="filter-input-group">
            <span className="filter-label">Department</span>
            <select
              className="filter-select-input"
              value={selectedDepartment}
              onChange={(e) => setSelectedDepartment(e.target.value)}
            >
              <option value="ALL">All Departments</option>
              {mockDepartments.map((d) => (
                <option key={d.id} value={d.name}>
                  {d.name}
                </option>
              ))}
            </select>
          </div>

          {/* Chart Style Toggle */}
          <div className="filter-input-group">
            <span className="filter-label">Chart Type</span>
            <div style={{ display: 'flex', gap: 4 }}>
              <button
                className={`btn btn-sm ${chartType === 'area' ? 'btn-primary' : 'btn-secondary'}`}
                style={{ fontSize: '0.72rem', padding: '4px 8px', borderRadius: 8 }}
                onClick={() => setChartType('area')}
                title="Area Curve"
              >
                Area
              </button>
              <button
                className={`btn btn-sm ${chartType === 'bar' ? 'btn-primary' : 'btn-secondary'}`}
                style={{ fontSize: '0.72rem', padding: '4px 8px', borderRadius: 8 }}
                onClick={() => setChartType('bar')}
                title="Column Bar"
              >
                Bar
              </button>
              <button
                className={`btn btn-sm ${chartType === 'line' ? 'btn-primary' : 'btn-secondary'}`}
                style={{ fontSize: '0.72rem', padding: '4px 8px', borderRadius: 8 }}
                onClick={() => setChartType('line')}
                title="Line Trend"
              >
                Line
              </button>
            </div>
          </div>

          {/* Metric Mode Filter */}
          <div className="filter-input-group" style={{ marginLeft: 'auto' }}>
            <span className="filter-label">Focus</span>
            <select
              className="filter-select-input"
              value={metricFocus}
              onChange={(e) => setMetricFocus(e.target.value as MetricFocus)}
            >
              <option value="volume">Ticket Volume</option>
              <option value="resolutionTime">Resolution Speed (hrs)</option>
              <option value="slaCompliance">SLA Compliance Rate</option>
            </select>
          </div>
        </div>

        {/* Tab 1: Executive Overview */}
        {activeTab === 'OVERVIEW' && (
          <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: 20 }}>
              {/* Chart 1: SLA Breach & Resolution Trend */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 14,
                  padding: '18px 20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.86rem', fontWeight: 800, color: 'var(--text-1)' }}>
                      <TrendingUp size={16} style={{ color: 'var(--accent)' }} />
                      <span>Resolution vs Breach Trajectory</span>
                    </div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginTop: 2 }}>
                      Monthly ticket volume resolved comfortably vs tickets at-risk and breached
                    </div>
                  </div>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      padding: '3px 8px',
                      borderRadius: 999,
                      background: 'rgba(34, 197, 94, 0.12)',
                      color: 'var(--success)',
                    }}
                  >
                    +18.4% Efficiency
                  </span>
                </div>

                <ResponsiveContainer width="100%" height={260}>
                  {chartType === 'area' ? (
                    <AreaChart data={trendData}>
                      <defs>
                        <linearGradient id="grad-resolved" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#22c55e" stopOpacity={0.35} />
                          <stop offset="95%" stopColor="#22c55e" stopOpacity={0} />
                        </linearGradient>
                        <linearGradient id="grad-breached" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#ef4444" stopOpacity={0.35} />
                          <stop offset="95%" stopColor="#ef4444" stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                      <XAxis dataKey="period" tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <YAxis tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <Tooltip content={<CustomTooltip />} />
                      <Legend wrapperStyle={{ fontSize: 11, color: 'var(--text-2)' }} />
                      <Area type="monotone" dataKey="resolved" name="Resolved" stroke="#22c55e" fill="url(#grad-resolved)" strokeWidth={2.5} />
                      <Area type="monotone" dataKey="atRisk" name="At Risk" stroke="#f59e0b" fill="transparent" strokeWidth={2} strokeDasharray="4 2" />
                      <Area type="monotone" dataKey="breached" name="Breached" stroke="#ef4444" fill="url(#grad-breached)" strokeWidth={2} />
                    </AreaChart>
                  ) : chartType === 'bar' ? (
                    <BarChart data={trendData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                      <XAxis dataKey="period" tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <YAxis tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <Tooltip content={<CustomTooltip />} />
                      <Legend wrapperStyle={{ fontSize: 11, color: 'var(--text-2)' }} />
                      <Bar dataKey="resolved" name="Resolved" fill="#22c55e" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="atRisk" name="At Risk" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="breached" name="Breached" fill="#ef4444" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  ) : (
                    <LineChart data={trendData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                      <XAxis dataKey="period" tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <YAxis tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                      <Tooltip content={<CustomTooltip />} />
                      <Legend wrapperStyle={{ fontSize: 11, color: 'var(--text-2)' }} />
                      <Line type="monotone" dataKey="resolved" name="Resolved" stroke="#22c55e" strokeWidth={3} dot={{ r: 3 }} />
                      <Line type="monotone" dataKey="atRisk" name="At Risk" stroke="#f59e0b" strokeWidth={2} strokeDasharray="4 2" />
                      <Line type="monotone" dataKey="breached" name="Breached" stroke="#ef4444" strokeWidth={2} dot={{ r: 3 }} />
                    </LineChart>
                  )}
                </ResponsiveContainer>
              </div>

              {/* Chart 2: Category Distribution Pie */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 14,
                  padding: '18px 20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.86rem', fontWeight: 800, color: 'var(--text-1)' }}>
                    <PieChartIcon size={16} style={{ color: 'var(--purple)' }} />
                    <span>Volume by Category</span>
                  </div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>
                    {filteredCategoryData.reduce((acc, curr) => acc + curr.value, 0)} total
                  </span>
                </div>

                <ResponsiveContainer width="100%" height={260}>
                  <PieChart>
                    <Pie
                      data={filteredCategoryData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={renderCustomizedLabel}
                      outerRadius={95}
                      innerRadius={45}
                      dataKey="value"
                      paddingAngle={3}
                    >
                      {filteredCategoryData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={entry.color} stroke="var(--bg-surface)" strokeWidth={2} />
                      ))}
                    </Pie>
                    <Tooltip
                      contentStyle={{
                        background: 'var(--bg-elevated)',
                        border: '1px solid var(--border)',
                        borderRadius: 8,
                        fontSize: 12,
                      }}
                    />
                    <Legend wrapperStyle={{ fontSize: 11, color: 'var(--text-2)' }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Campus Hotspots & Geo-Impact */}
        {activeTab === 'HOTSPOTS' && (
          <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 20 }}>
              {/* Location Frequency Bar Chart */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 14,
                  padding: '18px 20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.86rem', fontWeight: 800, color: 'var(--text-1)' }}>
                    <Building size={16} style={{ color: 'var(--orange)' }} />
                    <span>Top Complaint Hotspots</span>
                  </div>
                  <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                    High-density occurrence zones
                  </span>
                </div>

                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={mockHotspotData} layout="vertical" margin={{ left: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" horizontal={false} />
                    <XAxis type="number" tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                    <YAxis type="category" dataKey="location" tick={{ fill: 'var(--text-1)', fontSize: 11, fontWeight: 600 }} axisLine={false} tickLine={false} width={110} />
                    <Tooltip content={<CustomTooltip />} />
                    <Bar dataKey="count" name="Complaints" radius={[0, 6, 6, 0]}>
                      {mockHotspotData.map((entry, index) => (
                        <Cell key={index} fill={entry.count >= 15 ? 'var(--danger)' : entry.count >= 10 ? 'var(--warning)' : 'var(--accent)'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* Recurring Issue Flags & Preventive Action Dispatcher */}
              <div
                style={{
                  background: 'var(--bg-elevated)',
                  border: '1px solid var(--border)',
                  borderRadius: 14,
                  padding: '18px 20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.86rem', fontWeight: 800, color: 'var(--text-1)' }}>
                    <ShieldAlert size={16} style={{ color: 'var(--danger)' }} />
                    <span>Recurrence Flags & Inspections</span>
                  </div>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 700,
                      padding: '2px 7px',
                      borderRadius: 999,
                      background: 'rgba(239, 68, 68, 0.12)',
                      color: 'var(--danger)',
                    }}
                  >
                    Action Needed
                  </span>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  {[
                    { location: 'Main Cafeteria', category: 'Food & Hygiene', count: 18, risk: 'Critical', floor: 'Ground Floor Dining' },
                    { location: 'Block A Hostel', category: 'Electrical & Plumbing', count: 14, risk: 'High', floor: '2nd & 3rd Floors' },
                    { location: 'Block D Hostel', category: 'Water Supply', count: 10, risk: 'Elevated', floor: 'Block D Wing B' },
                    { location: 'CS Block', category: 'Networking & Lab Systems', count: 9, risk: 'Medium', floor: '3rd Floor Lab CS-306' },
                  ].map((item, i) => (
                    <div
                      key={i}
                      style={{
                        padding: '12px 14px',
                        background: 'var(--bg-surface)',
                        borderRadius: 10,
                        border: '1px solid var(--border)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        gap: 12,
                      }}
                    >
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.84rem', color: 'var(--text-1)', display: 'flex', alignItems: 'center', gap: 6 }}>
                          <MapPin size={12} style={{ color: 'var(--accent)' }} />
                          {item.location}
                        </div>
                        <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', marginTop: 2 }}>
                          {item.category} · {item.floor} · <strong style={{ color: 'var(--text-1)' }}>{item.count} complaints</strong>
                        </div>
                      </div>

                      <button
                        className="btn btn-warning btn-sm"
                        style={{ fontSize: '0.72rem', padding: '5px 10px', display: 'inline-flex', alignItems: 'center', gap: 5 }}
                        onClick={() => handleFlagInspection(item.location, item.count)}
                        title="Dispatch inspection crew"
                      >
                        <ShieldAlert size={12} />
                        <span>Inspect</span>
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Staff & Team Velocity */}
        {activeTab === 'STAFF_VELOCITY' && (
          <div style={{ padding: '20px 24px' }}>
            {/* Filter controls specifically for staff table */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 14,
                gap: 12,
                flexWrap: 'wrap',
              }}
            >
              <div className="filter-input-group">
                <span className="filter-label">Search Staff</span>
                <div className="search-input-wrapper" style={{ width: '220px' }}>
                  <Search size={14} className="search-icon" />
                  <input
                    type="text"
                    placeholder="Search technician..."
                    value={staffSearch}
                    onChange={(e) => setStaffSearch(e.target.value)}
                    className="filter-text-input"
                  />
                </div>
              </div>

              <div className="filter-input-group">
                <span className="filter-label">Sort Metric</span>
                <select
                  className="filter-select-input"
                  value={staffSort}
                  onChange={(e) => setStaffSort(e.target.value as any)}
                >
                  <option value="compliance">Highest SLA Compliance %</option>
                  <option value="hours">Fastest Avg Resolution (Lowest Hrs)</option>
                  <option value="resolved">Most Resolved Complaints</option>
                </select>
              </div>

              <span style={{ fontSize: '0.74rem', color: 'var(--text-2)', marginLeft: 'auto' }}>
                Showing <strong>{filteredStaff.length}</strong> technicians
              </span>
            </div>

            {/* High-Contrast Velocity Data Table */}
            <div className="management-table-container">
              <table className="management-data-table">
                <thead>
                  <tr>
                    <th style={{ width: '220px' }}>Technician</th>
                    <th style={{ width: '160px' }}>Department</th>
                    <th style={{ width: '150px' }}>Avg Resolution</th>
                    <th style={{ width: '180px' }}>SLA Compliance</th>
                    <th style={{ width: '130px' }}>Active Load</th>
                    <th style={{ width: '130px', textAlign: 'right' }}>Resolved (Month)</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredStaff.map((s) => (
                    <tr key={s.staffId} className="modern-table-row">
                      {/* Name & Avatar */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                          <div
                            style={{
                              width: 32,
                              height: 32,
                              borderRadius: '50%',
                              background: 'linear-gradient(135deg, #2563eb, #38bdf8)',
                              color: '#ffffff',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              fontWeight: 800,
                              fontSize: '0.72rem',
                            }}
                          >
                            {s.staffName.split(' ').map((n) => n[0]).join('')}
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, color: 'var(--text-1)', fontSize: '0.84rem' }}>
                              {s.staffName}
                            </div>
                            <div style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>
                              ID: {s.staffId}
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Department */}
                      <td>
                        <span
                          style={{
                            fontSize: '0.74rem',
                            fontWeight: 700,
                            padding: '3px 8px',
                            borderRadius: 6,
                            background: 'var(--bg-elevated)',
                            color: 'var(--text-1)',
                            border: '1px solid var(--border)',
                          }}
                        >
                          {s.department}
                        </span>
                      </td>

                      {/* Avg Resolution */}
                      <td>
                        <span style={{ fontWeight: 700, color: 'var(--text-1)', fontSize: '0.84rem' }}>
                          {s.avgResolutionHours}h
                        </span>
                      </td>

                      {/* SLA Compliance */}
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <div style={{ flex: 1, height: 6, background: 'var(--bg-elevated)', borderRadius: 999, overflow: 'hidden', border: '1px solid var(--border)' }}>
                            <div
                              style={{
                                height: '100%',
                                width: `${s.slaCompliancePercent}%`,
                                background:
                                  s.slaCompliancePercent >= 90
                                    ? 'var(--success)'
                                    : s.slaCompliancePercent >= 80
                                    ? 'var(--warning)'
                                    : 'var(--danger)',
                                borderRadius: 999,
                              }}
                            />
                          </div>
                          <span
                            style={{
                              fontSize: '0.78rem',
                              fontWeight: 800,
                              color:
                                s.slaCompliancePercent >= 90
                                  ? 'var(--success)'
                                  : s.slaCompliancePercent >= 80
                                  ? 'var(--warning)'
                                  : 'var(--danger)',
                              minWidth: 38,
                            }}
                          >
                            {s.slaCompliancePercent}%
                          </span>
                        </div>
                      </td>

                      {/* Active Load */}
                      <td>
                        <span
                          style={{
                            fontSize: '0.72rem',
                            fontWeight: 800,
                            padding: '3px 8px',
                            borderRadius: 999,
                            background: s.openTickets > 6 ? 'rgba(239, 68, 68, 0.12)' : 'rgba(34, 197, 94, 0.12)',
                            color: s.openTickets > 6 ? 'var(--danger)' : 'var(--success)',
                          }}
                        >
                          {s.openTickets} tickets open
                        </span>
                      </td>

                      {/* Resolved Count */}
                      <td style={{ textAlign: 'right' }}>
                        <span style={{ fontWeight: 800, fontSize: '0.86rem', color: 'var(--text-1)' }}>
                          {s.resolvedThisMonth}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Tab 4: SLA & Breach Forecast */}
        {activeTab === 'SLA_DEEP_DIVE' && (
          <div style={{ padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 20 }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
              {[
                { title: 'Critical SLA Breaches', count: 6, subtitle: 'Overdue by > 12 hours', color: 'var(--danger)' },
                { title: 'At Risk of Breach', count: 11, subtitle: 'Approaching < 4h deadline', color: 'var(--warning)' },
                { title: 'Met Within SLA Target', count: 127, subtitle: 'Resolved before target window', color: 'var(--success)' },
              ].map((card, i) => (
                <div
                  key={i}
                  style={{
                    padding: '16px 18px',
                    background: 'var(--bg-elevated)',
                    border: '1px solid var(--border)',
                    borderRadius: 12,
                  }}
                >
                  <div style={{ fontSize: '0.74rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', marginBottom: 4 }}>
                    {card.title}
                  </div>
                  <div style={{ fontSize: '1.6rem', fontWeight: 800, color: card.color }}>
                    {card.count}
                  </div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', marginTop: 2 }}>
                    {card.subtitle}
                  </div>
                </div>
              ))}
            </div>

            {/* Detailed Monthly SLA Trend Chart */}
            <div
              style={{
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 14,
                padding: '18px 20px',
              }}
            >
              <div style={{ fontSize: '0.86rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 12 }}>
                Comprehensive SLA Resolution Curve
              </div>
              <ResponsiveContainer width="100%" height={260}>
                <AreaChart data={trendData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="period" tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fill: 'var(--text-3)', fontSize: 11 }} axisLine={false} tickLine={false} />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend wrapperStyle={{ fontSize: 11, color: 'var(--text-2)' }} />
                  <Area type="monotone" dataKey="resolved" name="Resolved Within SLA" stroke="#22c55e" fill="#22c55e20" strokeWidth={2} />
                  <Area type="monotone" dataKey="atRisk" name="At-Risk Recovered" stroke="#f59e0b" fill="#f59e0b20" strokeWidth={2} />
                  <Area type="monotone" dataKey="breached" name="Breached SLA" stroke="#ef4444" fill="#ef444420" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Analytics;
