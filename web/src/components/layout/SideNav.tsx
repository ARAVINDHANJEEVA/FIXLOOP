import React, { useState, useRef, useEffect } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  ClipboardList,
  Timer,
  ShieldCheck,
  Users,
  BarChart3,
  Settings,
  History,
  Sun,
  Moon,
  LogOut,
  Pin,
  PinOff,
  MoreVertical,
} from 'lucide-react';
import { useAuth } from '../../hooks/useAuth';
import { useTheme } from '../../hooks/useTheme';

interface NavItemDef {
  label: string;
  path: string;
  icon: React.ReactNode;
  badge?: string | number;
  badgeDot?: boolean;
}

interface NavGroupDef {
  title: string;
  items: NavItemDef[];
}

const adminGroups: NavGroupDef[] = [
  {
    title: 'General',
    items: [
      { label: 'Triage Queue', path: '/admin/queue', icon: <LayoutDashboard size={18} strokeWidth={2} /> },
      { label: 'SLA Monitor', path: '/admin/sla', icon: <Timer size={18} strokeWidth={2} />, badge: 2, badgeDot: true },
      { label: 'Restricted', path: '/admin/restricted', icon: <ShieldCheck size={18} strokeWidth={2} /> },
    ],
  },
  {
    title: 'Management',
    items: [
      { label: 'Staff & Teams', path: '/admin/staff', icon: <Users size={18} strokeWidth={2} /> },
      { label: 'Resolution History', path: '/admin/history', icon: <History size={18} strokeWidth={2} /> },
      { label: 'Analytics', path: '/admin/analytics', icon: <BarChart3 size={18} strokeWidth={2} /> },
      { label: 'Settings', path: '/admin/settings', icon: <Settings size={18} strokeWidth={2} /> },
    ],
  },
];

const staffGroups: NavGroupDef[] = [
  {
    title: 'Workspace',
    items: [
      { label: 'My Queue', path: '/staff/queue', icon: <ClipboardList size={18} strokeWidth={2} />, badge: 3, badgeDot: true },
      { label: 'Resolution History', path: '/staff/history', icon: <History size={18} strokeWidth={2} /> },
      { label: 'Workload & KPIs', path: '/staff/workload', icon: <BarChart3 size={18} strokeWidth={2} /> },
    ],
  },
];

