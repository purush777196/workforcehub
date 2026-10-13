import React, { useState, useMemo } from 'react';
import {
  UserPlus,
  Search,
  X,
  RotateCcw,
  Eye,
  Edit,
  Trash2,
  Users,
  ChevronLeft,
  ChevronRight,
  Mail,
  Sparkles,
  AlertCircle,
  RefreshCw,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import Badge from '../../components/common/Badge';
import { useEmployees } from '../../context/useEmployees';
import { useDepartments } from '../../context/useDepartments';
import { EMPLOYMENT_STATUSES, EMPLOYMENT_TYPES } from '../../services/employeeService';
import EmployeeFormModal from './EmployeeFormModal';
import EmployeeDetailsModal from './EmployeeDetailsModal';
import DeleteConfirmModal from './DeleteConfirmModal';
import '../../styles/employees.css';

export default function EmployeesPage() {
  const {
    employees,
    totalCount,
    isLoading,
    error,
    refreshEmployees,
    addEmployee,
    updateEmployee,
    deleteEmployee,
  } = useEmployees();

  const { departments } = useDepartments();

  const departmentFilterOptions = useMemo(() => {
    const list = departments.map((d) => d.name);
    if (employees.some((e) => e.department === 'Unassigned')) {
      list.push('Unassigned');
    }
    return Array.from(new Set(list));
  }, [departments, employees]);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDept, setSelectedDept] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [sortBy, setSortBy] = useState('name-asc'); // name-asc, name-desc, date-desc, date-asc

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Modal states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [viewingEmployee, setViewingEmployee] = useState(null);
  const [deletingEmployee, setDeletingEmployee] = useState(null);

  // Reset to page 1 when search or any filter changes
  const handleSearchChange = (e) => {
    setSearchQuery(e.target.value);
    setCurrentPage(1);
  };

  const handleDeptChange = (e) => {
    setSelectedDept(e.target.value);
    setCurrentPage(1);
  };

  const handleStatusChange = (e) => {
    setSelectedStatus(e.target.value);
    setCurrentPage(1);
  };

  const handleTypeChange = (e) => {
    setSelectedType(e.target.value);
    setCurrentPage(1);
  };

  const handleSortChange = (e) => {
    setSortBy(e.target.value);
    setCurrentPage(1);
  };

  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setCurrentPage(1);
  };

  const clearAllFilters = () => {
    setSearchQuery('');
    setSelectedDept('');
    setSelectedStatus('');
    setSelectedType('');
    setSortBy('name-asc');
    setCurrentPage(1);
  };

  const isFilterActive = Boolean(
    searchQuery || selectedDept || selectedStatus || selectedType || sortBy !== 'name-asc'
  );

  // Filter and sort employees
  const filteredEmployees = useMemo(() => {
    return employees
      .filter((emp) => {
        // Search by name, employeeId, or email
        if (searchQuery.trim()) {
          const query = searchQuery.trim().toLowerCase();
          const fullName = `${emp.firstName || ''} ${emp.lastName || ''}`.toLowerCase();
          const empId = (emp.employeeId || '').toLowerCase();
          const email = (emp.email || '').toLowerCase();

          if (!fullName.includes(query) && !empId.includes(query) && !email.includes(query)) {
            return false;
          }
        }

        // Department filter
        if (selectedDept && emp.department !== selectedDept) {
          return false;
        }

        // Status filter
        if (selectedStatus && emp.employmentStatus !== selectedStatus) {
          return false;
        }

        // Type filter
        if (selectedType && emp.employmentType !== selectedType) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'name-asc') {
          const nameA = `${a.firstName} ${a.lastName}`.toLowerCase();
          const nameB = `${b.firstName} ${b.lastName}`.toLowerCase();
          return nameA.localeCompare(nameB);
        }
        if (sortBy === 'name-desc') {
          const nameA = `${a.firstName} ${a.lastName}`.toLowerCase();
          const nameB = `${b.firstName} ${b.lastName}`.toLowerCase();
          return nameB.localeCompare(nameA);
        }
        if (sortBy === 'date-desc') {
          return new Date(b.joiningDate) - new Date(a.joiningDate);
        }
        if (sortBy === 'date-asc') {
          return new Date(a.joiningDate) - new Date(b.joiningDate);
        }
        return 0;
      });
  }, [employees, searchQuery, selectedDept, selectedStatus, selectedType, sortBy]);

  // Pagination calculation
  const totalFiltered = filteredEmployees.length;
  const totalPages = Math.ceil(totalFiltered / pageSize) || 1;
  const validCurrentPage = Math.min(currentPage, totalPages);

  const paginatedEmployees = useMemo(() => {
    const startIndex = (validCurrentPage - 1) * pageSize;
    return filteredEmployees.slice(startIndex, startIndex + pageSize);
  }, [filteredEmployees, validCurrentPage, pageSize]);

  // Modal action handlers
  const handleOpenAdd = () => {
    setEditingEmployee(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (employee) => {
    setEditingEmployee(employee);
    setIsFormOpen(true);
  };

  const handleOpenView = (employee) => {
    setViewingEmployee(employee);
  };

  const handleOpenDelete = (employee) => {
    setDeletingEmployee(employee);
  };

  const handleFormSubmit = async (formData) => {
    if (editingEmployee) {
      return await updateEmployee(editingEmployee.id, formData);
    }
    return await addEmployee(formData);
  };

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
    <div className="employees-page" id="employees-page-root">
      {/* Header */}
      <PageHeader
        title="Employee Management"
        subtitle="Directory, workforce profiles, job designations, and team allocations."
        badge={
          <Badge variant="primary" icon={Sparkles}>
            Phase 8C.1 REST Active
          </Badge>
        }
        actions={
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span
              className="badge badge-neutral"
              style={{ padding: '0.45rem 0.85rem', fontSize: '0.85rem' }}
            >
              <strong>{totalCount}</strong> Total Employees
            </span>
            <button
              type="button"
              id="refresh-employees-btn"
              className="btn btn-secondary"
              onClick={refreshEmployees}
              disabled={isLoading}
              title="Refresh employees from PostgreSQL backend"
              aria-label="Refresh employees"
            >
              <RefreshCw size={15} className={isLoading ? 'spinning' : ''} />
              <span>Refresh</span>
            </button>
            <button
              type="button"
              id="add-employee-btn"
              className="btn btn-primary"
              onClick={handleOpenAdd}
            >
              <UserPlus size={16} />
              <span>Add Employee</span>
            </button>
          </div>
        }
      />

      {/* Backend Connection Error Banner */}
      {error && (
        <div
          className="alert-banner alert-banner-danger"
          style={{
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '1rem 1.25rem',
            borderRadius: '10px',
            background: 'rgba(239, 68, 68, 0.08)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            color: '#ef4444',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={20} />
            <div>
              <strong>Backend Connection Issue:</strong> {error}
            </div>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={refreshEmployees}
            disabled={isLoading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <RotateCcw size={14} />
            <span>Retry Connection</span>
          </button>
        </div>
      )}

      {/* Filters and Controls Bar */}
      <section className="employee-controls-card" aria-label="Employee Search and Filter Controls">
        <div className="controls-top-row">
          {/* Search Box */}
          <div className="employee-search-box">
            <Search size={16} className="search-icon" aria-hidden="true" />
            <input
              id="employee-search-input"
              type="search"
              className="employee-search-input"
              placeholder="Search by employee name, ID, or email..."
              value={searchQuery}
              onChange={handleSearchChange}
              aria-label="Search employees by name, ID, or email"
            />
            {searchQuery && (
              <button
                type="button"
                className="search-clear-btn"
                onClick={() => {
                  setSearchQuery('');
                  setCurrentPage(1);
                }}
                aria-label="Clear search text"
              >
                <X size={13} />
              </button>
            )}
          </div>

          {/* Sort Control */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="employee-sort-select">
              Sort By:
            </label>
            <select
              id="employee-sort-select"
              className="filter-select"
              value={sortBy}
              onChange={handleSortChange}
              aria-label="Sort employees by field"
            >
              <option value="name-asc">Name (A → Z)</option>
              <option value="name-desc">Name (Z → A)</option>
              <option value="date-desc">Joining Date (Newest first)</option>
              <option value="date-asc">Joining Date (Oldest first)</option>
            </select>
          </div>
        </div>

        {/* Second Row: Combined Filters */}
        <div className="filters-row">
          {/* Department Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="filter-department">
              Department:
            </label>
            <select
              id="filter-department"
              className="filter-select"
              value={selectedDept}
              onChange={handleDeptChange}
              aria-label="Filter employees by department"
            >
              <option value="">All Departments</option>
              {departmentFilterOptions.map((dept) => (
                <option key={dept} value={dept}>
                  {dept}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="filter-status">
              Status:
            </label>
            <select
              id="filter-status"
              className="filter-select"
              value={selectedStatus}
              onChange={handleStatusChange}
              aria-label="Filter employees by employment status"
            >
              <option value="">All Statuses</option>
              {EMPLOYMENT_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>

          {/* Type Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="filter-type">
              Type:
            </label>
            <select
              id="filter-type"
              className="filter-select"
              value={selectedType}
              onChange={handleTypeChange}
              aria-label="Filter employees by employment type"
            >
              <option value="">All Types</option>
              {EMPLOYMENT_TYPES.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </div>

          {/* Clear Filters Button */}
          {isFilterActive && (
            <button
              type="button"
              id="clear-filters-btn"
              className="clear-filters-btn"
              onClick={clearAllFilters}
            >
              <RotateCcw size={13} />
              <span>Clear Filters</span>
            </button>
          )}
        </div>
      </section>

      {/* Employee Data Table */}
      <section className="table-card" aria-label="Employee Directory Table">
        {isLoading ? (
          <div className="empty-state-box" id="loading-state-container" style={{ padding: '3.5rem 1rem' }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
              <RefreshCw size={28} className="spinning" style={{ color: 'var(--color-primary)' }} />
              <h3 className="empty-state-title" style={{ margin: 0 }}>Loading Employee Directory...</h3>
              <p className="empty-state-desc" style={{ margin: 0 }}>
                Synchronizing live employee profiles and workforce records from PostgreSQL backend.
              </p>
            </div>
          </div>
        ) : totalFiltered === 0 ? (
          <div className="empty-state-box" id="empty-state-container">
            <div className="empty-state-icon">
              <Users size={28} />
            </div>
            <h3 className="empty-state-title">No employees found</h3>
            <p className="empty-state-desc">
              {isFilterActive
                ? 'No personnel records match the current filter and search conditions. Try clearing filters or altering search keywords.'
                : 'No employees currently registered in WorkforceHub. Click "Add Employee" to register the first employee.'}
            </p>
            {isFilterActive && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={clearAllFilters}
              >
                <RotateCcw size={14} />
                <span>Reset All Filters</span>
              </button>
            )}
          </div>
        ) : (
          <>
            <div className="table-responsive-wrapper">
              <table className="employee-table" id="employees-data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Employee Name</th>
                    <th>Email</th>
                    <th>Department</th>
                    <th>Designation</th>
                    <th>Joining Date</th>
                    <th>Status</th>
                    <th style={{ textAlign: 'right' }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {paginatedEmployees.map((emp) => {
                    const initials = `${emp.firstName?.[0] || ''}${emp.lastName?.[0] || ''}`.toUpperCase();
                    return (
                      <tr key={emp.id} id={`emp-row-${emp.id}`}>
                        <td>
                          <span className="emp-id-chip">{emp.employeeId}</span>
                        </td>
                        <td>
                          <div className="emp-name-cell">
                            <div className="emp-avatar" aria-hidden="true">
                              {initials}
                            </div>
                            <div className="emp-name-text">
                              <span className="emp-full-name">
                                {emp.firstName} {emp.lastName}
                              </span>
                              <span className="emp-type-label">{emp.employmentType}</span>
                            </div>
                          </div>
                        </td>
                        <td>
                          <a
                            href={`mailto:${emp.email}`}
                            className="emp-email-link"
                            title={`Send email to ${emp.email}`}
                          >
                            <Mail size={13} />
                            <span>{emp.email}</span>
                          </a>
                        </td>
                        <td>
                          <span style={{ fontWeight: 500 }}>{emp.department}</span>
                        </td>
                        <td>
                          <span style={{ color: 'var(--color-text-secondary)' }}>
                            {emp.designation}
                          </span>
                        </td>
                        <td>
                          <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8125rem' }}>
                            {emp.joiningDate}
                          </span>
                        </td>
                        <td>
                          <Badge variant={getStatusBadgeVariant(emp.employmentStatus)}>
                            {emp.employmentStatus}
                          </Badge>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div className="emp-actions-cell" style={{ justifyContent: 'flex-end' }}>
                            {/* View Button */}
                            <button
                              type="button"
                              className="action-icon-btn"
                              title={`View ${emp.firstName} ${emp.lastName}`}
                              onClick={() => handleOpenView(emp)}
                              aria-label={`View ${emp.firstName} ${emp.lastName}`}
                            >
                              <Eye size={16} />
                            </button>

                            {/* Edit Button */}
                            <button
                              type="button"
                              className="action-icon-btn"
                              title={`Edit ${emp.firstName} ${emp.lastName}`}
                              onClick={() => handleOpenEdit(emp)}
                              aria-label={`Edit ${emp.firstName} ${emp.lastName}`}
                            >
                              <Edit size={16} />
                            </button>

                            {/* Delete Button */}
                            <button
                              type="button"
                              className="action-icon-btn danger"
                              title={`Delete ${emp.firstName} ${emp.lastName}`}
                              onClick={() => handleOpenDelete(emp)}
                              aria-label={`Delete ${emp.firstName} ${emp.lastName}`}
                            >
                              <Trash2 size={16} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div className="pagination-bar">
              <div className="pagination-info">
                Showing{' '}
                <strong>
                  {Math.min((validCurrentPage - 1) * pageSize + 1, totalFiltered)}
                </strong>{' '}
                to <strong>{Math.min(validCurrentPage * pageSize, totalFiltered)}</strong> of{' '}
                <strong>{totalFiltered}</strong> employees
                {totalFiltered < totalCount && ` (filtered from ${totalCount} total)`}
              </div>

              <div className="pagination-controls">
                <div className="page-size-selector">
                  <label htmlFor="page-size-select">Rows per page:</label>
                  <select
                    id="page-size-select"
                    className="filter-select"
                    style={{ height: '32px', padding: '0 1.75rem 0 0.5rem' }}
                    value={pageSize}
                    onChange={handlePageSizeChange}
                  >
                    <option value={10}>10</option>
                    <option value={25}>25</option>
                    <option value={50}>50</option>
                  </select>
                </div>

                <div className="pagination-btn-group">
                  <button
                    type="button"
                    className="page-btn"
                    disabled={validCurrentPage <= 1}
                    onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                    aria-label="Previous Page"
                  >
                    <ChevronLeft size={16} />
                  </button>

                  {Array.from({ length: totalPages }, (_, idx) => idx + 1).map((pageNum) => (
                    <button
                      key={pageNum}
                      type="button"
                      className={`page-btn ${pageNum === validCurrentPage ? 'active' : ''}`}
                      onClick={() => setCurrentPage(pageNum)}
                      aria-label={`Go to page ${pageNum}`}
                      aria-current={pageNum === validCurrentPage ? 'page' : undefined}
                    >
                      {pageNum}
                    </button>
                  ))}

                  <button
                    type="button"
                    className="page-btn"
                    disabled={validCurrentPage >= totalPages}
                    onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                    aria-label="Next Page"
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            </div>
          </>
        )}
      </section>

      {/* Create / Edit Modal */}
      <EmployeeFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        employee={editingEmployee}
        onSubmit={handleFormSubmit}
      />

      {/* View Details Modal */}
      <EmployeeDetailsModal
        isOpen={Boolean(viewingEmployee)}
        onClose={() => setViewingEmployee(null)}
        employee={viewingEmployee}
        onEdit={(emp) => {
          setViewingEmployee(null);
          handleOpenEdit(emp);
        }}
      />

      {/* Delete Confirmation Modal */}
      <DeleteConfirmModal
        isOpen={Boolean(deletingEmployee)}
        onClose={() => setDeletingEmployee(null)}
        employee={deletingEmployee}
        onConfirm={async (empId) => {
          return await deleteEmployee(empId);
        }}
      />
    </div>
  );
}
