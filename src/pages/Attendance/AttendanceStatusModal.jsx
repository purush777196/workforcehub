import React, { useState, useEffect } from 'react';
import { X, ShieldCheck, AlertCircle } from 'lucide-react';
import { ATTENDANCE_STATUSES } from '../../services/attendanceService';

function AttendanceStatusDialog({ employee, record, date, onClose, onSubmit }) {
  const [status, setStatus] = useState(() => (record ? record.status : 'Present'));
  const [checkInTime, setCheckInTime] = useState(() => (record && record.checkIn ? record.checkIn : '09:00'));
  const [checkOutTime, setCheckOutTime] = useState(() => (record && record.checkOut ? record.checkOut : ''));
  const [notes, setNotes] = useState(() => (record && record.notes ? record.notes : ''));
  const [error, setError] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && !isSubmitting) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose, isSubmitting]);

  // When status is changed to Absent, clear check in / check out
  const handleStatusChange = (e) => {
    const newStatus = e.target.value;
    setStatus(newStatus);
    setError('');

    if (newStatus === 'Absent') {
      setCheckInTime('');
      setCheckOutTime('');
    } else if (!checkInTime) {
      setCheckInTime('09:00');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Validation for contradictory status & times
    if (status !== 'Absent') {
      if (!checkInTime) {
        setError('Check-in time is required when marked Present, Late, or Half Day.');
        return;
      }
      if (checkOutTime && checkOutTime < checkInTime) {
        setError('Check-out time cannot be earlier than check-in time.');
        return;
      }
    }

    setIsSubmitting(true);
    setError('');

    try {
      const result = await onSubmit({
        employeeId: employee.id,
        date,
        status,
        checkInTime: status === 'Absent' ? null : checkInTime,
        checkOutTime: status === 'Absent' ? null : checkOutTime || null,
        notes,
      });

      if (result && !result.success) {
        setError(result.message || 'Failed to update attendance record.');
      } else {
        onClose();
      }
    } catch (err) {
      setError(err.message || 'An error occurred while saving the attendance record.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const initials = `${employee.firstName?.[0] || ''}${employee.lastName?.[0] || ''}`.toUpperCase();

  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="admin-status-modal-title"
    >
      <div
        className="modal-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <h3 className="modal-title" id="admin-status-modal-title">
                Manage Attendance Record
              </h3>
              <span className="admin-badge-strip">
                <ShieldCheck size={12} /> Admin
              </span>
            </div>
            <span className="modal-subtitle">
              Override status and work timestamps for {date}
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

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && (
              <div className="form-error-msg" style={{ fontSize: '0.85rem' }}>
                <AlertCircle size={16} />
                <span>{error}</span>
              </div>
            )}

            {/* Employee Preview Header */}
            <div className="admin-employee-card">
              <div className="admin-employee-avatar">{initials}</div>
              <div className="admin-employee-meta">
                <span className="admin-employee-name">
                  {employee.firstName} {employee.lastName}
                </span>
                <span className="admin-employee-sub">
                  {employee.employeeId} • {employee.department} • {employee.designation}
                </span>
              </div>
            </div>

            {/* Status Selection */}
            <div className="form-group">
              <label className="form-label" htmlFor="admin-status-select">
                Attendance Status <span className="required-asterisk">*</span>
              </label>
              <select
                id="admin-status-select"
                className="form-select"
                value={status}
                onChange={handleStatusChange}
                required
              >
                {ATTENDANCE_STATUSES.map((st) => (
                  <option key={st} value={st}>
                    {st}
                  </option>
                ))}
              </select>
            </div>

            {/* Check-In and Check-Out Times (Conditional if not Absent) */}
            {status !== 'Absent' ? (
              <div className="form-grid-2">
                <div className="form-group">
                  <label className="form-label" htmlFor="admin-checkin-input">
                    Check-In Time <span className="required-asterisk">*</span>
                  </label>
                  <input
                    id="admin-checkin-input"
                    type="time"
                    className="form-input"
                    value={checkInTime}
                    onChange={(e) => {
                      setCheckInTime(e.target.value);
                      setError('');
                    }}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label" htmlFor="admin-checkout-input">
                    Check-Out Time (Optional)
                  </label>
                  <input
                    id="admin-checkout-input"
                    type="time"
                    className="form-input"
                    value={checkOutTime}
                    onChange={(e) => {
                      setCheckOutTime(e.target.value);
                      setError('');
                    }}
                  />
                </div>
              </div>
            ) : (
              <div
                style={{
                  fontSize: '0.8125rem',
                  color: 'var(--color-text-muted)',
                  backgroundColor: 'var(--color-bg-subtle)',
                  padding: '0.65rem 0.85rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color-light)',
                }}
              >
                Check-in and check-out timestamps are cleared when an employee is marked Absent.
              </div>
            )}

            {/* Notes */}
            <div className="form-group">
              <label className="form-label" htmlFor="admin-notes-input">
                Administrative Notes / Justification
              </label>
              <textarea
                id="admin-notes-input"
                rows={2}
                className="form-textarea"
                placeholder="Reason for manual adjustment, manager sign-off, or absence reason..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              id="submit-admin-attendance-btn"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Attendance Record'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function AttendanceStatusModal({
  isOpen,
  onClose,
  employee,
  record,
  date,
  onSubmit,
}) {
  if (!isOpen || !employee) return null;

  return (
    <AttendanceStatusDialog
      key={`${employee.id}-${date}-${record ? record.id : 'new'}`}
      employee={employee}
      record={record}
      date={date}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}
