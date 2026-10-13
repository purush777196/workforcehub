import React, { useState, useMemo } from 'react';
import {
  Building2,
  Plus,
  Search,
  X,
  RotateCcw,
  RefreshCw,
  Eye,
  Edit,
  Trash2,
  Users,
  UserCheck,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import { useDepartments } from '../../context/useDepartments';
import DepartmentFormModal from './DepartmentFormModal';
import DepartmentDetailsModal from './DepartmentDetailsModal';
import DepartmentDeleteModal from './DepartmentDeleteModal';
import '../../styles/departments.css';
import '../../styles/employees.css';

export default function DepartmentsPage() {
  const {
    departments,
    totalDepartmentCount,
    departmentsWithManagersCount,
    totalEmployeesInDepartments,
    unassignedEmployeesCount,
    isLoading,
    error,
    refreshDepartments,
    getDepartmentEmployeeCount,
    getManagerEmployee,
    addDepartment,
    updateDepartment,
    deleteDepartment,
  } = useDepartments();

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [sortBy, setSortBy] = useState('name-asc'); // name-asc, name-desc, headcount-desc, headcount-asc, date-desc, date-asc

  // Modal states
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingDept, setEditingDept] = useState(null);
  const [viewingDept, setViewingDept] = useState(null);
  const [deletingDept, setDeletingDept] = useState(null);

  const isFilterActive = Boolean(searchQuery || selectedStatus || sortBy !== 'name-asc');

  const clearAllFilters = () => {
    setSearchQuery('');
    setSelectedStatus('');
    setSortBy('name-asc');
  };

  // Filtered and sorted departments
  const filteredDepartments = useMemo(() => {
    return departments
      .filter((dept) => {
        // Search by department name or manager name
        if (searchQuery.trim()) {
          const query = searchQuery.trim().toLowerCase();
          const nameMatch = dept.name.toLowerCase().includes(query);
          const idMatch = (dept.departmentId || dept.departmentCode || '').toLowerCase().includes(query);

          let managerMatch = false;
          const managerId = dept.managerEmployeeId || dept.managerId;
          if (managerId) {
            const manager = getManagerEmployee(managerId);
            if (manager) {
              const managerFullName = `${manager.firstName} ${manager.lastName}`.toLowerCase();
              managerMatch = managerFullName.includes(query);
            }
          }

          if (!nameMatch && !idMatch && !managerMatch) {
            return false;
          }
        }

        // Status filter
        if (selectedStatus && dept.status !== selectedStatus) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'name-asc') {
          return a.name.localeCompare(b.name);
        }
        if (sortBy === 'name-desc') {
          return b.name.localeCompare(a.name);
        }
        if (sortBy === 'headcount-desc') {
          const countA = getDepartmentEmployeeCount(a.name);
          const countB = getDepartmentEmployeeCount(b.name);
          return countB - countA;
        }
        if (sortBy === 'headcount-asc') {
          const countA = getDepartmentEmployeeCount(a.name);
          const countB = getDepartmentEmployeeCount(b.name);
          return countA - countB;
        }
        if (sortBy === 'date-desc') {
          return new Date(b.createdAt || '1970-01-01') - new Date(a.createdAt || '1970-01-01');
        }
        if (sortBy === 'date-asc') {
          return new Date(a.createdAt || '1970-01-01') - new Date(b.createdAt || '1970-01-01');
        }
        return 0;
      });
  }, [departments, searchQuery, selectedStatus, sortBy, getDepartmentEmployeeCount, getManagerEmployee]);

  // Modal handlers
  const handleOpenAdd = () => {
    setEditingDept(null);
    setIsFormOpen(true);
  };

  const handleOpenEdit = (dept) => {
    setEditingDept(dept);
    setIsFormOpen(true);
  };

  const handleOpenView = (dept) => {
    setViewingDept(dept);
  };

  const handleOpenDelete = (dept) => {
    setDeletingDept(dept);
  };

  const handleFormSubmit = async (formData) => {
    if (editingDept) {
      return await updateDepartment(editingDept.id, formData);
    }
    return await addDepartment(formData);
  };

  return (
    <div className="departments-page" id="departments-page-root">
      {/* Page Header */}
      <PageHeader
        title="Department Management"
        subtitle="Organize teams, assign managers, and monitor department headcount."
        badge={
          <Badge variant="primary" icon={Sparkles}>
            Phase 8C.2 REST Active
          </Badge>
        }
        actions={
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <button
              type="button"
              id="refresh-departments-btn"
              className="btn btn-secondary"
              onClick={refreshDepartments}
              disabled={isLoading}
              title="Refresh departments from PostgreSQL backend"
              aria-label="Refresh departments"
            >
              <RefreshCw size={15} className={isLoading ? 'spinning' : ''} />
              <span>Refresh</span>
            </button>
            <button
              type="button"
              id="add-department-btn"
              className="btn btn-primary"
              onClick={handleOpenAdd}
            >
              <Plus size={16} />
              <span>Add Department</span>
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
            onClick={refreshDepartments}
            disabled={isLoading}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <RotateCcw size={14} className={isLoading ? 'spinning' : ''} />
            <span>Retry</span>
          </button>
        </div>
      )}

      {/* Top 4 Summary Cards */}
      <section className="dept-summary-grid" aria-label="Department Overview Statistics">
        <StatCard
          id="stat-total-departments"
          title="Total Departments"
          value={totalDepartmentCount}
          trend={`${departments.filter((d) => d.status === 'Active').length} Active`}
          trendType="positive"
          footerText="Configured business divisions"
          icon={Building2}
          colorVariant="blue"
        />

        <StatCard
          id="stat-total-assigned-employees"
          title="Assigned Personnel"
          value={totalEmployeesInDepartments}
          trend="In active teams"
          trendType="positive"
          footerText="Staff attached to departments"
          icon={Users}
          colorVariant="green"
        />

        <StatCard
          id="stat-departments-with-managers"
          title="Manager Coverage"
          value={`${departmentsWithManagersCount}/${totalDepartmentCount}`}
          trend={`${totalDepartmentCount > 0 ? Math.round((departmentsWithManagersCount / totalDepartmentCount) * 100) : 0}% covered`}
          trendType={departmentsWithManagersCount === totalDepartmentCount ? 'positive' : 'neutral'}
          footerText="Divisions with designated leads"
          icon={UserCheck}
          colorVariant="purple"
        />

        <StatCard
          id="stat-unassigned-employees"
          title="Unassigned Staff"
          value={unassignedEmployeesCount}
          trend={unassignedEmployeesCount > 0 ? 'Requires allocation' : 'All assigned'}
          trendType={unassignedEmployeesCount > 0 ? 'warning' : 'positive'}
          footerText="Awaiting department allocation"
          icon={AlertCircle}
          colorVariant="amber"
        />
      </section>

      {/* Controls Bar: Search & Filter */}
      <section className="dept-controls-card" aria-label="Department Search and Filter Controls">
        <div className="dept-search-box">
          <Search size={16} className="search-icon" aria-hidden="true" />
          <input
            id="department-search-input"
            type="search"
            className="dept-search-input"
            placeholder="Search by department name or manager..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Search departments by name or manager"
          />
          {searchQuery && (
            <button
              type="button"
              className="search-clear-btn"
              onClick={() => setSearchQuery('')}
              aria-label="Clear search input"
            >
              <X size={13} />
            </button>
          )}
        </div>

        <div className="dept-filters-group">
          {/* Status Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="dept-status-filter">
              Status:
            </label>
            <select
              id="dept-status-filter"
              className="filter-select"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              aria-label="Filter departments by status"
            >
              <option value="">All Statuses</option>
              <option value="Active">Active</option>
              <option value="Inactive">Inactive</option>
            </select>
          </div>

          {/* Sort Control */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="dept-sort-select">
              Sort By:
            </label>
            <select
              id="dept-sort-select"
              className="filter-select"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              aria-label="Sort departments"
            >
              <option value="name-asc">Name (A → Z)</option>
              <option value="name-desc">Name (Z → A)</option>
              <option value="headcount-desc">Headcount (Highest first)</option>
              <option value="headcount-asc">Headcount (Lowest first)</option>
              <option value="date-desc">Creation Date (Newest first)</option>
              <option value="date-asc">Creation Date (Oldest first)</option>
            </select>
          </div>

          {/* Clear Filters Button */}
          {isFilterActive && (
            <button
              type="button"
              id="clear-dept-filters-btn"
              className="clear-filters-btn"
              onClick={clearAllFilters}
            >
              <RotateCcw size={13} />
              <span>Clear</span>
            </button>
          )}
        </div>
      </section>

      {/* Departments Table View */}
      <section className="table-card" aria-label="Department Directory Table">
        {isLoading && departments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '3.5rem 1rem', color: 'var(--color-text-muted)' }}>
            <RefreshCw size={32} className="spinning" style={{ margin: '0 auto 1rem', color: 'var(--color-primary)' }} />
            <p style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>Loading departments from PostgreSQL...</p>
            <p style={{ fontSize: '0.85rem' }}>Connecting to Spring Boot REST API</p>
          </div>
        ) : filteredDepartments.length === 0 ? (
          <div className="empty-state-box" id="dept-empty-state">
            <div className="empty-state-icon">
              <Building2 size={28} />
            </div>
            <h3 className="empty-state-title">No departments found</h3>
            <p className="empty-state-desc">
              {isFilterActive
                ? 'No department records match your search or filter criteria. Try clearing search filters.'
                : 'No departments currently registered. Click "Add Department" to configure the first division.'}
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
          <div className="table-responsive-wrapper">
            <table className="employee-table" id="departments-data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Department Name</th>
                  <th>Description</th>
                  <th>Manager</th>
                  <th>Headcount</th>
                  <th>Created Date</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredDepartments.map((dept) => {
                  const managerId = dept.managerEmployeeId || dept.managerId;
                  const manager = getManagerEmployee(managerId);
                  const memberCount = getDepartmentEmployeeCount(dept.name);
                  const managerInitials = manager
                    ? `${manager.firstName[0]}${manager.lastName[0]}`.toUpperCase()
                    : null;

                  return (
                    <tr key={dept.id} id={`dept-row-${dept.id}`}>
                      <td>
                        <span className="emp-id-chip">{dept.departmentId || dept.departmentCode}</span>
                      </td>

                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                          <span style={{ fontWeight: 700, color: 'var(--color-text-primary)' }}>
                            {dept.name}
                          </span>
                        </div>
                      </td>

                      <td>
                        <span
                          style={{
                            color: 'var(--color-text-secondary)',
                            fontSize: '0.8125rem',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            maxWidth: 320,
                          }}
                        >
                          {dept.description || '—'}
                        </span>
                      </td>

                      <td>
                        {manager ? (
                          <div className="dept-manager-cell">
                            <div className="dept-manager-avatar" aria-hidden="true">
                              {managerInitials}
                            </div>
                            <div className="dept-manager-info">
                              <span className="dept-manager-name">
                                {manager.firstName} {manager.lastName}
                              </span>
                              <span className="dept-manager-role">{manager.designation || manager.jobTitle}</span>
                            </div>
                          </div>
                        ) : (
                          <span
                            className="badge badge-neutral"
                            style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}
                          >
                            Unassigned
                          </span>
                        )}
                      </td>

                      <td>
                        <span className="dept-headcount-badge">
                          <Users size={12} />
                          {memberCount} member{memberCount !== 1 ? 's' : ''}
                        </span>
                      </td>

                      <td>
                        <span style={{ color: 'var(--color-text-muted)', fontSize: '0.8125rem' }}>
                          {dept.createdAt || '—'}
                        </span>
                      </td>

                      <td>
                        <Badge variant={dept.status === 'Active' ? 'success' : 'neutral'}>
                          {dept.status}
                        </Badge>
                      </td>

                      <td style={{ textAlign: 'right' }}>
                        <div className="emp-actions-cell" style={{ justifyContent: 'flex-end' }}>
                          {/* View Action */}
                          <button
                            type="button"
                            className="action-icon-btn"
                            title={`View ${dept.name}`}
                            onClick={() => handleOpenView(dept)}
                            aria-label={`View details for ${dept.name}`}
                          >
                            <Eye size={16} />
                          </button>

                          {/* Edit Action */}
                          <button
                            type="button"
                            className="action-icon-btn"
                            title={`Edit ${dept.name}`}
                            onClick={() => handleOpenEdit(dept)}
                            aria-label={`Edit ${dept.name}`}
                          >
                            <Edit size={16} />
                          </button>

                          {/* Delete Action */}
                          <button
                            type="button"
                            className="action-icon-btn danger"
                            title={`Delete ${dept.name}`}
                            onClick={() => handleOpenDelete(dept)}
                            aria-label={`Delete ${dept.name}`}
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
        )}
      </section>

      {/* Department Create / Edit Modal */}
      <DepartmentFormModal
        isOpen={isFormOpen}
        onClose={() => setIsFormOpen(false)}
        department={editingDept}
        onSubmit={handleFormSubmit}
      />

      {/* Department Details Modal */}
      <DepartmentDetailsModal
        isOpen={Boolean(viewingDept)}
        onClose={() => setViewingDept(null)}
        department={viewingDept}
        onEdit={(dept) => {
          setViewingDept(null);
          handleOpenEdit(dept);
        }}
      />

      {/* Department Delete Confirmation Modal */}
      <DepartmentDeleteModal
        isOpen={Boolean(deletingDept)}
        onClose={() => setDeletingDept(null)}
        department={deletingDept}
        onConfirm={async (deptId) => {
          return await deleteDepartment(deptId);
        }}
      />
    </div>
  );
}
