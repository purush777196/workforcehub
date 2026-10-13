import React, { useState, useMemo } from 'react';
import {
  X,
  AlertCircle,
  Clock,
  CalendarCheck2,
} from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';
import {
  calculateCalendarDays,
  findOverlappingRequest,
  LEAVE_POLICIES,
} from '../../services/leaveService';
import '../../styles/employees.css';
import '../../styles/leaves.css';

const DEFAULT_CALENDAR_YEAR = new Date().getFullYear();

export default function ApplyLeaveModal({ isOpen, onClose }) {
  const { employees } = useEmployees();
  const { leaves, applyLeave, getEmployeeBalances, isMutating } = useLeaves();
  const { user, isAdmin, isEmployee } = useAuth();

  // Filter only Active employees for new applications
  const activeEmployees = useMemo(
    () => employees.filter((e) => e.employmentStatus === 'Active'),
    [employees]
  );

  const currentEmployee = useMemo(() => {
    if (!user) return null;
    return employees.find(
      (e) => (user.email && e.email?.toLowerCase() === user.email.toLowerCase()) || e.id === user.employeeId
    );
  }, [user, employees]);

  const [selectedEmployeeId, setSelectedEmployeeId] = useState('');
  const employeeId = (isEmployee && !isAdmin && currentEmployee)
    ? currentEmployee.id
    : selectedEmployeeId;

  const [leaveType, setLeaveType] = useState('Annual');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState('');

  // Selected employee balances for context
  const selectedEmployeeBalances = useMemo(() => {
    if (!employeeId) return null;
    const reqYear = startDate ? Number(startDate.split('-')[0]) : DEFAULT_CALENDAR_YEAR;
    return getEmployeeBalances(employeeId, reqYear);
  }, [employeeId, startDate, getEmployeeBalances]);

  // Calculate calendar days
  const calculatedDays = useMemo(() => {
    return calculateCalendarDays(startDate, endDate);
  }, [startDate, endDate]);

  // Overlap check
  const overlapWarning = useMemo(() => {
    if (!employeeId || !startDate || !endDate || calculatedDays <= 0) return null;
    return findOverlappingRequest(employeeId, startDate, endDate, leaves);
  }, [employeeId, startDate, endDate, calculatedDays, leaves]);

  // Balance warning
  const balanceWarning = useMemo(() => {
    if (!employeeId || !selectedEmployeeBalances) return null;
    if (leaveType === 'Annual' && calculatedDays > 0) {
      const remaining = selectedEmployeeBalances.Annual?.remainingDays ?? 20;
      if (calculatedDays > remaining) {
        return `Requested duration (${calculatedDays} calendar days) exceeds the remaining Annual Leave balance (${remaining} days available).`;
      }
    }
    return null;
  }, [employeeId, selectedEmployeeBalances, leaveType, calculatedDays]);

  // Form validity check
  const isValid =
    Boolean(employeeId) &&
    Boolean(leaveType) &&
    Boolean(startDate) &&
    Boolean(endDate) &&
    calculatedDays > 0 &&
    Boolean(reason.trim()) &&
    !overlapWarning &&
    !balanceWarning;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isValid || isSubmitting || isMutating) return;

    setIsSubmitting(true);
    setSubmitError('');

    try {
      const res = await applyLeave({
        employeeId,
        leaveType,
        startDate,
        endDate,
        reason,
      });

      if (res && res.success) {
        handleClose();
      } else {
        setSubmitError(res?.error || 'Failed to submit leave request.');
      }
    } catch (err) {
      setSubmitError(err.message || 'An unexpected error occurred.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    setSelectedEmployeeId('');
    setLeaveType('Annual');
    setStartDate('');
    setEndDate('');
    setReason('');
    setSubmitError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="apply-leave-title"
      onClick={handleClose}
    >
      <div
        className="modal-container"
        style={{ maxWidth: 580 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-wrap">
            <h2 id="apply-leave-title" className="modal-title">
              Apply for Leave
            </h2>
            <p className="modal-subtitle">
              Submit a formal employee leave application for HR review.
            </p>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={handleClose}
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

            {/* Employee Selector */}
            <div className="form-group">
              <label htmlFor="leave-employee" className="form-label required">
                Employee (Active Personnel Only)
              </label>
              <div className="select-wrap">
                <select
                  id="leave-employee"
                  className="form-select"
                  value={employeeId}
                  onChange={(e) => setSelectedEmployeeId(e.target.value)}
                  disabled={isEmployee && !isAdmin}
                  required
                >
                  <option value="">Select an employee...</option>
                  {activeEmployees.map((emp) => (
                    <option key={emp.id} value={emp.id}>
                      {emp.firstName} {emp.lastName} ({emp.employeeId}) — {emp.department}
                    </option>
                  ))}
                </select>
              </div>
              {selectedEmployeeBalances && (
                <span className="modal-field-note">
                  Current Year Balance: Annual ({selectedEmployeeBalances.Annual.remainingDays}d rem),
                  Sick ({selectedEmployeeBalances.Sick.remainingDays}d rem),
                  Personal ({selectedEmployeeBalances.Personal.remainingDays}d rem).
                </span>
              )}
            </div>

            {/* Leave Type Selector */}
            <div className="form-group">
              <label htmlFor="leave-type" className="form-label required">
                Leave Type
              </label>
              <div className="select-wrap">
                <select
                  id="leave-type"
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
              <span className="modal-field-note">
                {LEAVE_POLICIES[leaveType]?.description}
              </span>
            </div>

            {/* Date Range: Start Date & End Date */}
            <div className="form-row" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label htmlFor="leave-start-date" className="form-label required">
                  Start Date
                </label>
                <input
                  id="leave-start-date"
                  type="date"
                  className="form-input"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="leave-end-date" className="form-label required">
                  End Date
                </label>
                <input
                  id="leave-end-date"
                  type="date"
                  className="form-input"
                  value={endDate}
                  min={startDate || undefined}
                  onChange={(e) => setEndDate(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Calendar Days Calculation & Policy Notice */}
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
                    Demonstration Policy: Requested days are calculated using inclusive calendar days.
                  </p>
                </div>
              </div>
            )}

            {/* Overlap Error Banner */}
            {overlapWarning && (
              <div className="modal-notice-box danger">
                <AlertCircle size={18} style={{ minWidth: 18 }} />
                <div>
                  <strong>Date Conflict Detected</strong>
                  <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.785rem' }}>
                    Employee already has {overlapWarning.status === 'Approved' ? 'an approved' : 'a pending'}{' '}
                    request overlapping this period ({overlapWarning.startDate} to {overlapWarning.endDate}).
                  </p>
                </div>
              </div>
            )}

            {/* Balance Warning Banner */}
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
              <label htmlFor="leave-reason" className="form-label required">
                Reason for Leave
              </label>
              <textarea
                id="leave-reason"
                className="form-textarea"
                rows={3}
                placeholder="Briefly state the reason or context for this leave application..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
              />
            </div>
          </div>

          {/* Footer */}
          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={handleClose}
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              id="submit-leave-btn"
              className="btn btn-primary"
              disabled={!isValid || isSubmitting}
            >
              {isSubmitting ? 'Submitting...' : 'Submit Application'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
