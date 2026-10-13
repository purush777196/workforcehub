import React from 'react';
import {
  Users,
  UserCheck,
  CalendarOff,
  ClockAlert,
  Info,
  Calendar,
  Sparkles,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import DepartmentStats from './DepartmentStats';
import RecentActivity from './RecentActivity';
import QuickActions from './QuickActions';
import { useEmployees } from '../../context/useEmployees';
import { useAttendance } from '../../context/useAttendance';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';
import '../../styles/dashboard.css';
import '../../styles/components.css';

const CURRENT_DATE_FORMATTED = new Date().toLocaleDateString('en-US', {
  weekday: 'long',
  month: 'short',
  day: 'numeric',
  year: 'numeric',
});

export default function Dashboard() {
  const { totalCount, activeCount, inactiveCount } = useEmployees();
  const { todayStats } = useAttendance();
  const { pendingCount } = useLeaves();
  const { user, isAdmin } = useAuth();

  return (
    <div className="dashboard-page" id="dashboard-page-root">
      {/* Page Header */}
      <PageHeader
        title={`Welcome back, ${user?.name ? user.name.split(' ')[0] : 'there'}`}
        subtitle={
          isAdmin
            ? "Executive Overview · Real-time operational intelligence and workforce metrics."
            : `Employee Portal · Welcome to your personal HR workspace, ${user?.name}.`
        }
        badge={
          <Badge variant={isAdmin ? 'primary' : 'neutral'} icon={Sparkles}>
            {isAdmin ? 'Admin Console' : 'Employee Portal'}
          </Badge>
        }

        actions={
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              color: 'var(--color-text-secondary)',
              fontSize: '0.85rem',
              fontWeight: 500,
              backgroundColor: 'var(--color-bg-surface)',
              border: '1px solid var(--border-color)',
              padding: '0.45rem 0.85rem',
              borderRadius: 'var(--radius-md)',
            }}
          >
            <Calendar size={15} color="var(--color-primary)" />
            <span>{CURRENT_DATE_FORMATTED}</span>
          </div>
        }
      />

      {/* Demonstration Notice Banner */}
      <section className="demo-data-banner" role="alert" aria-label="Demo Data Notice">
        <div className="demo-data-banner-content">
          <Info size={19} style={{ minWidth: 19 }} />
          <span>
            {isAdmin ? (
              <>
                <strong>Administrator Console:</strong> Authenticated as <strong>{user?.name}</strong> with full administrative authority across Employee Directory, Department Assignments, Attendance, Leaves, and System Settings.
              </>
            ) : (
              <>
                <strong>Employee Workspace:</strong> Authenticated as <strong>{user?.name}</strong> ({user?.department || 'Staff'}). Navigating via role-tailored view with administrative configuration routes safeguarded.
              </>
            )}
          </span>
        </div>
        <span className="demo-badge-tag">{user?.role || 'Phase 7'} Access</span>
      </section>


      {/* Four Summary Stat Cards */}
      <section className="stats-grid" aria-label="Workforce Statistics Summary">
        <StatCard
          id="stat-total-employees"
          title="Total Employees"
          value={totalCount}
          trend={`${activeCount} active personnel`}
          trendType="positive"
          footerText={`${inactiveCount} inactive • ${totalCount} registered`}
          icon={Users}
          colorVariant="blue"
        />

        <StatCard
          id="stat-present-today"
          title="Present Today"
          value={todayStats.totalAttending}
          trend={`${todayStats.attendanceRate}% attendance rate`}
          trendType="positive"
          footerText={`${todayStats.presentCount} on-time, ${todayStats.lateCount} late • ${todayStats.totalAbsent} absent`}
          icon={UserCheck}
          colorVariant="green"
        />

        <StatCard
          id="stat-on-leave"
          title="On Leave Today"
          value={todayStats.onLeaveCount}
          trend={`${totalCount > 0 ? Math.round((todayStats.onLeaveCount / totalCount) * 100) : 0}% of workforce`}
          trendType="neutral"
          footerText="Approved policy leave / medical"
          icon={CalendarOff}
          colorVariant="amber"
        />

        <StatCard
          id="stat-pending-leave-requests"
          title="Pending Leave Requests"
          value={pendingCount}
          trend={pendingCount > 0 ? `${pendingCount} awaiting sign-off` : 'Queue clear'}
          trendType={pendingCount > 0 ? 'warning' : 'positive'}
          footerText={pendingCount > 0 ? 'Requires administrator review' : 'No pending approvals'}
          icon={ClockAlert}
          colorVariant="purple"
        />
      </section>

      {/* Quick HR Actions */}
      <QuickActions />

      {/* Department Statistics and Recent Employee Activity Grid */}
      <div className="dashboard-sections-grid">
        <DepartmentStats />
        <RecentActivity />
      </div>
    </div>
  );
}
