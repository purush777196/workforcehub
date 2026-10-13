import React from 'react';
import PageHeader from '../components/common/PageHeader';
import Badge from '../components/common/Badge';
import { Sparkles, ArrowRight } from 'lucide-react';
import '../styles/components.css';

/**
 * Reusable PlaceholderPage for upcoming modules in WorkforceHub
 */
export default function PlaceholderPage({
  title,
  subtitle,
  icon: ModuleIcon,
  description,
  roadmapFeatures = [],
  previewTable = null,
  actionText = 'Explore Documentation',
}) {
  return (
    <div className="placeholder-container">
      <PageHeader
        title={title}
        subtitle={subtitle}
        badge={
          <Badge variant="primary" icon={Sparkles}>
            Phase 1 Foundation
          </Badge>
        }
      />

      <div className="placeholder-card">
        {ModuleIcon && (
          <div className="placeholder-icon-badge">
            <ModuleIcon size={30} strokeWidth={2.2} />
          </div>
        )}

        <h2 className="placeholder-title">{title} Module</h2>
        <p className="placeholder-desc">{description}</p>

        {roadmapFeatures.length > 0 && (
          <div className="placeholder-roadmap-grid">
            {roadmapFeatures.map((feat, idx) => {
              const FeatIcon = feat.icon;
              return (
                <div key={idx} className="roadmap-card">
                  <div className="roadmap-card-header">
                    {FeatIcon && <FeatIcon size={18} color="var(--color-primary)" />}
                    <span>{feat.title}</span>
                  </div>
                  <p>{feat.desc}</p>
                </div>
              );
            })}
          </div>
        )}

        {previewTable && (
          <div className="placeholder-table-preview">
            <div className="table-preview-header">
              <span>{previewTable.title}</span>
              <Badge variant="neutral">Demonstration Layout</Badge>
            </div>
            <div className="table-preview-rows">
              {previewTable.rows.map((row, idx) => (
                <div key={idx} className="table-preview-row">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                      {row.primary}
                    </span>
                    <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8rem' }}>
                      {row.secondary}
                    </span>
                  </div>
                  <Badge variant={row.badgeVariant || 'neutral'}>{row.badge}</Badge>
                </div>
              ))}
            </div>
          </div>
        )}

        <div style={{ marginTop: '1.75rem', display: 'flex', justifyContent: 'center' }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => alert(`Phase 1 Notice: Full ${title} capabilities will be implemented in subsequent phases.`)}
          >
            <span>{actionText}</span>
            <ArrowRight size={15} />
          </button>
        </div>
      </div>
    </div>
  );
}
