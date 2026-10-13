import React, { useEffect } from 'react';
import { AlertTriangle, X } from 'lucide-react';

export default function DeleteConfirmModal({
  isOpen,
  onClose,
  employee,
  onConfirm,
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

  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-confirm-title"
    >
      <div
        className="modal-dialog danger-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="delete-confirm-title" style={{ color: 'var(--color-danger)' }}>
              Confirm Deletion
            </h3>
            <span className="modal-subtitle">Permanent action</span>
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
          <div className="delete-warning-icon">
            <AlertTriangle size={24} />
          </div>

          <p className="delete-dialog-text">
            Are you sure you want to delete employee{' '}
            <strong style={{ color: 'var(--color-text-primary)' }}>
              {employee.firstName} {employee.lastName}
            </strong>{' '}
            (Business ID: <code style={{ color: 'var(--color-primary)' }}>{employee.employeeId}</code>)?
          </p>
          <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
            This will immediately remove their employment records and update company statistics.
            You can cancel to preserve this record.
          </p>
        </div>

        <div className="modal-footer">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onClose}
            disabled={isDeleting}
          >
            Cancel
          </button>
          <button
            type="button"
            id="confirm-delete-employee-btn"
            className="btn btn-danger"
            disabled={isDeleting}
            onClick={async () => {
              setIsDeleting(true);
              try {
                const res = await onConfirm(employee.id);
                if (res !== false) {
                  onClose();
                }
              } finally {
                setIsDeleting(false);
              }
            }}
          >
            {isDeleting ? 'Deleting...' : 'Delete Employee'}
          </button>
        </div>
      </div>
    </div>
  );
}
