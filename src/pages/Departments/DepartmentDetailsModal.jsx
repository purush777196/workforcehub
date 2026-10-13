import React, { useEffect } from 'react';
import {
  X,
  Building2,
  Users,
  Calendar,
  Mail,
  Edit,
  UserCheck,
} from 'lucide-react';
import Badge from '../../components/common/Badge';
import { useDepartments } from '../../context/useDepartments';

export default function DepartmentDetailsModal({
  isOpen,
  onClose,
  department,
  onEdit,
}) {
  const { getDepartmentEmployees, getManagerEmployee } = useDepartments();

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !department) return null;

  const manager = getManagerEmployee(department.managerEmployeeId);
  const deptEmployees = getDepartmentEmployees(department.name);

  const getStatusBadgeVariant = (status) => {
    switch (status) {
      case 'Active':
        return 'success';
      case 'On Leave':
        return 'warning';
      case 'Inactive':
        return 'neutral';
      default:
        return 'neutral';
    }
  };

  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="view-dept-modal-title"
    >
      <div
        className="modal-dialog large"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="view-dept-modal-title">
              Department Overview
            </h3>
            <span className="modal-subtitle">
              Organizational structure, assigned lead, and active workforce members
            </span>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <X size={18} />
          </button>
        </div>

        <div className="modal-body">
          {/* Hero Header */}
          <div className="dept-view-hero">
            <div className="dept-view-hero-left">
              <div className="dept-view-icon-box">
                <Building2 size={26} strokeWidth={2.2} />
              </div>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                  <h2 className="dept-view-meta-title">{department.name}</h2>
                  <span className="emp-id-chip">{department.departmentId}</span>
                  <Badge variant={department.status === 'Active' ? 'success' : 'neutral'}>
                    {department.status}
                  </Badge>
                </div>
                <p className="dept-view-desc">
                  {department.description || 'No detailed department description provided.'}
                </p>
              </div>
            </div>
          </div>

          {/* Quick Metrics Strip */}
          <div className="dept-stats-strip">
            <div className="dept-stat-box">
              <span className="dept-stat-box-label">Department Manager</span>
              {manager ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.2rem' }}>
                  <div
                    className="dept-manager-avatar"
                    style={{ width: 28, height: 28, fontSize: '0.7rem' }}
                  >
                    {`${manager.firstName[0]}${manager.lastName[0]}`}
                  </div>
                  <div>
                    <div style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--color-text-primary)' }}>
                      {manager.firstName} {manager.lastName}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                      {manager.designation}
                    </div>
                  </div>
                </div>
              ) : (
                <span style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', fontStyle: 'italic', marginTop: '0.2rem' }}>
                  Unassigned
                </span>
              )}
            </div>

            <div className="dept-stat-box">
              <span className="dept-stat-box-label">Total Assigned Headcount</span>
              <span className="dept-stat-box-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Users size={18} color="var(--color-primary)" />
                {deptEmployees.length} member{deptEmployees.length !== 1 ? 's' : ''}
              </span>
            </div>

            <div className="dept-stat-box">
              <span className="dept-stat-box-label">Created On</span>
              <span className="dept-stat-box-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.95rem' }}>
                <Calendar size={16} color="var(--color-text-muted)" />
                {department.createdAt || 'N/A'}
              </span>
            </div>
          </div>

          {/* Department Personnel List */}
          <div className="dept-employees-section">
            <div className="dept-employees-title">
              <span>Department Members ({deptEmployees.length})</span>
              {deptEmployees.length > 0 && (
                <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
                  Live Roster
                </span>
              )}
            </div>

            {deptEmployees.length === 0 ? (
              <div
                style={{
                  textAlign: 'center',
                  padding: '2rem 1rem',
                  border: '1px dashed var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--color-text-muted)',
                }}
              >
                <UserCheck size={32} style={{ margin: '0 auto 0.5rem', opacity: 0.4 }} />
                <p style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                  No employees assigned yet
                </p>
                <p style={{ fontSize: '0.8125rem', marginTop: '0.25rem' }}>
                  Assign employees to "{department.name}" via the Employee Management directory.
                </p>
              </div>
            ) : (
              <div className="dept-employee-mini-list">
                {deptEmployees.map((emp) => {
                  const initials = `${emp.firstName?.[0] || ''}${emp.lastName?.[0] || ''}`.toUpperCase();
                  const isDeptManager = manager && manager.id === emp.id;
                  return (
                    <div key={emp.id} className="dept-employee-mini-row">
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <div className="dept-manager-avatar">{initials}</div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                            <span style={{ fontWeight: 700, color: 'var(--color-text-primary)', fontSize: '0.875rem' }}>
                              {emp.firstName} {emp.lastName}
                            </span>
                            <span className="emp-id-chip" style={{ fontSize: '0.7rem' }}>
                              {emp.employeeId}
                            </span>
                            {isDeptManager && (
                              <Badge variant="primary" style={{ fontSize: '0.68rem', padding: '0.1rem 0.4rem' }}>
                                Manager
                              </Badge>
                            )}
                          </div>
                          <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                            {emp.designation} • {emp.email}
                          </span>
                        </div>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <a
                          href={`mailto:${emp.email}`}
                          className="emp-email-link"
                          title="Contact employee"
                        >
                          <Mail size={14} />
                        </a>
                        <Badge variant={getStatusBadgeVariant(emp.employmentStatus)}>
                          {emp.employmentStatus}
                        </Badge>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
          >
            Close
          </button>
          <button
            type="button"
            id="view-dept-edit-btn"
            className="btn btn-primary"
            onClick={() => {
              onClose();
              onEdit(department);
            }}
          >
            <Edit size={15} />
            <span>Edit Department</span>
          </button>
        </div>
      </div>
    </div>
  );
}
