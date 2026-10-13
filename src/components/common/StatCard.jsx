import React from 'react';
import { ArrowUpRight, ArrowDownRight, Info } from 'lucide-react';

/**
 * Reusable StatCard component for summary metrics
 */
export default function StatCard({
  id,
  title,
  value,
  trend,
  trendType = 'neutral',
  footerText,
  icon: Icon,
  colorVariant = 'blue',
}) {
  return (
    <div id={id} className="stat-card" role="region" aria-label={title}>
      <div className="stat-card-header">
        <span className="stat-card-label">{title}</span>
        {Icon && (
          <div className={`stat-card-icon-wrapper ${colorVariant}`}>
            <Icon size={20} strokeWidth={2.2} />
          </div>
        )}
      </div>

      <div className="stat-card-value-container">
        <span className="stat-card-value">{value}</span>
        {trend && (
          <span className={`stat-card-trend ${trendType}`}>
            {trendType === 'positive' && <ArrowUpRight size={13} strokeWidth={2.5} />}
            {trendType === 'warning' && <ArrowDownRight size={13} strokeWidth={2.5} />}
            {trend}
          </span>
        )}
      </div>

      <div className="stat-card-footer">
        <Info size={13} color="var(--color-text-tertiary)" />
        <span>{footerText}</span>
      </div>
    </div>
  );
}
