import React, { useState, useMemo } from 'react';
import {
  CalendarOff,
  ClockAlert,
  CheckCircle2,
  XCircle,
  Users,
  Search,
  Plus,
  RotateCcw,
  SlidersHorizontal,
  Eye,
  Check,
  X,
  Trash2,
  Edit2,
  Calendar,
  Sparkles,
  PieChart,
  FileCheck2,
  AlertCircle,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import { useEmployees } from '../../context/useEmployees';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';
import ApplyLeaveModal from './ApplyLeaveModal';
import ReviewLeaveModal from './ReviewLeaveModal';
import EditLeaveModal from './EditLeaveModal';
import LeaveDetailsModal from './LeaveDetailsModal';
import LeaveBalancesView from './LeaveBalancesView';
import '../../styles/leaves.css';
import '../../styles/employees.css';

const TODAY_FORMATTED = new Date().toLocaleDateString('en-US', {
  weekday: 'short',
  month: 'short',
  day: 'numeric',
  year: 'numeric',
});

export default function LeavesPage() {
  const { employees } = useEmployees();
  const { user, isAdmin } = useAuth();
  const {
    leaves,
    totalRequestsCount,
    pendingCount,
    approvedCount,
    rejectedCount,
    todayOnLeaveCount,
    cancelLeave,
    isLoading,
    isMutating,
    error,
    refreshLeaves,
  } = useLeaves();

  const currentEmployee = useMemo(() => {
    if (!user) return null;
    return employees.find(
      (e) => (user.email && e.email?.toLowerCase() === user.email.toLowerCase()) || e.id === user.employeeId
    );
  }, [user, employees]);

  // Active tab: 'requests' | 'balances'
  const [activeTab, setActiveTab] = useState('requests');

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [selectedType, setSelectedType] = useState('');
  const [filterStartDate, setFilterStartDate] = useState('');
  const [filterEndDate, setFilterEndDate] = useState('');
  const [sortBy, setSortBy] = useState('submitted-desc');

  // Modals state
  const [isApplyOpen, setIsApplyOpen] = useState(false);
  const [reviewModalState, setReviewModalState] = useState({
    isOpen: false,
    request: null,
    employee: null,
    mode: 'approve', // 'approve' | 'reject'
  });
  const [editModalState, setEditModalState] = useState({
    isOpen: false,
    request: null,
    employee: null,
  });
  const [detailsModalState, setDetailsModalState] = useState({
    isOpen: false,
    request: null,
    employee: null,
  });
  const [cancelConfirmState, setCancelConfirmState] = useState({
    isOpen: false,
    request: null,
    employee: null,
  });

  // Filtered & Sorted Requests
  const filteredRequests = useMemo(() => {
    return leaves
      .map((req) => {
        const employee = employees.find((e) => e.id === req.employeeId) || {
          firstName: 'Former',
          lastName: 'Employee',
          employeeId: req.employeeId,
          department: 'Unassigned',
        };
        return { req, employee };
      })
      .filter(({ req, employee }) => {
        // Search query (employee name, ID, or reason)
        if (searchQuery.trim()) {
          const q = searchQuery.trim().toLowerCase();
          const fullName = `${employee.firstName} ${employee.lastName}`.toLowerCase();
          const empId = (employee.employeeId || '').toLowerCase();
          const reason = (req.reason || '').toLowerCase();
          if (!fullName.includes(q) && !empId.includes(q) && !reason.includes(q)) {
            return false;
          }
        }

        // Status filter
        if (selectedStatus && req.status !== selectedStatus) {
          return false;
        }

        // Leave type filter
        if (selectedType && req.leaveType !== selectedType) {
          return false;
        }

        // Date range filter
        if (filterStartDate && req.endDate < filterStartDate) {
          return false;
        }
        if (filterEndDate && req.startDate > filterEndDate) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'submitted-desc') {
          return new Date(b.req.submittedAt) - new Date(a.req.submittedAt);
        }
        if (sortBy === 'submitted-asc') {
          return new Date(a.req.submittedAt) - new Date(b.req.submittedAt);
        }
        if (sortBy === 'start-asc') {
          return a.req.startDate.localeCompare(b.req.startDate);
        }
        if (sortBy === 'start-desc') {
          return b.req.startDate.localeCompare(a.req.startDate);
        }
        if (sortBy === 'name-asc') {
          const nameA = `${a.employee.firstName} ${a.employee.lastName}`.toLowerCase();
          const nameB = `${b.employee.firstName} ${b.employee.lastName}`.toLowerCase();
          return nameA.localeCompare(nameB);
        }
        return 0;
      });
  }, [
    leaves,
    employees,
    searchQuery,
    selectedStatus,
    selectedType,
    filterStartDate,
    filterEndDate,
    sortBy,
  ]);

  const clearFilters = () => {
    setSearchQuery('');
    setSelectedStatus('');
    setSelectedType('');
    setFilterStartDate('');
    setFilterEndDate('');
    setSortBy('submitted-desc');
  };

  const isFilterActive = Boolean(
    searchQuery ||
      selectedStatus ||
      selectedType ||
      filterStartDate ||
      filterEndDate ||
      sortBy !== 'submitted-desc'
  );

  const handleOpenReview = (request, employee, mode) => {
    setReviewModalState({
      isOpen: true,
      request,
      employee,
      mode,
    });
  };

  const handleOpenEdit = (request, employee) => {
    setEditModalState({
      isOpen: true,
      request,
      employee,
    });
  };

  const handleOpenDetails = (request, employee) => {
    setDetailsModalState({
      isOpen: true,
      request,
      employee,
    });
  };

  const handleOpenCancel = (request, employee) => {
    setCancelConfirmState({
      isOpen: true,
      request,
      employee,
    });
  };

  const handleConfirmCancel = () => {
    if (cancelConfirmState.request) {
      cancelLeave(cancelConfirmState.request.id);
      setCancelConfirmState({ isOpen: false, request: null, employee: null });
    }
  };

  return (
    <div className="leaves-page" id="leaves-page-root">
      {/* Page Header */}
      <PageHeader
        title="Leave Management"
        subtitle="Review requests and manage employee leave"
        badge={
          <Badge variant="success" icon={Sparkles}>
            Phase 8C.4 REST Active
          </Badge>
        }
        actions={
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.45rem',
                fontSize: '0.85rem',
                color: 'var(--color-text-secondary)',
                backgroundColor: 'var(--color-bg-surface)',
                border: '1px solid var(--border-color)',
                padding: '0.45rem 0.85rem',
                borderRadius: 'var(--radius-md)',
              }}
            >
              <Calendar size={15} color="var(--color-primary)" />
              <span>{TODAY_FORMATTED}</span>
            </div>
            <button
              type="button"
              id="refresh-leaves-btn"
              className="btn btn-secondary"
              onClick={refreshLeaves}
              disabled={isLoading || isMutating}
              title="Refresh leaves from database"
            >
              <RotateCcw size={15} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            <button
              type="button"
              id="apply-leave-btn"
              className="btn btn-primary"
              onClick={() => setIsApplyOpen(true)}
            >
              <Plus size={16} /> Apply for Leave
            </button>
          </div>
        }
      />

      {error && (
        <div className="modal-notice-box danger" style={{ margin: '1rem 0' }}>
          <AlertCircle size={18} style={{ minWidth: 18 }} />
          <div style={{ flex: 1 }}>
            <strong>Backend Connection Issue</strong>
            <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.825rem' }}>{error}</p>
          </div>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={refreshLeaves}
            style={{ marginLeft: 'auto' }}
          >
            <RotateCcw size={12} /> Retry
          </button>
        </div>
      )}

      {/* 5 Summary Stat Cards */}
      <section className="leaves-stats-grid" aria-label="Leave Overview Statistics">
        <StatCard
          id="stat-total-leaves"
          title="Total Requests"
          value={totalRequestsCount}
          trend="All recorded"
          trendType="neutral"
          footerText="Lifetime leave submissions"
          icon={CalendarOff}
          colorVariant="blue"
        />

        <StatCard
          id="stat-pending-leaves"
          title="Pending Requests"
          value={pendingCount}
          trend={pendingCount > 0 ? 'Requires review' : 'Queue clear'}
          trendType={pendingCount > 0 ? 'warning' : 'positive'}
          footerText="Awaiting HR sign-off"
          icon={ClockAlert}
          colorVariant="purple"
        />

        <StatCard
          id="stat-approved-leaves"
          title="Approved Requests"
          value={approvedCount}
          trend="Authorized"
          trendType="positive"
          footerText="Active & upcoming time-off"
          icon={CheckCircle2}
          colorVariant="green"
        />

        <StatCard
          id="stat-rejected-leaves"
          title="Rejected Requests"
          value={rejectedCount}
          trend="Declined"
          trendType="neutral"
          footerText="With reviewer comments"
          icon={XCircle}
          colorVariant="amber"
        />

        <StatCard
          id="stat-today-on-leave"
          title="On Leave Today"
          value={todayOnLeaveCount}
          trend="Currently away"
          trendType="neutral"
          footerText="Exempt from attendance"
          icon={Users}
          colorVariant="blue"
        />
      </section>

      {/* Module Tabs Navigation */}
      <nav className="leaves-tabs" aria-label="Leave Sections">
        <button
          type="button"
          id="tab-leave-requests"
          className={`leaves-tab-btn ${activeTab === 'requests' ? 'active' : ''}`}
          onClick={() => setActiveTab('requests')}
        >
          <FileCheck2 size={16} />
          <span>Leave Requests</span>
          <span className="leaves-tab-badge">{totalRequestsCount}</span>
        </button>

        <button
          type="button"
          id="tab-leave-balances"
          className={`leaves-tab-btn ${activeTab === 'balances' ? 'active' : ''}`}
          onClick={() => setActiveTab('balances')}
        >
          <PieChart size={16} />
          <span>Leave Balances & Policies</span>
        </button>
      </nav>

      {/* Tab 1: Leave Requests Directory */}
      {activeTab === 'requests' && (
        <section aria-label="Leave Requests Directory">
          {/* Filter Bar */}
          <div className="leaves-filter-bar">
            {/* Top Row: Search and Sort */}
            <div className="leaves-filter-row-top">
              <div className="leaves-search-wrap">
                <Search size={16} className="leaves-search-icon" />
                <input
                  type="text"
                  id="search-leaves-input"
                  className="leaves-search-input"
                  placeholder="Search by employee name, ID, or reason..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>

              <div className="leaves-select-control" style={{ marginLeft: 'auto' }}>
                <label htmlFor="sort-leaves-select">
                  <SlidersHorizontal size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
                  Sort By:
                </label>
                <select
                  id="sort-leaves-select"
                  className="leaves-select"
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                >
                  <option value="submitted-desc">Submission Date (Newest first)</option>
                  <option value="submitted-asc">Submission Date (Oldest first)</option>
                  <option value="start-asc">Start Date (Earliest first)</option>
                  <option value="start-desc">Start Date (Latest first)</option>
                  <option value="name-asc">Employee Name (A → Z)</option>
                </select>
              </div>
            </div>

            {/* Bottom Row: Status, Leave Type, Date Range & Clear */}
            <div className="leaves-filter-row-bottom">
              {/* Status Filter */}
              <div className="leaves-select-control">
                <label htmlFor="filter-status-select">Status:</label>
                <select
                  id="filter-status-select"
                  className="leaves-select"
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                >
                  <option value="">All Statuses</option>
                  <option value="Pending">Pending</option>
                  <option value="Approved">Approved</option>
                  <option value="Rejected">Rejected</option>
                </select>
              </div>

              {/* Leave Type Filter */}
              <div className="leaves-select-control">
                <label htmlFor="filter-type-select">Type:</label>
                <select
                  id="filter-type-select"
                  className="leaves-select"
                  value={selectedType}
                  onChange={(e) => setSelectedType(e.target.value)}
                >
                  <option value="">All Types</option>
                  <option value="Annual">Annual Leave</option>
                  <option value="Sick">Sick Leave</option>
                  <option value="Personal">Personal Leave</option>
                  <option value="Unpaid">Unpaid Leave</option>
                </select>
              </div>

              {/* Date Range Start */}
              <div className="leaves-date-filter">
                <label htmlFor="filter-start-date">From:</label>
                <input
                  type="date"
                  id="filter-start-date"
                  className="leaves-date-input"
                  value={filterStartDate}
                  onChange={(e) => setFilterStartDate(e.target.value)}
                />
              </div>

              {/* Date Range End */}
              <div className="leaves-date-filter">
                <label htmlFor="filter-end-date">To:</label>
                <input
                  type="date"
                  id="filter-end-date"
                  className="leaves-date-input"
                  value={filterEndDate}
                  min={filterStartDate || undefined}
                  onChange={(e) => setFilterEndDate(e.target.value)}
                />
              </div>

              {/* Clear Filters Button */}
              {isFilterActive && (
                <button
                  type="button"
                  id="clear-leaves-filters-btn"
                  className="leaves-clear-btn"
                  onClick={clearFilters}
                  title="Reset all filters"
                >
                  <RotateCcw size={13} /> Clear Filters
                </button>
              )}
            </div>
          </div>

          {/* Table Container */}
          <div
            className="table-responsive"
            style={{
              background: 'var(--color-bg-surface)',
              borderRadius: 'var(--radius-lg)',
              border: '1px solid var(--border-color)',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <table className="data-table" aria-label="Leave Requests Table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>ID</th>
                  <th>Department</th>
                  <th>Leave Type</th>
                  <th>Date Range</th>
                  <th>Days</th>
                  <th>Submitted</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredRequests.length === 0 ? (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '3rem' }}>
                      <CalendarOff
                        size={36}
                        style={{ color: 'var(--color-text-tertiary)', margin: '0 auto 0.75rem' }}
                      />
                      <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                        No Leave Requests Found
                      </h3>
                      <p style={{ fontSize: '0.825rem', color: 'var(--color-text-secondary)', marginTop: '0.25rem' }}>
                        {isFilterActive
                          ? 'Try adjusting your search query, status, or date filters.'
                          : 'No leave applications have been submitted yet.'}
                      </p>
                      {isFilterActive && (
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ marginTop: '0.85rem' }}
                          onClick={clearFilters}
                        >
                          Reset Filters
                        </button>
                      )}
                    </td>
                  </tr>
                ) : (
                  filteredRequests.map(({ req, employee }) => {
                    const initials = `${employee.firstName[0]}${employee.lastName[0]}`.toUpperCase();
                    const isPending = req.status === 'Pending';
                    const isApproved = req.status === 'Approved';
                    const isRejected = req.status === 'Rejected';

                    let statusVariant = 'warning';
                    if (isApproved) statusVariant = 'success';
                    if (isRejected) statusVariant = 'danger';

                    let typeVariant = 'primary';
                    if (req.leaveType === 'Sick') typeVariant = 'warning';
                    if (req.leaveType === 'Personal') typeVariant = 'info';
                    if (req.leaveType === 'Unpaid') typeVariant = 'neutral';

                    const formattedSubmitted = new Date(req.submittedAt).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                      year: 'numeric',
                    });

                    return (
                      <tr key={req.id} id={`leave-row-${req.id}`}>
                        {/* Employee Avatar & Name */}
                        <td>
                          <div className="emp-name-cell">
                            <div className="emp-avatar" aria-hidden="true">
                              {initials}
                            </div>
                            <div className="emp-name-text">
                              <span className="emp-full-name">
                                {employee.firstName} {employee.lastName}
                              </span>
                              <span className="emp-id-label">{employee.designation || 'Staff'}</span>
                            </div>
                          </div>
                        </td>

                        {/* Employee ID */}
                        <td>
                          <span className="emp-dept-tag">{employee.employeeId}</span>
                        </td>

                        {/* Department */}
                        <td>
                          <span className="emp-dept-tag">{employee.department}</span>
                        </td>

                        {/* Leave Type */}
                        <td>
                          <Badge variant={typeVariant}>{req.leaveType}</Badge>
                        </td>

                        {/* Date Range */}
                        <td>
                          <span style={{ fontSize: '0.825rem', fontWeight: 500 }}>
                            {req.startDate} <span style={{ color: 'var(--color-text-tertiary)' }}>→</span> {req.endDate}
                          </span>
                        </td>

                        {/* Days Pill */}
                        <td>
                          <span className="leave-days-pill">
                            {req.requestedDays} {req.requestedDays === 1 ? 'day' : 'days'}
                          </span>
                        </td>

                        {/* Submitted Date */}
                        <td>
                          <span style={{ fontSize: '0.8rem', color: 'var(--color-text-secondary)' }}>
                            {formattedSubmitted}
                          </span>
                        </td>

                        {/* Status Badge */}
                        <td>
                          <Badge variant={statusVariant}>{req.status}</Badge>
                        </td>

                        {/* Actions */}
                        <td>
                          <div className="leave-actions-group">
                            {/* View Details always available */}
                            <button
                              type="button"
                              className="btn-action-icon"
                              title="View details"
                              aria-label={`View details for ${employee.firstName}'s leave request`}
                              onClick={() => handleOpenDetails(req, employee)}
                            >
                              <Eye size={15} />
                            </button>

                            {/* Pending-only actions: Approve, Reject, Edit, Cancel */}
                            {isPending && (
                              <>
                                {isAdmin && (
                                  <>
                                    <button
                                      type="button"
                                      className="btn-action-icon approve"
                                      title="Approve request"
                                      aria-label={`Approve ${employee.firstName}'s leave`}
                                      onClick={() => handleOpenReview(req, employee, 'approve')}
                                    >
                                      <Check size={15} />
                                    </button>

                                    <button
                                      type="button"
                                      className="btn-action-icon reject"
                                      title="Reject request"
                                      aria-label={`Reject ${employee.firstName}'s leave`}
                                      onClick={() => handleOpenReview(req, employee, 'reject')}
                                    >
                                      <X size={15} />
                                    </button>
                                  </>
                                )}

                                {(isAdmin || (currentEmployee && req.employeeId === currentEmployee.id)) && (
                                  <>
                                    <button
                                      type="button"
                                      className="btn-action-icon"
                                      title="Edit request"
                                      aria-label={`Edit ${employee.firstName}'s leave request`}
                                      onClick={() => handleOpenEdit(req, employee)}
                                    >
                                      <Edit2 size={14} />
                                    </button>

                                    <button
                                      type="button"
                                      className="btn-action-icon cancel"
                                      title="Cancel pending request"
                                      aria-label={`Cancel ${employee.firstName}'s pending request`}
                                      onClick={() => handleOpenCancel(req, employee)}
                                    >
                                      <Trash2 size={14} />
                                    </button>
                                  </>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Tab 2: Leave Balances & Policies View */}
      {activeTab === 'balances' && <LeaveBalancesView />}

      {/* Apply Leave Modal */}
      <ApplyLeaveModal isOpen={isApplyOpen} onClose={() => setIsApplyOpen(false)} />

      {/* Review Modal (Approve or Reject) */}
      <ReviewLeaveModal
        isOpen={reviewModalState.isOpen}
        request={reviewModalState.request}
        employee={reviewModalState.employee}
        mode={reviewModalState.mode}
        onClose={() =>
          setReviewModalState({ isOpen: false, request: null, employee: null, mode: 'approve' })
        }
      />

      {/* Edit Modal */}
      {editModalState.isOpen && editModalState.request && (
        <EditLeaveModal
          key={editModalState.request.id}
          isOpen={editModalState.isOpen}
          request={editModalState.request}
          employee={editModalState.employee}
          onClose={() => setEditModalState({ isOpen: false, request: null, employee: null })}
        />
      )}

      {/* View Details Modal */}
      <LeaveDetailsModal
        isOpen={detailsModalState.isOpen}
        request={detailsModalState.request}
        employee={detailsModalState.employee}
        onClose={() => setDetailsModalState({ isOpen: false, request: null, employee: null })}
      />

      {/* Cancel Confirmation Dialog */}
      {cancelConfirmState.isOpen && cancelConfirmState.request && (
        <div
          className="modal-overlay"
          role="dialog"
          aria-modal="true"
          aria-labelledby="cancel-confirm-title"
          onClick={() => setCancelConfirmState({ isOpen: false, request: null, employee: null })}
        >
          <div
            className="modal-container"
            style={{ maxWidth: 460 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-header">
              <div className="modal-title-wrap">
                <h2 id="cancel-confirm-title" className="modal-title">
                  Cancel Leave Request?
                </h2>
                <p className="modal-subtitle">Confirm removal of pending application.</p>
              </div>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setCancelConfirmState({ isOpen: false, request: null, employee: null })}
              >
                <X size={20} />
              </button>
            </div>

            <div className="modal-body">
              <p style={{ fontSize: '0.875rem', color: 'var(--color-text-secondary)', lineHeight: 1.5 }}>
                Are you sure you want to cancel the pending{' '}
                <strong>{cancelConfirmState.request.leaveType} Leave</strong> request for{' '}
                <strong>
                  {cancelConfirmState.employee?.firstName} {cancelConfirmState.employee?.lastName}
                </strong>{' '}
                ({cancelConfirmState.request.startDate} to {cancelConfirmState.request.endDate})?
              </p>
              <div className="modal-notice-box warning" style={{ marginTop: '0.75rem' }}>
                <ClockAlert size={16} style={{ minWidth: 16 }} />
                <span>This action will withdraw the application from the approval queue.</span>
              </div>
            </div>

            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setCancelConfirmState({ isOpen: false, request: null, employee: null })}
              >
                Keep Request
              </button>
              <button
                type="button"
                id="confirm-cancel-leave-btn"
                className="btn btn-danger"
                style={{ backgroundColor: '#dc2626', borderColor: '#dc2626', color: '#fff' }}
                onClick={handleConfirmCancel}
              >
                Confirm Cancellation
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
