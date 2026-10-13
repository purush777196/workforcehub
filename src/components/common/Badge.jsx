import React from 'react';

/**
 * Reusable Badge component for status, departments, and tags
 */
export default function Badge({
  children,
  variant = 'neutral',
  className = '',
  icon: Icon = null,
  id,
}) {
  return (
    <span
      id={id}
      className={`badge badge-${variant} ${className}`.trim()}
    >
      {Icon && <Icon size={12} strokeWidth={2.5} />}
      {children}
    </span>
  );
}
