import React from 'react';
import { Link } from 'react-router-dom';
import { UserPlus, CalendarCheck, FileCheck, Building2 } from 'lucide-react';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';

/**
 * Quick HR Actions section component
 */
export default function QuickActions() {
  const { pendingCount } = useLeaves();
  const { isEmployee } = useAuth();

  const adminActions = [
    {
      title: 'Add New Employee',
      desc: 'Onboard team member',
      to: '/employees',
      icon: UserPlus,
    },
    {
      title: 'Record Attendance',
      desc: 'Log shift or clock-in',
      to: '/attendance',
      icon: CalendarCheck,
    },
    {
      title: 'Review Leaves',
      desc:
        pendingCount > 0
          ? `${pendingCount} pending request${pendingCount !== 1 ? 's' : ''}`
          : 'Queue clear',
      to: '/leaves',
      icon: FileCheck,
    },
    {
      title: 'Manage Departments',
      desc: 'Organize teams & leads',
      to: '/departments',
      icon: Building2,
    },
  ];

  const employeeActions = [
    {
      title: 'Log Attendance',
      desc: 'Review daily status & check in',
      to: '/attendance',
      icon: CalendarCheck,
    },
    {
      title: 'Request Leave',
      desc: 'Submit vacation or medical leave',
      to: '/leaves',
      icon: FileCheck,
    },
  ];

  const quickActions = isEmployee ? employeeActions : adminActions;


  return (
    <div className="quick-actions-card" id="quick-actions-section">
      <div className="dashboard-card-title-group">
        <h3 className="dashboard-card-title">Quick HR Operations</h3>
        <span className="dashboard-card-subtitle">
          Jump straight into core personnel tasks and operational workflows
        </span>
      </div>

      <div className="quick-actions-grid">
        {quickActions.map((action) => {
          const Icon = action.icon;
          return (
            <Link
              key={action.to}
              to={action.to}
              className="quick-action-btn"
              id={`quick-action-${action.title.toLowerCase().replace(/\s+/g, '-')}`}
            >
              <div className="quick-action-icon-box">
                <Icon size={20} strokeWidth={2.2} />
              </div>
              <div className="quick-action-text">
                <span className="quick-action-title">{action.title}</span>
                <span className="quick-action-desc">{action.desc}</span>
              </div>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
