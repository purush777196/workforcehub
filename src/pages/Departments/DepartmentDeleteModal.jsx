import React, { useState, useEffect } from 'react';
import { AlertTriangle, X, AlertCircle } from 'lucide-react';
import { useDepartments } from '../../context/useDepartments';

function DepartmentDeleteDialog({ department, onClose, onConfirm }) {
  const { getDepartmentEmployeeCount } = useDepartments();
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape' && !isDeleting) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isDeleting, onClose]);

  const employeeCount = getDepartmentEmployeeCount(department.name);

  const handleDelete = async () => {
    if (employeeCount > 0) return;
    setIsDeleting(true);
    setError(null);
    try {
      const result = await onConfirm(department.id);
      if (result && !result.success) {
        setError(result.error || 'Failed to delete department.');
        setIsDeleting(false);
      } else {
        onClose();
      }
    } catch (err) {
      setError(err.message || 'An unexpected error occurred during deletion.');
      setIsDeleting(false);
    }
  };

  return (
    <div
      className="modal-overlay"
      onClick={() => {
        if (!isDeleting) onClose();
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-dept-title"
    >
      <div
        className="modal-dialog danger-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="delete-dept-title" style={{ color: 'var(--color-danger)' }}>
              Confirm Department Deletion
            </h3>
            <span className="modal-subtitle">Permanent organizational modification</span>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            disabled={isDeleting}
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
            Are you sure you want to delete department{' '}
            <strong style={{ color: 'var(--color-text-primary)' }}>{department.name}</strong>{' '}
            (<code style={{ color: 'var(--color-primary)' }}>{department.departmentId || department.departmentCode}</code>)?
          </p>

          {error && (
            <div
              className="dept-delete-affected-alert"
              style={{
                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                borderColor: 'rgba(239, 68, 68, 0.3)',
                color: '#b91c1c',
                marginBottom: '1rem',
              }}
            >
              <AlertCircle size={20} style={{ minWidth: 20, marginTop: 2 }} />
              <div>
                <strong>Deletion Rejected:</strong>
                <p style={{ marginTop: '0.2rem' }}>{error}</p>
              </div>
            </div>
          )}

          {employeeCount > 0 ? (
            <div className="dept-delete-affected-alert">
              <AlertCircle size={20} style={{ minWidth: 20, marginTop: 2 }} />
              <div>
                <strong>Restricted Foreign Key Notice:</strong>
                <p style={{ marginTop: '0.2rem' }}>
                  This department currently has <strong>{employeeCount}</strong> assigned employee
                  {employeeCount !== 1 ? 's' : ''}. In accordance with relational database constraints and
                  WorkforceHub business rules, departments with assigned personnel cannot be deleted.
                </p>
                <p style={{ marginTop: '0.25rem', fontSize: '0.8rem', color: '#78350f' }}>
                  Please reassign all {employeeCount} employee(s) to another department before proceeding with deletion.
                </p>
              </div>
            </div>
          ) : (
            <p style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
              This department currently has 0 assigned employees. Deleting it will safely remove
              the team record from PostgreSQL without impacting any employee profiles.
            </p>
          )}
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
            id="confirm-delete-dept-btn"
            className="btn btn-danger"
            onClick={handleDelete}
            disabled={employeeCount > 0 || isDeleting}
          >
            {employeeCount > 0
              ? 'Cannot Delete (Employees Assigned)'
              : isDeleting
              ? 'Deleting...'
              : 'Delete Department'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function DepartmentDeleteModal({
  isOpen,
  onClose,
  department,
  onConfirm,
}) {
  if (!isOpen || !department) return null;

  return (
    <DepartmentDeleteDialog
      key={department.id}
      department={department}
      onClose={onClose}
      onConfirm={onConfirm}
    />
  );
}
