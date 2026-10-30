import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  ChevronRight,
  Search,
  X,
  MapPin,
  Calendar,
} from 'lucide-react';
import NotificationBell from './NotificationBell';
import { useAuth } from '../../hooks/useAuth';
import { complaintApi } from '../../api';
import { Complaint } from '../../types';
import {
  formatReportedDateTime,
  getCategoryColor,
  truncate,
} from '../../utils/helpers';
import StatusBadge from '../ui/StatusBadge';
import PriorityIndicator from '../ui/PriorityIndicator';
import CategoryIcon from '../ui/CategoryIcon';

const breadcrumbMap: Record<string, string> = {
  '/admin/queue':      'Triage Queue',
  '/admin/sla':        'SLA Monitor',
  '/admin/analytics':  'Analytics',
  '/admin/restricted': 'Restricted Cases',
  '/admin/staff':      'Staff & Departments',
  '/admin/settings':   'System Settings',
  '/staff/queue':      'My Queue',
  '/staff/workload':   'My Workload',
};

const AppHeader: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [liveComplaints, setLiveComplaints] = useState<Complaint[]>([]);

  const searchContainerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const rolePrefix = user?.role === 'STAFF' ? 'staff' : 'admin';

  const matchedEntry = Object.entries(breadcrumbMap).find(([path]) =>
    location.pathname.startsWith(path)
  );

  const breadcrumb = matchedEntry?.[1] ?? 'Dashboard';
  const section = user?.role === 'ADMIN' ? 'Admin Portal' : 'Staff Portal';

  useEffect(() => {
    const fetchSearchSuggestions = async () => {
      try {
        const res = user?.role === 'STAFF'
          ? await complaintApi.getStaffQueue({ size: 50 })
          : await complaintApi.getAdminQueue({ size: 50 });
        setLiveComplaints(res.content || []);
      } catch {
        setLiveComplaints([]);
      }
    };
    if (user) {
      fetchSearchSuggestions();
    }
  }, [user]);

  // Global keyboard shortcut: Ctrl+K / Cmd+K to focus search, Escape to close
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        inputRef.current?.focus();
        setIsOpen(true);
      } else if (e.key === 'Escape') {
        setIsOpen(false);
        inputRef.current?.blur();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Click outside listener to dismiss search dropdown
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Real-time search filtered complaints
  const searchResults = useMemo(() => {
    const trimmed = query.trim().toLowerCase();
    if (!trimmed) {
      // If empty and open, show latest 5 complaints as quick suggestions
      return liveComplaints.slice(0, 5);
    }

    return liveComplaints
      .filter((c) => {
        const tokenMatch = c.token.toLowerCase().includes(trimmed);
        const descMatch = c.description.toLowerCase().includes(trimmed);
        const catMatch = c.categoryName.toLowerCase().includes(trimmed);
        const bldgMatch = c.location.building.toLowerCase().includes(trimmed);
        const roomMatch = c.location.room?.toLowerCase().includes(trimmed);
        const studentMatch = c.student?.name?.toLowerCase().includes(trimmed);
        const staffMatch = c.assignedToStaff?.name?.toLowerCase().includes(trimmed);
        return tokenMatch || descMatch || catMatch || bldgMatch || roomMatch || studentMatch || staffMatch;
      })
      .slice(0, 8); // Top 8 relevant matches
  }, [query, liveComplaints]);

  // Reset selected index when query changes
  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  const handleSelectComplaint = (complaintId: string) => {
    setIsOpen(false);
    setQuery('');
    navigate(`/${rolePrefix}/complaint/${complaintId}`);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!isOpen) {
      if (e.key === 'ArrowDown' || e.key === 'Enter') {
        setIsOpen(true);
      }
      return;
    }

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev < searchResults.length - 1 ? prev + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev > 0 ? prev - 1 : searchResults.length - 1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (searchResults[selectedIndex]) {
        handleSelectComplaint(searchResults[selectedIndex].id);
      }
    }
  };

  return (
    <header className="app-header">
      {/* Breadcrumb Navigation */}
      <div className="header-breadcrumb">
        <span style={{ fontWeight: 700, color: 'var(--accent)' }}>FixLoop</span>
        <ChevronRight size={13} className="header-breadcrumb-sep" />
        <span>{section}</span>
        <ChevronRight size={13} className="header-breadcrumb-sep" />
        <span className="header-breadcrumb-current">{breadcrumb}</span>
      </div>

      {/* Header Actions */}
      <div className="header-actions">
        {/* Interactive Global Search Bar */}
        <div className="header-search-container" ref={searchContainerRef}>
          <div
            className={`header-quick-search ${isOpen ? 'active' : ''}`}
            onClick={() => {
              setIsOpen(true);
              inputRef.current?.focus();
            }}
          >
            <Search size={14} className="header-search-icon" />
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setIsOpen(true);
              }}
              onFocus={() => setIsOpen(true)}
              onKeyDown={handleKeyDown}
              placeholder="Search complaints, tokens, locations..."
              className="header-search-input"
            />
            {query ? (
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setQuery('');
                  inputRef.current?.focus();
                }}
                className="header-search-clear-btn"
                title="Clear search"
              >
                <X size={13} />
              </button>
            ) : (
              <kbd className="header-search-kbd">Ctrl+K</kbd>
            )}
          </div>

          {/* Live Search Results Dropdown Overlay */}
          {isOpen && (
            <div className="header-search-dropdown">
              {/* Dropdown Header */}
              <div className="header-search-dropdown-header">
                <span>
                  {query.trim()
                    ? `${searchResults.length} result${searchResults.length === 1 ? '' : 's'} matching "${query}"`
                    : 'Recent Incident Complaints'}
                </span>
                <span style={{ fontSize: '0.66rem', color: 'var(--text-3)' }}>
                  Press <kbd style={{ padding: '1px 4px', borderRadius: 3, background: 'var(--border)' }}>↵</kbd> to open
                </span>
              </div>

              {/* Results List */}
              <div className="header-search-results-list">
                {searchResults.length === 0 ? (
                  <div className="header-search-empty">
                    <Search size={22} style={{ color: 'var(--text-3)', margin: '0 auto 8px', display: 'block' }} />
                    <div style={{ fontWeight: 700, fontSize: '0.84rem', color: 'var(--text-1)' }}>No complaints found</div>
                    <div style={{ fontSize: '0.74rem', color: 'var(--text-3)', marginTop: 3 }}>
                      Try searching with a token like <strong>FXL-2026</strong>, building name, or keyword.
                    </div>
                  </div>
                ) : (
                  searchResults.map((c, idx) => {
                    const catColor = getCategoryColor(c.categoryName);
                    const isSelected = selectedIndex === idx;

                    return (
                      <div
                        key={c.id}
                        className={`header-search-item ${isSelected ? 'active' : ''}`}
                        onClick={() => handleSelectComplaint(c.id)}
                        onMouseEnter={() => setSelectedIndex(idx)}
                      >
                        {/* Item Header: Token, Category, Status, Priority */}
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6, marginBottom: 4 }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <span className="table-token-badge" style={{ fontSize: '0.74rem', padding: '2px 6px' }}>
                              {c.token}
                            </span>
                            <span
                              className="table-category-pill"
                              style={{
                                fontSize: '0.68rem',
                                padding: '2px 7px',
                                borderColor: `${catColor}30`,
                                color: catColor,
                                background: `${catColor}12`,
                              }}
                            >
                              <CategoryIcon name={c.categoryName} size={11} />
                              {c.categoryName}
                            </span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                            <PriorityIndicator score={c.priorityScore} showBar={false} />
                            <StatusBadge status={c.status} />
                          </div>
                        </div>

                        {/* Item Description */}
                        <div style={{ fontSize: '0.80rem', color: 'var(--text-1)', fontWeight: 500, lineHeight: 1.4, marginBottom: 6 }}>
                          {truncate(c.description, 110)}
                        </div>

                        {/* Item Footer: Location & Exact Reported Date/Timestamp */}
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, fontSize: '0.70rem', color: 'var(--text-3)' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                            <MapPin size={11} style={{ color: 'var(--accent)' }} />
                            <span>{c.location.building} · {c.location.room || c.location.floor}</span>
                          </div>

                          <div style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--text-2)', fontWeight: 600 }}>
                            <Calendar size={11} style={{ color: 'var(--accent)' }} />
                            <span>Reported: {formatReportedDateTime(c.createdAt)}</span>
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>

              {/* Dropdown Footer Shortcuts */}
              <div className="header-search-dropdown-footer">
                <div style={{ display: 'flex', gap: 12 }}>
                  <span><kbd>↑</kbd> <kbd>↓</kbd> Navigate</span>
                  <span><kbd>↵</kbd> Select</span>
                  <span><kbd>Esc</kbd> Close</span>
                </div>
                <span>FixLoop Live Search</span>
              </div>
            </div>
          )}
        </div>

        {/* Notification Bell */}
        <NotificationBell />
      </div>
    </header>
  );
};

export default AppHeader;
