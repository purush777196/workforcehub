import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import Toast from '../common/Toast';
import '../../styles/layout.css';

/**
 * AppLayout component wrapping Sidebar, Header, and page Outlets
 */
export default function AppLayout() {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isMobileOpen, setIsMobileOpen] = useState(false);

  // Close mobile drawer on desktop resize
  useEffect(() => {
    function handleResize() {
      if (window.innerWidth > 1024 && isMobileOpen) {
        setIsMobileOpen(false);
      }
    }

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [isMobileOpen]);

  return (
    <div className={`app-shell ${isCollapsed ? 'sidebar-collapsed' : ''}`}>
      {/* Sidebar Navigation */}
      <Sidebar
        isCollapsed={isCollapsed}
        setIsCollapsed={setIsCollapsed}
        isMobileOpen={isMobileOpen}
        setIsMobileOpen={setIsMobileOpen}
      />

      {/* Main Wrapper: Header + Dynamic Page Content */}
      <div className="app-wrapper">
        <Header onToggleMobileMenu={() => setIsMobileOpen(prev => !prev)} />
        <main className="app-main" id="main-content">
          <Outlet />
        </main>
      </div>

      <Toast />
    </div>
  );
}
