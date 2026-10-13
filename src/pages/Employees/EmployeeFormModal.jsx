import React, { useState, useEffect } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { EMPLOYMENT_STATUSES, EMPLOYMENT_TYPES } from '../../services/employeeService';
import { useDepartments } from '../../context/useDepartments';

function EmployeeFormDialog({ employee, onClose, onSubmit }) {
  const isEditing = Boolean(employee);

  const [formData, setFormData] = useState(() => {
    if (employee) {
      return {
        firstName: employee.firstName || '',
        lastName: employee.lastName || '',
        email: employee.email || '',
        phone: employee.phone || '',
        department: employee.department || '',
        designation: employee.designation || '',
        joiningDate: employee.joiningDate || '',
        employmentStatus: employee.employmentStatus || 'Active',
        employmentType: employee.employmentType || 'Full-Time',
        address: employee.address || '',
      };
    }
    return {
      firstName: '',
      lastName: '',
      email: '',
      phone: '',
      department: '',
      designation: '',
      joiningDate: new Date().toISOString().split('T')[0],
      employmentStatus: 'Active',
      employmentType: 'Full-Time',
      address: '',
    };
  });

  const { departments } = useDepartments();
  const [errors, setErrors] = useState({});

  const departmentOptions = departments
    .filter((d) => d.status === 'Active')
    .map((d) => d.name);

  if (formData.department && !departmentOptions.includes(formData.department)) {
    departmentOptions.push(formData.department);
  }

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

  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const selectedDeptObj = departments.find((d) => d.name === formData.department);
      const submissionData = {
        ...formData,
        departmentId: selectedDeptObj ? selectedDeptObj.id : null,
      };
      const result = await onSubmit(submissionData);
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
      aria-labelledby="employee-modal-title"
    >
      <div
        className="modal-dialog large"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <div className="modal-header-title-group">
            <h3 className="modal-title" id="employee-modal-title">
              {isEditing ? `Edit Employee: ${employee.firstName} ${employee.lastName}` : 'Add New Employee'}
            </h3>
            <span className="modal-subtitle">
              {isEditing
                ? `Updating records for business ID ${employee.employeeId}`
                : 'Fill in required workforce details. Business ID will be generated automatically.'}
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

            <div className="form-grid-2">
              {/* First Name */}
              <div className="form-group">
                <label className="form-label" htmlFor="firstName">
                  First Name <span className="required-asterisk">*</span>
                </label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  className={`form-input ${errors.firstName ? 'error' : ''}`}
                  placeholder="e.g. Elena"
                  value={formData.firstName}
                  onChange={handleChange}
                  required
                />
                {errors.firstName && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.firstName}
                  </span>
                )}
              </div>

              {/* Last Name */}
              <div className="form-group">
                <label className="form-label" htmlFor="lastName">
                  Last Name <span className="required-asterisk">*</span>
                </label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  className={`form-input ${errors.lastName ? 'error' : ''}`}
                  placeholder="e.g. Rostova"
                  value={formData.lastName}
                  onChange={handleChange}
                  required
                />
                {errors.lastName && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.lastName}
                  </span>
                )}
              </div>

              {/* Email */}
              <div className="form-group">
                <label className="form-label" htmlFor="email">
                  Work Email <span className="required-asterisk">*</span>
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  className={`form-input ${errors.email ? 'error' : ''}`}
                  placeholder="elena.rostova@workforcehub.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
                {errors.email && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.email}
                  </span>
                )}
              </div>

              {/* Phone */}
              <div className="form-group">
                <label className="form-label" htmlFor="phone">
                  Phone Number <span className="required-asterisk">*</span>
                </label>
                <input
                  id="phone"
                  name="phone"
                  type="tel"
                  className={`form-input ${errors.phone ? 'error' : ''}`}
                  placeholder="+1 (555) 234-5671"
                  value={formData.phone}
                  onChange={handleChange}
                  required
                />
                {errors.phone && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.phone}
                  </span>
                )}
              </div>

              {/* Department */}
              <div className="form-group">
                <label className="form-label" htmlFor="department">
                  Department <span className="required-asterisk">*</span>
                </label>
                <select
                  id="department"
                  name="department"
                  className={`form-select ${errors.department ? 'error' : ''}`}
                  value={formData.department}
                  onChange={handleChange}
                  required
                >
                  <option value="">Select Department...</option>
                  {departmentOptions.map((dept) => (
                    <option key={dept} value={dept}>
                      {dept}
                    </option>
                  ))}
                </select>
                {errors.department && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.department}
                  </span>
                )}
              </div>

              {/* Designation */}
              <div className="form-group">
                <label className="form-label" htmlFor="designation">
                  Designation / Job Title <span className="required-asterisk">*</span>
                </label>
                <input
                  id="designation"
                  name="designation"
                  type="text"
                  className={`form-input ${errors.designation ? 'error' : ''}`}
                  placeholder="e.g. Senior Frontend Engineer"
                  value={formData.designation}
                  onChange={handleChange}
                  required
                />
                {errors.designation && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.designation}
                  </span>
                )}
              </div>

              {/* Joining Date */}
              <div className="form-group">
                <label className="form-label" htmlFor="joiningDate">
                  Joining Date <span className="required-asterisk">*</span>
                </label>
                <input
                  id="joiningDate"
                  name="joiningDate"
                  type="date"
                  className={`form-input ${errors.joiningDate ? 'error' : ''}`}
                  value={formData.joiningDate}
                  onChange={handleChange}
                  required
                />
                {errors.joiningDate && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.joiningDate}
                  </span>
                )}
              </div>

              {/* Employment Status */}
              <div className="form-group">
                <label className="form-label" htmlFor="employmentStatus">
                  Employment Status <span className="required-asterisk">*</span>
                </label>
                <select
                  id="employmentStatus"
                  name="employmentStatus"
                  className={`form-select ${errors.employmentStatus ? 'error' : ''}`}
                  value={formData.employmentStatus}
                  onChange={handleChange}
                  required
                >
                  {EMPLOYMENT_STATUSES.map((status) => (
                    <option key={status} value={status}>
                      {status}
                    </option>
                  ))}
                </select>
                {errors.employmentStatus && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.employmentStatus}
                  </span>
                )}
              </div>

              {/* Employment Type */}
              <div className="form-group">
                <label className="form-label" htmlFor="employmentType">
                  Employment Type <span className="required-asterisk">*</span>
                </label>
                <select
                  id="employmentType"
                  name="employmentType"
                  className={`form-select ${errors.employmentType ? 'error' : ''}`}
                  value={formData.employmentType}
                  onChange={handleChange}
                  required
                >
                  {EMPLOYMENT_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
                {errors.employmentType && (
                  <span className="form-error-msg">
                    <AlertCircle size={13} /> {errors.employmentType}
                  </span>
                )}
              </div>

              {/* Address (Optional) */}
              <div className="form-group col-span-2">
                <label className="form-label" htmlFor="address">
                  Physical / Residential Address (Optional)
                </label>
                <input
                  id="address"
                  name="address"
                  type="text"
                  className="form-input"
                  placeholder="Street address, City, State/Province, Country"
                  value={formData.address}
                  onChange={handleChange}
                />
              </div>
            </div>
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
            >
              Cancel
            </button>
            <button
              type="submit"
              id="submit-employee-form-btn"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Employee'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

/**
 * Modal wrapper that mounts/unmounts form content with clean keying
 */
export default function EmployeeFormModal({
  isOpen,
  onClose,
  employee = null,
  onSubmit,
}) {
  if (!isOpen) return null;

  return (
    <EmployeeFormDialog
      key={employee ? employee.id : 'new-employee'}
      employee={employee}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}
