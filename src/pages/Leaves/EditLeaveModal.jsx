import React, { useState, useMemo } from 'react';
import {
  X,
  AlertCircle,
  CalendarCheck2,
  Clock,
} from 'lucide-react';
import { useLeaves } from '../../context/useLeaves';
import {
  calculateCalendarDays,
  findOverlappingRequest,
  LEAVE_POLICIES,
} from '../../services/leaveService';
import '../../styles/employees.css';
import '../../styles/leaves.css';

const DEFAULT_CALENDAR_YEAR = new Date().getFullYear();

export default function EditLeaveModal({ isOpen, onClose, request, employee }) {
  const { leaves, updateLeave, getEmployeeBalances } = useLeaves();

  const [leaveType, setLeaveType] = useState(() => request?.leaveType || 'Annual');
  const [startDate, setStartDate] = useState(() => request?.startDate || '');
  const [endDate, setEndDate] = useState(() => request?.endDate || '');
  const [reason, setReason] = useState(() => request?.reason || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const calculatedDays = useMemo(() => {
    return calculateCalendarDays(startDate, endDate);
  }, [startDate, endDate]);

  const overlapWarning = useMemo(() => {
    if (!request || !startDate || !endDate || calculatedDays <= 0) return null;
    return findOverlappingRequest(request.employeeId, startDate, endDate, leaves, request.id);
  }, [request, startDate, endDate, calculatedDays, leaves]);

  const balanceWarning = useMemo(() => {
    if (!request) return null;
    if (leaveType === 'Annual' && calculatedDays > 0) {
      const reqYear = startDate ? Number(startDate.split('-')[0]) : DEFAULT_CALENDAR_YEAR;
      const balances = getEmployeeBalances(request.employeeId, reqYear);
      const remaining = balances?.Annual?.remainingDays ?? 20;
      if (calculatedDays > remaining) {
        return `Requested duration (${calculatedDays} calendar days) exceeds the remaining Annual Leave balance (${remaining} days available).`;
      }
    }
    return null;
  }, [request, leaveType, calculatedDays, startDate, getEmployeeBalances]);

  const isValid =
    Boolean(leaveType) &&
    Boolean(startDate) &&
    Boolean(endDate) &&
    calculatedDays > 0 &&
    Boolean(reason.trim()) &&
    !overlapWarning &&
    !balanceWarning;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isValid || isSubmitting || !request) return;

    setIsSubmitting(true);
    setSubmitError('');

    try {
      const res = await updateLeave(request.id, {
        leaveType,
        startDate,
        endDate,
        reason,
      });

      if (res && res.success) {
        onClose();
      } else {
        setSubmitError(res?.error || 'Failed to update leave request.');
      }
    } catch (err) {
      setSubmitError(err.message || 'Failed to update leave request.');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen || !request || !employee) return null;

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="edit-leave-title"
      onClick={onClose}
    >
      <div
        className="modal-container"
        style={{ maxWidth: 560 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-wrap">
            <h2 id="edit-leave-title" className="modal-title">
              Edit Pending Leave Request
            </h2>
            <p className="modal-subtitle">
              Modify dates or reason for {employee.firstName} {employee.lastName} ({request.id}).
            </p>
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

        <form onSubmit={handleSubmit} className="modal-form">
          <div className="modal-body">
            {submitError && (
              <div className="modal-notice-box danger">
                <AlertCircle size={17} style={{ minWidth: 17 }} />
                <span>{submitError}</span>
              </div>
            )}

            {/* Leave Type */}
            <div className="form-group">
              <label htmlFor="edit-leave-type" className="form-label required">
                Leave Type
              </label>
              <div className="select-wrap">
                <select
                  id="edit-leave-type"
                  className="form-select"
                  value={leaveType}
                  onChange={(e) => setLeaveType(e.target.value)}
                  required
                >
                  {Object.entries(LEAVE_POLICIES).map(([typeKey, policy]) => (
                    <option key={typeKey} value={typeKey}>
                      {policy.label} (Allowance: {policy.allowanceDays} calendar days/yr)
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Dates */}
            <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label htmlFor="edit-start-date" className="form-label required">
                  Start Date
                </label>
                <input
                  id="edit-start-date"
                  type="date"
                  className="form-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="edit-end-date" className="form-label required">
                  End Date
                </label>
                <input
                  id="edit-end-date"
                  type="date"
                  className="form-input"
                  value={endDate}
                  min={startDate || undefined}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Calendar Days */}
            {startDate && endDate && (
              <div
                className={`modal-notice-box ${
                  calculatedDays <= 0 || overlapWarning || balanceWarning
                    ? 'danger'
                    : 'info'
                }`}
              >
                <CalendarCheck2 size={18} style={{ minWidth: 18 }} />
                <div>
                  <strong>
                    {calculatedDays > 0
                      ? `Duration: ${calculatedDays} calendar day${calculatedDays > 1 ? 's' : ''}`
                      : 'Invalid date selection: End date cannot precede Start date'}
                  </strong>
                  <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.785rem' }}>
                    Demonstration Policy: Inclusive calendar days.
                  </p>
                </div>
              </div>
            )}

            {/* Overlap Error */}
            {overlapWarning && (
              <div className="modal-notice-box danger">
                <AlertCircle size={18} style={{ minWidth: 18 }} />
                <div>
                  <strong>Date Conflict Detected</strong>
                  <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.785rem' }}>
                    Overlaps with another request ({overlapWarning.startDate} to {overlapWarning.endDate}).
                  </p>
                </div>
              </div>
            )}

            {/* Balance Warning */}
            {balanceWarning && (
              <div className="modal-notice-box warning">
                <Clock size={18} style={{ minWidth: 18 }} />
                <div>
                  <strong>Allowance Limit Exceeded</strong>
                  <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.785rem' }}>
                    {balanceWarning}
                  </p>
                </div>
              </div>
            )}

            {/* Reason */}
            <div className="form-group">
              <label htmlFor="edit-leave-reason" className="form-label required">
                Reason for Leave
              </label>
              <textarea
                id="edit-leave-reason"
                className="form-textarea"
                rows={3}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
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
              className="btn btn-primary"
              disabled={!isValid || isSubmitting}
            >
              {isSubmitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
