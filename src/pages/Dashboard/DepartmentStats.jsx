import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Building2 } from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';
import { useDepartments } from '../../context/useDepartments';

const DEPT_COLOR_PALETTE = [
  '#2563eb', // blue
  '#059669', // emerald
  '#d97706', // amber
  '#7c3aed', // purple
  '#0284c7', // sky
  '#e11d48', // rose
  '#0d9488', // teal
  '#64748b', // slate
];

/**
 * Department Statistics breakdown card with visual progress tracks
 * Dynamically synchronized with actual employee and department data.
 */
export default function DepartmentStats() {
  const { employees, totalCount } = useEmployees();
  const { departments } = useDepartments();

  // Compute live breakdown for each department
  const activeDepartments = departments.filter((d) => d.status === 'Active');

  const deptListWithCounts = activeDepartments.map((dept, index) => {
    const count = employees.filter((e) => e.department === dept.name).length;
    const percentage = totalCount > 0 ? Math.round((count / totalCount) * 100) : 0;
    return {
      id: dept.id,
      name: dept.name,
      count,
      percentage,
      color: DEPT_COLOR_PALETTE[index % DEPT_COLOR_PALETTE.length],
    };
  });

  // Calculate unassigned employees if any
  const assignedDeptNames = activeDepartments.map((d) => d.name);
  const unassignedCount = employees.filter((e) => !assignedDeptNames.includes(e.department)).length;
  if (unassignedCount > 0) {
    const unassignedPercentage = totalCount > 0 ? Math.round((unassignedCount / totalCount) * 100) : 0;
    deptListWithCounts.push({
      id: 'dept-unassigned',
      name: 'Unassigned',
      count: unassignedCount,
      percentage: unassignedPercentage,
      color: '#94a3b8',
    });
  }

  return (
    <div className="dashboard-card" id="department-stats-card">
      <div className="dashboard-card-header">
        <div className="dashboard-card-title-group">
          <h3 className="dashboard-card-title">Department Distribution</h3>
          <span className="dashboard-card-subtitle">
            {activeDepartments.length} active department{activeDepartments.length !== 1 ? 's' : ''} across {totalCount} total personnel
          </span>
        </div>
        <Link
          to="/departments"
          className="btn btn-ghost btn-sm"
          style={{ gap: '0.25rem', padding: '0.25rem 0.5rem' }}
        >
          <span>Manage All</span>
          <ArrowRight size={14} />
        </Link>
      </div>

      <div className="dept-stats-list">
        {deptListWithCounts.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '2rem 1rem', color: 'var(--color-text-muted)' }}>
            <Building2 size={32} style={{ margin: '0 auto 0.5rem', opacity: 0.5 }} />
            <p>No active departments registered.</p>
          </div>
        ) : (
          deptListWithCounts.map((dept) => (
            <div key={dept.id} className="dept-stat-item">
              <div className="dept-stat-meta">
                <div className="dept-stat-name-group">
                  <span
                    className="dept-color-pip"
                    style={{ backgroundColor: dept.color }}
                    aria-hidden="true"
                  />
                  <span className="dept-name">{dept.name}</span>
                </div>
                <div className="dept-count-group">
                  <span className="dept-headcount">
                    {dept.count} member{dept.count !== 1 ? 's' : ''}
                  </span>
                  <span className="dept-percentage">({dept.percentage}%)</span>
                </div>
              </div>

              <div className="dept-progress-track">
                <div
                  className="dept-progress-bar"
                  style={{
                    width: `${dept.percentage}%`,
                    backgroundColor: dept.color,
                  }}
                  role="progressbar"
                  aria-valuenow={dept.percentage}
                  aria-valuemin="0"
                  aria-valuemax="100"
                  aria-label={`${dept.name} has ${dept.count} members (${dept.percentage}%)`}
                />
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
