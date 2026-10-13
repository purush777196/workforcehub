import React from 'react';

/**
 * Reusable PageHeader component for standardizing module headers
 */
export default function PageHeader({
  title,
  subtitle,
  badge = null,
  actions = null,
  id = 'page-header'
}) {
  return (
    <header id={id} className="page-header">
      <div className="page-header-text">
        <div className="page-header-title-row">
          <h1 className="page-title">{title}</h1>
          {badge}
        </div>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>

      {actions && (
        <div className="page-header-actions">
          {actions}
        </div>
      )}
    </header>
  );
}
