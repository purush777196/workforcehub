import React from 'react';
import { Settings, BellRing, Lock, Building } from 'lucide-react';
import PlaceholderPage from '../PlaceholderPage';

export default function SettingsPage() {
  return (
    <PlaceholderPage
      title="Settings"
      subtitle="System configurations, organization settings, role definitions, and notification policies."
      icon={Settings}
      description="The Settings module configures corporate holidays, standard office hours, role-based access permissions, audit logging, and email notification preferences."
      roadmapFeatures={[
        {
          icon: Building,
          title: 'Organization Profile',
          desc: 'Manage company legal entities, primary offices, timezones, and official calendars.',
        },
        {
          icon: Lock,
          title: 'Security & Access Control',
          desc: 'Fine-grained permissions for HR admins, department heads, and regular employees.',
        },
        {
          icon: BellRing,
          title: 'Notification Preferences',
          desc: 'Configure alert triggers for daily attendance thresholds and pending leave escalations.',
        },
      ]}
      previewTable={{
        title: 'System Preferences (Preview)',
        rows: [
          { primary: 'Standard Workweek', secondary: 'Monday - Friday (40 hrs/week)', badge: 'Configured', badgeVariant: 'primary' },
          { primary: 'Primary Office Timezone', secondary: 'Europe/London (GMT+1)', badge: 'Configured', badgeVariant: 'primary' },
          { primary: 'Automatic Leave Escalation', secondary: 'Escalate to HR after 48 hrs without lead approval', badge: 'Active', badgeVariant: 'success' },
          { primary: 'Two-Factor Authentication', secondary: 'Required for all administrators and managers', badge: 'Enforced', badgeVariant: 'success' },
        ],
      }}
    />
  );
}
