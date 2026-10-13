import React from 'react';
import {
  X,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import Badge from '../../components/common/Badge';
import '../../styles/employees.css';
import '../../styles/leaves.css';

export default function LeaveDetailsModal({ isOpen, onClose, request, employee }) {
  if (!isOpen || !request) return null;

  const initials = employee
    ? `${employee.firstName[0]}${employee.lastName[0]}`.toUpperCase()
    : 'EM';

  let statusBadgeVariant = 'warning';
  if (request.status === 'Approved') statusBadgeVariant = 'success';
  if (request.status === 'Rejected') statusBadgeVariant = 'danger';

  const formattedSubmitted = new Date(request.submittedAt).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  const formattedReviewed = request.reviewedAt
    ? new Date(request.reviewedAt).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    : null;

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="leave-details-title"
      onClick={onClose}
    >
      <div
        className="modal-container"
        style={{ maxWidth: 580 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-wrap">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
              <h2 id="leave-details-title" className="modal-title">
                Leave Request Details
              </h2>
              <Badge variant={statusBadgeVariant}>{request.status}</Badge>
            </div>
            <p className="modal-subtitle">Record Reference: {request.id}</p>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Close modal"
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Employee Profile Preview */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.85rem',
              padding: '0.85rem 1rem',
              backgroundColor: 'var(--color-bg-subtle)',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)',
            }}
          >
            <div className="emp-avatar" style={{ width: 44, height: 44, fontSize: '1rem' }}>
              {initials}
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontWeight: 600, fontSize: '0.95rem', color: 'var(--color-text-primary)' }}>
                {employee ? `${employee.firstName} ${employee.lastName}` : 'Unknown Employee'}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', display: 'flex', gap: '0.75rem', marginTop: '0.15rem' }}>
                <span>ID: {employee?.employeeId || request.employeeId}</span>
                <span>•</span>
                <span>{employee?.department || 'Unassigned'}</span>
              </div>
            </div>
          </div>

          {/* Core Leave Details Grid */}
          <div className="leave-recap-box">
            <div className="leave-recap-item">
              <span className="leave-recap-label">Leave Type</span>
              <span className="leave-recap-value">{request.leaveType} Leave</span>
            </div>
            <div className="leave-recap-item">
              <span className="leave-recap-label">Requested Days</span>
              <span className="leave-recap-value">{request.requestedDays} Calendar Days</span>
            </div>
            <div className="leave-recap-item">
              <span className="leave-recap-label">Start Date</span>
              <span className="leave-recap-value">{request.startDate}</span>
            </div>
            <div className="leave-recap-item">
              <span className="leave-recap-label">End Date</span>
              <span className="leave-recap-value">{request.endDate}</span>
            </div>
            <div className="leave-recap-item" style={{ gridColumn: 'span 2' }}>
              <span className="leave-recap-label">Submission Date</span>
              <span className="leave-recap-value">{formattedSubmitted}</span>
            </div>
          </div>

          {/* Stated Reason */}
          <div>
            <label className="form-label" style={{ fontSize: '0.8rem', marginBottom: '0.35rem' }}>
              Employee's Stated Reason
            </label>
            <div
              style={{
                padding: '0.75rem 1rem',
                backgroundColor: 'var(--color-bg-surface)',
                border: '1px solid var(--border-color)',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                color: 'var(--color-text-primary)',
                lineHeight: 1.5,
              }}
            >
              {request.reason}
            </div>
          </div>

          {/* Review Details (if Approved or Rejected) */}
          {request.reviewedAt && (
            <div
              style={{
                padding: '0.85rem 1rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: request.status === 'Approved' ? '#f0fdf4' : '#fef2f2',
                border: `1px solid ${request.status === 'Approved' ? '#bbf7d0' : '#fecaca'}`,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontWeight: 600,
                  fontSize: '0.85rem',
                  color: request.status === 'Approved' ? '#166534' : '#991b1b',
                  marginBottom: '0.4rem',
                }}
              >
                {request.status === 'Approved' ? (
                  <CheckCircle2 size={16} />
                ) : (
                  <XCircle size={16} />
                )}
                <span>Review Decision: {request.status}</span>
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)', marginBottom: '0.5rem' }}>
                Reviewed by <strong>{request.reviewedBy || 'HR Administrator'}</strong> on {formattedReviewed}
              </div>
              {request.reviewerComment && (
                <div style={{ fontSize: '0.825rem', color: 'var(--color-text-primary)', fontStyle: 'italic' }}>
                  "{request.reviewerComment}"
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="modal-footer">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
