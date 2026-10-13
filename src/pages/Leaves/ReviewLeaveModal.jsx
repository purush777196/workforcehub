import React, { useState } from 'react';
import {
  X,
  CheckCircle2,
  XCircle,
  User,
  ShieldCheck,
} from 'lucide-react';
import { useLeaves } from '../../context/useLeaves';
import '../../styles/employees.css';
import '../../styles/leaves.css';

export default function ReviewLeaveModal({
  isOpen,
  onClose,
  request,
  employee,
  mode = 'approve', // 'approve' | 'reject'
}) {
  const { approveLeave, rejectLeave, getEmployeeBalances } = useLeaves();

  const [reviewerComment, setReviewerComment] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [commentError, setCommentError] = useState('');

  if (!isOpen || !request || !employee) return null;

  const isApprove = mode === 'approve';
  const reqYear = new Date(`${request.startDate}T00:00:00`).getFullYear();
  const balances = getEmployeeBalances(employee.id, reqYear);
  const annualBalance = balances?.Annual?.remainingDays ?? 20;

  const handleConfirm = async (e) => {
    e.preventDefault();

    if (!isApprove && !reviewerComment.trim()) {
      setCommentError('A rejection reason / comment is required to reject this request.');
      return;
    }

    setIsSubmitting(true);
    setCommentError('');

    try {
      let res;
      if (isApprove) {
        res = await approveLeave(request.id, {
          reviewerComment: reviewerComment.trim() || 'Approved by HR Administrator',
        });
      } else {
        res = await rejectLeave(request.id, {
          reviewerComment: reviewerComment.trim(),
        });
      }

      if (res && res.success) {
        handleClose();
      } else {
        setCommentError(res?.error || 'Failed to process review action.');
      }
    } catch (err) {
      setCommentError(err.message || 'Failed to process review action.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    setReviewerComment('');
    setCommentError('');
    onClose();
  };

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="review-leave-title"
      onClick={handleClose}
    >
      <div
        className="modal-container"
        style={{ maxWidth: 540 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="modal-header">
          <div className="modal-title-wrap">
            <h2 id="review-leave-title" className="modal-title">
              {isApprove ? 'Approve Leave Request' : 'Reject Leave Request'}
            </h2>
            <p className="modal-subtitle">
              Review and record administrative decision for {employee.firstName} {employee.lastName}.
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

        <form onSubmit={handleConfirm} className="modal-form">
          <div className="modal-body">
            {/* Request Summary Recap Card */}
            <div className="leave-recap-box">
              <div className="leave-recap-item">
                <span className="leave-recap-label">Employee</span>
                <span className="leave-recap-value">
                  {employee.firstName} {employee.lastName} ({employee.employeeId})
                </span>
              </div>
              <div className="leave-recap-item">
                <span className="leave-recap-label">Department</span>
                <span className="leave-recap-value">{employee.department}</span>
              </div>
              <div className="leave-recap-item">
                <span className="leave-recap-label">Leave Type</span>
                <span className="leave-recap-value">{request.leaveType} Leave</span>
              </div>
              <div className="leave-recap-item">
                <span className="leave-recap-label">Duration</span>
                <span className="leave-recap-value">
                  {request.startDate} to {request.endDate} ({request.requestedDays} days)
                </span>
              </div>
            </div>

            {/* Stated Reason */}
            <div className="form-group">
              <label className="form-label" style={{ fontSize: '0.8rem' }}>
                Employee's Stated Reason
              </label>
              <div
                style={{
                  padding: '0.65rem 0.85rem',
                  backgroundColor: 'var(--color-bg-subtle)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.85rem',
                  color: 'var(--color-text-secondary)',
                  border: '1px solid var(--border-color)',
                  lineHeight: 1.45,
                }}
              >
                "{request.reason}"
              </div>
            </div>

            {/* Allowance notice for Annual Leave */}
            {isApprove && request.leaveType === 'Annual' && (
              <div className="modal-notice-box info">
                <ShieldCheck size={18} style={{ minWidth: 18 }} />
                <div>
                  <strong>Annual Leave Allowance Check</strong>
                  <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.785rem' }}>
                    Employee has {annualBalance} days remaining in {reqYear}. Approving {request.requestedDays} days
                    will leave {Math.max(0, annualBalance - request.requestedDays)} days.
                  </p>
                </div>
              </div>
            )}

            {/* Reviewer Notice */}
            <div className="modal-notice-box info" style={{ padding: '0.55rem 0.85rem' }}>
              <User size={16} style={{ minWidth: 16 }} />
              <span style={{ fontSize: '0.8rem' }}>
                Reviewer: <strong>Alex Morgan (HR Administrator)</strong> (Demonstration Role)
              </span>
            </div>

            {/* Reviewer Comment */}
            <div className="form-group">
              <label
                htmlFor="review-comment"
                className={`form-label ${!isApprove ? 'required' : ''}`}
              >
                {isApprove ? 'Approval Notes (Optional)' : 'Rejection Reason / Comments (Mandatory)'}
              </label>
              <textarea
                id="review-comment"
                className={`form-textarea ${commentError ? 'error' : ''}`}
                rows={3}
                placeholder={
                  isApprove
                    ? 'Add optional notes for the employee or departmental record...'
                    : 'Provide the operational or policy justification for rejecting this request...'
                }
                value={reviewerComment}
                onChange={(e) => {
                  setReviewerComment(e.target.value);
                  if (commentError) setCommentError('');
                }}
                required={!isApprove}
              />
              {commentError && (
                <span className="modal-field-note" style={{ color: '#dc2626' }}>
                  {commentError}
                </span>
              )}
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
              id="confirm-review-btn"
              className={`btn ${isApprove ? 'btn-primary' : 'btn-danger'}`}
              style={{
                backgroundColor: isApprove ? '#059669' : '#dc2626',
                borderColor: isApprove ? '#059669' : '#dc2626',
                color: '#ffffff',
              }}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                'Processing...'
              ) : isApprove ? (
                <>
                  <CheckCircle2 size={16} /> Confirm Approval
                </>
              ) : (
                <>
                  <XCircle size={16} /> Confirm Rejection
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
