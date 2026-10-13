import React, { useEffect } from 'react';
import { X, Mail, Phone, MapPin, Calendar, Briefcase, Building2, Edit } from 'lucide-react';
import Badge from '../../components/common/Badge';

export default function EmployeeDetailsModal({
  isOpen,
  onClose,
  employee,
  onEdit,
}) {
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !employee) return null;

  const initials = `${employee.firstName?.[0] || ''}${employee.lastName?.[0] || ''}`.toUpperCase();

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
      aria-labelledby="view-employee-title"
    >
      <div
        className="modal-dialog large"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="view-employee-title">
              Employee Profile
            </h3>
            <span className="modal-subtitle">
              Detailed employment records and personnel credentials
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
          {/* Hero Profile Block */}
          <div className="view-profile-hero">
            <div className="view-profile-avatar">{initials}</div>
            <div className="view-profile-meta">
              <h2 className="view-profile-name">
                {employee.firstName} {employee.lastName}
              </h2>
              <span className="view-profile-role">{employee.designation}</span>
              <div className="view-profile-badges">
                <span className="emp-id-chip">{employee.employeeId}</span>
                <Badge variant={getStatusBadgeVariant(employee.employmentStatus)}>
                  {employee.employmentStatus}
                </Badge>
                <Badge variant="primary">{employee.employmentType}</Badge>
              </div>
            </div>
          </div>

          {/* Details Grid */}
          <div className="view-details-grid">
            <div className="view-detail-card">
              <span className="view-detail-label">Department</span>
              <span className="view-detail-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Building2 size={16} color="var(--color-primary)" />
                {employee.department}
              </span>
            </div>

            <div className="view-detail-card">
              <span className="view-detail-label">Designation</span>
              <span className="view-detail-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Briefcase size={16} color="var(--color-primary)" />
                {employee.designation}
              </span>
            </div>

            <div className="view-detail-card">
              <span className="view-detail-label">Work Email</span>
              <span className="view-detail-value">
                <a
                  href={`mailto:${employee.email}`}
                  className="emp-email-link"
                  style={{ color: 'var(--color-primary)' }}
                >
                  <Mail size={15} />
                  {employee.email}
                </a>
              </span>
            </div>

            <div className="view-detail-card">
              <span className="view-detail-label">Phone Number</span>
              <span className="view-detail-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Phone size={15} color="var(--color-text-secondary)" />
                {employee.phone}
              </span>
            </div>

            <div className="view-detail-card">
              <span className="view-detail-label">Joining Date</span>
              <span className="view-detail-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Calendar size={15} color="var(--color-text-secondary)" />
                {employee.joiningDate}
              </span>
            </div>

            <div className="view-detail-card">
              <span className="view-detail-label">Internal System ID</span>
              <span className="view-detail-value" style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem' }}>
                {employee.id}
              </span>
            </div>

            <div className="view-detail-card" style={{ gridColumn: 'span 2' }}>
              <span className="view-detail-label">Residential Address</span>
              <span className="view-detail-value" style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <MapPin size={15} color="var(--color-text-secondary)" />
                {employee.address || 'No residential address on file.'}
              </span>
            </div>
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
            id="view-details-edit-btn"
            className="btn btn-primary"
            onClick={() => {
              onClose();
              onEdit(employee);
            }}
          >
            <Edit size={15} />
            <span>Edit Employee</span>
          </button>
        </div>
      </div>
    </div>
  );
}
