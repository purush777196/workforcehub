import React, { useState, useRef, useEffect } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import {
  Menu,
  Search,
  Bell,
  ChevronDown,
  User,
  Settings as SettingsIcon,
  LogOut,
  HelpCircle,
  Clock,
  CalendarCheck,
  UserPlus,
  Info
} from 'lucide-react';
import { useAuth } from '../../context/useAuth';

const ROUTE_INFO_MAP = {
  '/dashboard': { title: 'Dashboard Overview', breadcrumb: 'Dashboard' },
  '/employees': { title: 'Employee Directory', breadcrumb: 'Employees' },
  '/departments': { title: 'Departments & Teams', breadcrumb: 'Departments' },
  '/attendance': { title: 'Daily Attendance', breadcrumb: 'Attendance' },
  '/leaves': { title: 'Leave Management', breadcrumb: 'Leaves' },
  '/settings': { title: 'System Settings', breadcrumb: 'Settings' },
};

const SAMPLE_NOTIFICATIONS = [
  {
    id: 1,
    title: 'New Leave Request: Marcus Vance',
    time: '12m ago',
    icon: CalendarCheck,
    unread: true
  },
  {
    id: 2,
    title: 'Daily Attendance Report generated',
    time: '1h ago',
    icon: Clock,
    unread: true
  },
  {
    id: 3,
    title: 'New employee onboarded: Elena Rostova',
    time: '3h ago',
    icon: UserPlus,
    unread: false
  }
];

/**
 * Top Navigation Bar with breadcrumbs, search, notification popover & profile dropdown
 */