const SideNav: React.FC = () => {
  const { user, logout } = useAuth();
  const { toggleTheme, isDark } = useTheme();
  const location = useLocation();
  const navigate = useNavigate();

  // Industry Standard: Auto-expands smoothly with 0.25s hover activation delay
  const [isHovered, setIsHovered] = useState(false);
  const [isPinned, setIsPinned] = useState(false);
  const isExpanded = isPinned || isHovered;
  const hoverTimerRef = useRef<NodeJS.Timeout | null>(null);

  const handleMouseEnter = () => {
    if (hoverTimerRef.current) clearTimeout(hoverTimerRef.current);
    // 0.25s hover activation delay
    hoverTimerRef.current = setTimeout(() => {
      setIsHovered(true);
    }, 250);
  };

  const handleMouseLeave = () => {
    if (hoverTimerRef.current) {
      clearTimeout(hoverTimerRef.current);
      hoverTimerRef.current = null;
    }
    setIsHovered(false);
  };

  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const profileMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (profileMenuRef.current && !profileMenuRef.current.contains(event.target as Node)) {
        setShowProfileMenu(false);
      }
    };
    if (showProfileMenu) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [showProfileMenu]);

  useEffect(() => {
    document.body.classList.toggle('sidebar-is-expanded', isExpanded);
    return () => {
      document.body.classList.remove('sidebar-is-expanded');
    };
  }, [isExpanded]);

  useEffect(() => {
    return () => {
      if (hoverTimerRef.current) clearTimeout(hoverTimerRef.current);
    };
  }, []);

  const groups = user?.role === 'ADMIN' ? adminGroups : staffGroups;

  return (
    <aside
      className={`shopall-nav-container ${!isExpanded ? 'collapsed' : ''}`}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
    >
      {/* Brand Header */}
      <div className="shopall-header">
        <div className="shopall-brand" title="FixLoop">
          {/* Geometric Clover Loop Logo */}
          <div className="shopall-logo-mark">
            <span className="petal petal-1" />
            <span className="petal petal-2" />
            <span className="petal petal-3" />
            <span className="petal petal-4" />
          </div>
          {isExpanded && <span className="shopall-brand-name">FixLoop</span>}
        </div>

        {/* Pin / Lock Open Toggle Button (Visible when expanded) */}
        {isExpanded && (
          <button
            className={`shopall-collapse-btn ${isPinned ? 'pinned' : ''}`}
            onClick={() => setIsPinned((prev) => !prev)}
            title={isPinned ? 'Unpin sidebar (auto-collapse on mouse leave)' : 'Pin sidebar open'}
          >
            {isPinned ? <PinOff size={13} /> : <Pin size={13} />}
          </button>
        )}
      </div>

      {/* Navigation Menu Groups (Campus part removed) */}
      <div className="shopall-groups-list" style={{ marginTop: 8 }}>
        {groups.map((group) => (
          <div key={group.title} className="shopall-group">
            <div className="shopall-group-label">
              {!isExpanded ? group.title.slice(0, 3) : group.title}
            </div>

            <div className="shopall-group-items">
              {group.items.map((item) => {
                const isActive =
                  location.pathname === item.path ||
                  (item.path !== '/admin' && location.pathname.startsWith(item.path + '/'));

                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={`shopall-nav-item ${isActive ? 'active' : ''}`}
                    title={!isExpanded ? item.label : undefined}
                  >
                    <span className="shopall-item-icon">{item.icon}</span>

                    {isExpanded && (
                      <span className="shopall-item-label">{item.label}</span>
                    )}

                    {/* Badge */}
                    {item.badge !== undefined && (
                      !isExpanded ? (
                        <span className="shopall-dot-badge" />
                      ) : (
                        <span className="shopall-count-badge">{item.badge}</span>
                      )
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* Bottom Section: Theme & Account */}
      <div className="shopall-footer-section">
        <div className="shopall-group-label">
          {!isExpanded ? 'App' : 'Preferences'}
        </div>

        {/* Theme Toggle Item */}
        <button
          className="shopall-nav-item shopall-action-item"
          onClick={toggleTheme}
          title={!isExpanded ? (isDark ? 'Switch to Light' : 'Switch to Dark') : undefined}
        >
          <span className="shopall-item-icon">
            {isDark ? <Sun size={18} strokeWidth={2} /> : <Moon size={18} strokeWidth={2} />}
          </span>
          {isExpanded && (
            <span className="shopall-item-label">
              {isDark ? 'Light Theme' : 'Dark Theme'}
            </span>
          )}
        </button>

        {/* Profile Card Container - Popover anchored strictly here directly above the profile card */}
        <div className="shopall-profile-wrapper" ref={profileMenuRef} style={{ position: 'relative' }}>
          <div
            className="shopall-profile-card"
            title={`${user?.name || 'Administrator'} (${user?.role || 'Admin'})`}
          >
            <div className="shopall-avatar">
              {user?.avatarInitials || 'AK'}
            </div>
            {isExpanded && (
              <>
                <div className="shopall-profile-meta">
                  <div className="shopall-profile-name">{user?.name || 'Admin Krishnan'}</div>
                  <div className="shopall-profile-sub">
                    {user?.role === 'ADMIN' ? 'admin@fixloop.edu' : 'staff@fixloop.edu'}
                  </div>
                </div>
                <button
                  type="button"
                  className="shopall-profile-3dot-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowProfileMenu((prev) => !prev);
                  }}
                  title="Profile actions"
                >
                  <MoreVertical size={16} />
                </button>
              </>
            )}
          </div>

          {/* 3-Dot Profile Popover Menu (Directly above profile card, no theme switch) */}
          {showProfileMenu && (
            <div className="shopall-profile-popover">
              <div className="shopall-popover-header">
                <div style={{ fontWeight: 700, fontSize: '0.82rem', color: 'var(--text-1)' }}>
                  {user?.name || 'User'}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>
                  {user?.role === 'ADMIN' ? 'Administrator' : 'Staff Technician'}
                </div>
              </div>
              <div className="shopall-popover-divider" />
              <button
                type="button"
                className="shopall-popover-item shopall-popover-logout"
                onClick={() => {
                  setShowProfileMenu(false);
                  logout();
                  navigate('/login', { state: { justLoggedOut: true } });
                }}
              >
                <LogOut size={14} />
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};

export default SideNav;
