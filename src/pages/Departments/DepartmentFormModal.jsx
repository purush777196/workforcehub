import React, { useState, useEffect } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';

function DepartmentFormDialog({ department, onClose, onSubmit }) {
  const isEditing = Boolean(department);
  const { employees } = useEmployees();

  const [formData, setFormData] = useState(() => {
    if (department) {
      return {
        name: department.name || '',
        description: department.description || '',
        managerEmployeeId: department.managerEmployeeId || department.managerId || '',
        status: department.status || 'Active',
      };
    }
    return {
      name: '',
      description: '',
      managerEmployeeId: '',
      status: 'Active',
    };
  });

  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const result = await onSubmit({
        ...formData,
        managerEmployeeId: formData.managerEmployeeId || null,
      });
      if (result && !result.success && result.errors) {
        setErrors(result.errors);
      } else if (result && result.success) {
        onClose();
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      className="modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="dept-modal-title"
    >
      <div
        className="modal-dialog"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="dept-modal-title">
              {isEditing ? `Edit Department: ${department.name}` : 'Add New Department'}
            </h3>
            <span className="modal-subtitle">
              {isEditing
                ? `Updating department parameters for ${department.departmentId || department.departmentCode}`
                : 'Define organizational department and designate leadership.'}
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

        <form onSubmit={handleSubmit} noValidate>
          <div className="modal-body">
            {errors.general && (
              <div className="form-error-msg" style={{ fontSize: '0.85rem' }}>
                <AlertCircle size={16} />
                <span>{errors.general}</span>
              </div>
            )}

            {/* Department Name */}
            <div className="form-group">
              <label className="form-label" htmlFor="dept-name-input">
                Department Name <span className="required-asterisk">*</span>
              </label>
              <input
                id="dept-name-input"
                name="name"
                type="text"
                className={`form-input ${errors.name ? 'error' : ''}`}
                placeholder="e.g. Engineering or Quality Assurance"
                value={formData.name}
                onChange={handleChange}
                required
              />
              {errors.name && (
                <span className="form-error-msg">
                  <AlertCircle size={13} /> {errors.name}
                </span>
              )}
            </div>

            {/* Department Description */}
            <div className="form-group">
              <label className="form-label" htmlFor="dept-desc-input">
                Department Description
              </label>
              <textarea
                id="dept-desc-input"
                name="description"
                rows={3}
                className="form-textarea"
                placeholder="Brief summary of department responsibilities and operational mandate..."
                value={formData.description}
                onChange={handleChange}
              />
            </div>

            {/* Department Manager Selection */}
            <div className="form-group">
              <label className="form-label" htmlFor="dept-manager-select">
                Department Manager (Optional)
              </label>
              <select
                id="dept-manager-select"
                name="managerEmployeeId"
                className="form-select"
                value={formData.managerEmployeeId}
                onChange={handleChange}
              >
                <option value="">No Manager Assigned (Unassigned)</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {emp.firstName} {emp.lastName} ({emp.employeeId || emp.employeeCode}) — {emp.designation || emp.jobTitle}
                  </option>
                ))}
              </select>
              <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
                {employees.length === 0
                  ? 'No employees currently registered to designate as manager.'
                  : 'Select an existing employee as the division lead.'}
              </span>
            </div>

            {/* Department Status */}
            <div className="form-group">
              <label className="form-label" htmlFor="dept-status-select">
                Status <span className="required-asterisk">*</span>
              </label>
              <select
                id="dept-status-select"
                name="status"
                className="form-select"
                value={formData.status}
                onChange={handleChange}
                required
              >
                <option value="Active">Active</option>
                <option value="Inactive">Inactive</option>
              </select>
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
              id="submit-dept-form-btn"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Department'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function DepartmentFormModal({
  isOpen,
  onClose,
  department = null,
  onSubmit,
}) {
  if (!isOpen) return null;

  return (
    <DepartmentFormDialog
      key={department ? department.id : 'new-department'}
      department={department}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}