export default function Header({ onToggleMobileMenu }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout, isAdmin } = useAuth();

  const currentRoute = ROUTE_INFO_MAP[location.pathname] || {
    title: 'WorkforceHub',
    breadcrumb: 'Overview'
  };

  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [isNotificationsOpen, setIsNotificationsOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');

  const profileRef = useRef(null);
  const notificationsRef = useRef(null);

  const handleSignOut = async () => {
    setIsProfileOpen(false);
    await logout();
    navigate('/login', { replace: true });
  };

  // Close dropdowns on outside click or Escape key
  useEffect(() => {
    function handleClickOutside(event) {
      if (profileRef.current && !profileRef.current.contains(event.target)) {
        setIsProfileOpen(false);
      }
      if (notificationsRef.current && !notificationsRef.current.contains(event.target)) {
        setIsNotificationsOpen(false);
      }
    }

    function handleKeyDown(e) {
      if (e.key === 'Escape') {
        setIsProfileOpen(false);
        setIsNotificationsOpen(false);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  return (
    <header className="app-header" id="app-header">
      {/* Left: Mobile Drawer Trigger & Breadcrumb Title */}
      <div className="header-left">
        <button
          type="button"
          id="mobile-drawer-toggle-btn"
          className="mobile-menu-trigger"
          onClick={onToggleMobileMenu}
          aria-label="Open mobile navigation drawer"
        >
          <Menu size={20} />
        </button>

        <div className="header-title-container">
          <div className="header-breadcrumb">
            <span>WorkforceHub</span>
            <span>/</span>
            <span style={{ color: 'var(--color-text-secondary)' }}>{currentRoute.breadcrumb}</span>
          </div>
          <h2 className="header-page-title">{currentRoute.title}</h2>
        </div>
      </div>

      {/* Middle: Search Field UI */}
      <div className="header-search">
        <div className="search-input-wrapper">
          <Search size={16} className="search-icon" aria-hidden="true" />
          <input
            id="global-search-input"
            type="search"
            className="header-search-input"
            placeholder="Search employees, departments, leaves..."
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            aria-label="Search employees, departments, and records"
          />
          <kbd className="search-shortcut-badge" aria-hidden="true">⌘K</kbd>
        </div>
      </div>

      {/* Right: Notification Popover & User Profile */}
      <div className="header-right">
        {/* Notifications Popover */}
        <div className="notifications-wrapper" ref={notificationsRef}>
          <button
            type="button"
            id="notifications-menu-btn"
            className="header-icon-btn"
            onClick={() => {
              setIsNotificationsOpen(prev => !prev);
              setIsProfileOpen(false);
            }}
            aria-label="Notifications"
            aria-expanded={isNotificationsOpen}
            aria-haspopup="true"
          >
            <Bell size={18} />
            <span className="notification-badge-dot" aria-label="3 unread notifications" />
          </button>

          {isNotificationsOpen && (
            <div
              className="notifications-popover"
              role="region"
              aria-label="Notifications Panel"
            >
              <div className="notifications-header">
                <h4>Notifications</h4>
                <span className="badge badge-primary">2 Unread</span>
              </div>
              <ul className="notifications-list">
                {SAMPLE_NOTIFICATIONS.map((notif) => {
                  const NotifIcon = notif.icon;
                  return (
                    <li
                      key={notif.id}
                      className={`notification-item ${notif.unread ? 'unread' : ''}`}
                    >
                      <div className="notification-item-icon">
                        <NotifIcon size={14} />
                      </div>
                      <div className="notification-item-content">
                        <span className="notification-item-title">{notif.title}</span>
                        <span className="notification-item-time">{notif.time}</span>
                      </div>
                    </li>
                  );
                })}
              </ul>
              <div className="profile-dropdown-footer" style={{ textAlign: 'center' }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                  <Info size={12} /> Demonstration notice: Mock notifications preview
                </span>
              </div>
            </div>
          )}
        </div>

        {/* User Profile Dropdown */}
        <div className="profile-wrapper" ref={profileRef}>
          <button
            type="button"
            id="user-profile-menu-btn"
            className="user-profile-trigger"
            onClick={() => {
              setIsProfileOpen(prev => !prev);
              setIsNotificationsOpen(false);
            }}
            aria-expanded={isProfileOpen}
            aria-haspopup="true"
            aria-label="User profile options"
          >
            <div className="user-avatar" aria-hidden="true">
              {user?.avatarInitials || (user?.name ? user.name.slice(0, 2).toUpperCase() : 'WH')}
            </div>
            <div className="user-details">
              <span className="user-name">{user?.name || 'Workforce User'}</span>
              <span className="user-role">
                {user?.title || (isAdmin ? 'HR Administrator' : 'Staff Employee')}
              </span>
            </div>
            <ChevronDown
              size={15}
              className="profile-chevron"
              style={{
                transform: isProfileOpen ? 'rotate(180deg)' : 'rotate(0deg)'
              }}
              aria-hidden="true"
            />
          </button>

          {isProfileOpen && (
            <div
              className="profile-dropdown"
              role="menu"
              aria-label="User Options Menu"
            >
              <div className="profile-dropdown-header">
                <div className="user-avatar" style={{ width: 42, height: 42, fontSize: '0.95rem' }}>
                  {user?.avatarInitials || (user?.name ? user.name.slice(0, 2).toUpperCase() : 'WH')}
                </div>
                <div className="user-details" style={{ display: 'flex' }}>
                  <span className="user-name">{user?.name || 'Workforce User'}</span>
                  <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                    {user?.email || 'user@workforcehub.io'}
                  </span>
                  <div style={{ marginTop: '0.25rem', display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
                    <span className={`badge ${isAdmin ? 'badge-primary' : 'badge-neutral'}`}>
                      {user?.role || 'USER'}
                    </span>
                    {user?.department && (
                      <span style={{ fontSize: '0.7rem', color: 'var(--color-text-secondary)' }}>
                        {user.department}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              <ul className="profile-dropdown-menu">
                <li>
                  <button
                    type="button"
                    className="profile-dropdown-item"
                    onClick={() => setIsProfileOpen(false)}
                    role="menuitem"
                  >
                    <User size={16} />
                    <span>Profile: {user?.name}</span>
                  </button>
                </li>
                {isAdmin && (
                  <li>
                    <Link
                      to="/settings"
                      className="profile-dropdown-item"
                      onClick={() => setIsProfileOpen(false)}
                      role="menuitem"
                    >
                      <SettingsIcon size={16} />
                      <span>System Settings</span>
                    </Link>
                  </li>
                )}
                <li>
                  <button
                    type="button"
                    className="profile-dropdown-item"
                    onClick={() => setIsProfileOpen(false)}
                    role="menuitem"
                  >
                    <HelpCircle size={16} />
                    <span>Documentation & Help</span>
                  </button>
                </li>
                <li style={{ borderTop: '1px solid var(--border-color-light)', margin: '0.25rem 0' }} />
                <li>
                  <button
                    type="button"
                    id="header-sign-out-btn"
                    className="profile-dropdown-item danger"
                    onClick={handleSignOut}
                    role="menuitem"
                  >
                    <LogOut size={16} />
                    <span>Sign Out</span>
                  </button>
                </li>
              </ul>

              <div className="profile-dropdown-footer">
                Demonstration Session • {user?.role} Access Mode
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
