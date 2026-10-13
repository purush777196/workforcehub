import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  Building2,
  Clock,
  CalendarOff,
  Settings,
  ChevronLeft,
  ChevronRight,
  X,
  Layers,
  Database
} from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';
import { useDepartments } from '../../context/useDepartments';
import { useAttendance } from '../../context/useAttendance';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';

const NAV_ITEMS = [
  {
    name: 'Dashboard',
    to: '/dashboard',
    icon: LayoutDashboard,
    badge: null,
    roles: ['ADMIN', 'EMPLOYEE'],
  },
  {
    name: 'Employees',
    to: '/employees',
    icon: Users,
    badge: '248',
    roles: ['ADMIN'],
  },
  {
    name: 'Departments',
    to: '/departments',
    icon: Building2,
    badge: null,
    roles: ['ADMIN'],
  },
  {
    name: 'Attendance',
    to: '/attendance',
    icon: Clock,
    badge: null,
    roles: ['ADMIN', 'EMPLOYEE'],
  },
  {
    name: 'Leave Management',
    to: '/leaves',
    icon: CalendarOff,
    badge: '8 req',
    roles: ['ADMIN', 'EMPLOYEE'],
  },
  {
    name: 'Data Migration',
    to: '/admin/data-import',
    icon: Database,
    badge: 'Phase 8C.5',
    roles: ['ADMIN'],
  },
  {
    name: 'Settings',
    to: '/settings',
    icon: Settings,
    badge: null,
    roles: ['ADMIN'],
  },
];

/**
 * Responsive Sidebar Navigation with Collapse & Mobile Drawer support
 */
export default function Sidebar({
  isCollapsed,
  setIsCollapsed,
  isMobileOpen,
  setIsMobileOpen
}) {
  const { totalCount } = useEmployees();
  const { totalDepartmentCount } = useDepartments();
  const { todayStats } = useAttendance();
  const { pendingCount } = useLeaves();
  const { user, isEmployee } = useAuth();

  const visibleNavItems = NAV_ITEMS.filter(
    (item) => !item.roles || (user?.role && item.roles.includes(user.role))
  );

  const handleNavClick = () => {
    // Automatically close drawer on mobile screen when a link is clicked
    if (isMobileOpen) {
      setIsMobileOpen(false);
    }
  };

  const toggleCollapse = () => {
    setIsCollapsed(prev => !prev);
  };

  return (
    <>
      {/* Mobile Drawer Backdrop */}
      <div
        className={`sidebar-backdrop ${isMobileOpen ? 'active' : ''}`}
        onClick={() => setIsMobileOpen(false)}
        aria-hidden="true"
      />

      <aside
        id="main-sidebar"
        className={`sidebar ${isCollapsed ? 'collapsed' : ''} ${isMobileOpen ? 'mobile-open' : ''}`}
        aria-label="Main Application Navigation"
      >
        {/* Brand Header */}
        <div className="sidebar-header">
          <NavLink
            to="/dashboard"
            className="sidebar-brand"
            onClick={handleNavClick}
            aria-label="WorkforceHub Home"
          >
            <div className="sidebar-logo-icon">
              <Layers size={22} strokeWidth={2.4} />
            </div>
            {!isCollapsed && (
              <div className="sidebar-brand-text">
                <span className="sidebar-brand-name">
                  Workforce<span>Hub</span>
                </span>
                <span className="sidebar-brand-tagline">HR Suite</span>
              </div>
            )}
          </NavLink>

          {/* Close button on mobile view */}
          <button
            type="button"
            className="sidebar-close-btn"
            onClick={() => setIsMobileOpen(false)}
            aria-label="Close navigation menu"
          >
            <X size={20} />
          </button>
        </div>

        {/* Navigation Items */}
        <div className="sidebar-body">
          <div>
            {!isCollapsed && (
              <div className="sidebar-section-title">
                {isEmployee ? 'My Workspace' : 'Core Operations'}
              </div>
            )}
            <ul className="sidebar-nav-list">
              {visibleNavItems.map((item) => {
                const Icon = item.icon;
                return (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={handleNavClick}
                      className={({ isActive }) =>
                        `sidebar-nav-link ${isActive ? 'active' : ''}`
                      }
                      title={isCollapsed ? item.name : undefined}
                      aria-label={item.name}
                    >
                      <span className="sidebar-nav-icon">
                        <Icon size={19} strokeWidth={2} />
                      </span>
                      {!isCollapsed && (
                        <span className="sidebar-nav-text" style={{ flex: 1 }}>
                          {item.name}
                        </span>
                      )}
                      {!isCollapsed && (
                        item.to === '/employees'
                          ? totalCount
                          : item.to === '/departments'
                          ? totalDepartmentCount
                          : item.to === '/attendance'
                          ? `${todayStats.totalAttending} in`
                          : item.to === '/leaves'
                          ? `${pendingCount} req`
                          : item.badge
                      ) && (
                        <span
                          className="badge badge-neutral"
                          style={{ fontSize: '0.7rem', padding: '0.1rem 0.45rem' }}
                        >
                          {item.to === '/employees'
                            ? totalCount
                            : item.to === '/departments'
                            ? totalDepartmentCount
                            : item.to === '/attendance'
                            ? `${todayStats.totalAttending} in`
                            : item.to === '/leaves'
                            ? `${pendingCount} req`
                            : item.badge}
                        </span>
                      )}
                    </NavLink>
                  </li>
                );
              })}
            </ul>
          </div>
        </div>

        {/* Sidebar Footer with Collapse Toggle & Status */}
        <div className="sidebar-footer">
          {!isCollapsed && (
            <div className="sidebar-system-badge">
              <span className="system-status-dot" aria-hidden="true" />
              <span>{user?.role === 'ADMIN' ? 'Admin Console' : 'Employee Portal'}</span>
            </div>
          )}

          {/* Desktop collapse button */}
          <button
            type="button"
            id="sidebar-collapse-toggle-btn"
            className="sidebar-collapse-toggle"
            onClick={toggleCollapse}
            aria-label={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            title={isCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {isCollapsed ? (
              <ChevronRight size={18} />
            ) : (
              <>
                <ChevronLeft size={18} />
                <span>Collapse Navigation</span>
              </>
            )}
          </button>
        </div>
      </aside>
    </>
  );
}
