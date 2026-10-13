import React from 'react';
import Badge from '../../components/common/Badge';

const RECENT_ACTIVITIES = [
  {
    id: 1,
    actor: 'Elena Rostova',
    initials: 'ER',
    role: 'Senior Frontend Engineer',
    action: 'Clocked in on-site at London HQ',
    time: '8:45 AM',
    department: 'Engineering',
    badgeVariant: 'primary',
  },
  {
    id: 2,
    actor: 'Marcus Vance',
    initials: 'MV',
    role: 'Staff Product Manager',
    action: 'Submitted request for 3 days Annual Leave (Oct 20-22)',
    time: '9:12 AM',
    department: 'Product',
    badgeVariant: 'warning',
  },
  {
    id: 3,
    actor: 'Aisha Patel',
    initials: 'AP',
    role: 'Lead UX Designer',
    action: 'Clocked in remotely (Verified via GPS)',
    time: '9:30 AM',
    department: 'Design',
    badgeVariant: 'info',
  },
  {
    id: 4,
    actor: 'Liam O’Connor',
    initials: 'LO',
    role: 'Operations Director',
    action: 'Approved equipment request for new hire onboarding',
    time: '10:05 AM',
    department: 'Operations',
    badgeVariant: 'neutral',
  },
  {
    id: 5,
    actor: 'Sophia Chen',
    initials: 'SC',
    role: 'Data Scientist',
    action: 'Updated medical contact and emergency information',
    time: '10:15 AM',
    department: 'Engineering',
    badgeVariant: 'primary',
  },
];

/**
 * Recent Employee Activity feed section
 */
export default function RecentActivity() {
  return (
    <div className="dashboard-card" id="recent-activity-card">
      <div className="dashboard-card-header">
        <div className="dashboard-card-title-group">
          <h3 className="dashboard-card-title">Recent Workforce Activity</h3>
          <span className="dashboard-card-subtitle">
            Live updates across attendance, leaves & profile updates (Sample Data)
          </span>
        </div>
      </div>

      <div className="activity-feed">
        {RECENT_ACTIVITIES.map((act) => (
          <div key={act.id} className="activity-item">
            <div className="activity-avatar" aria-hidden="true">
              {act.initials}
            </div>

            <div className="activity-body">
              <div className="activity-header-line">
                <span className="activity-actor">{act.actor}</span>
                <span className="activity-time">{act.time}</span>
              </div>
              <p className="activity-description">{act.action}</p>
              <div className="activity-meta-tag">
                <Badge variant={act.badgeVariant}>{act.department}</Badge>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
