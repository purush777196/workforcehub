import React, { useState, useMemo, useCallback } from 'react';
import {
  Clock,
  UserCheck,
  UserX,
  CalendarOff,
  Percent,
  Search,
  X,
  RotateCcw,
  LogIn,
  LogOut,
  SlidersHorizontal,
  CheckCircle2,
  Sparkles,
  AlertCircle,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import StatCard from '../../components/common/StatCard';
import Badge from '../../components/common/Badge';
import { useEmployees } from '../../context/useEmployees';
import { useDepartments } from '../../context/useDepartments';
import { useAttendance } from '../../context/useAttendance';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';
import {
  formatTimeDisplay,
  formatWorkingDuration,
  calculateMinutesWorked,
} from '../../services/attendanceService';
import AttendanceStatusModal from './AttendanceStatusModal';
import '../../styles/attendance.css';
import '../../styles/employees.css';

export default function AttendancePage() {
  const { employees, totalCount } = useEmployees();
  const { departments } = useDepartments();
  const {
    todayDate,
    getRecord,
    checkIn,
    checkOut,
    updateAttendanceRecord,
    getDateStats,
    isLoading,
    isMutating,
    error,
    refreshAttendance,
  } = useAttendance();
  const { isEmployeeOnApprovedLeave, getApprovedLeaveOnDate } = useLeaves();
  const { user, isAdmin, isEmployee } = useAuth();

  // Selected date state (defaults to today)
  const [selectedDate, setSelectedDate] = useState(() => todayDate);

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDept, setSelectedDept] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [sortBy, setSortBy] = useState('name-asc'); // name-asc, name-desc, checkin-asc, checkin-desc

  // Admin status modal state
  const [managingEmployee, setManagingEmployee] = useState(null);

  const isToday = selectedDate === todayDate;

  // Compute summary stats for the selected date
  const dateStats = useMemo(() => getDateStats(selectedDate), [getDateStats, selectedDate]);

  // Dynamic department options
  const departmentOptions = useMemo(() => {
    const list = departments.map((d) => d.name);
    if (employees.some((e) => e.department === 'Unassigned')) {
      list.push('Unassigned');
    }
    return Array.from(new Set(list));
  }, [departments, employees]);

  const clearFilters = () => {
    setSearchQuery('');
    setSelectedDept('');
    setSelectedStatus('');
    setSortBy('name-asc');
  };

  const isFilterActive = Boolean(
    searchQuery || selectedDept || selectedStatus || sortBy !== 'name-asc'
  );

  // Resolve attendance status for an employee on the selected date
  const getEmployeeAttendanceInfo = useCallback((emp) => {
    const record = getRecord(emp.id, selectedDate);

    const onApprovedLeave = isEmployeeOnApprovedLeave(emp.id, selectedDate);
    if (emp.employmentStatus === 'On Leave' || onApprovedLeave) {
      const leaveDetail = getApprovedLeaveOnDate(emp.id, selectedDate);
      const leaveLabel = leaveDetail ? `${leaveDetail.leaveType} Leave` : 'On Approved Leave';
      return {
        record,
        status: 'On Leave',
        badgeVariant: 'warning',
        canLiveCheckIn: false,
        canLiveCheckOut: false,
        reason: leaveLabel,
      };
    }

    if (emp.employmentStatus === 'Inactive') {
      return {
        record,
        status: 'Inactive',
        badgeVariant: 'neutral',
        canLiveCheckIn: false,
        canLiveCheckOut: false,
        reason: 'Inactive Employee',
      };
    }

    if (!record) {
      return {
        record: null,
        status: isToday ? 'Not Checked In' : 'Absent',
        badgeVariant: isToday ? 'neutral' : 'danger',
        canLiveCheckIn: isToday,
        canLiveCheckOut: false,
        reason: isToday ? 'Pending Check-In' : 'Unrecorded',
      };
    }

    const hasCheckedIn = Boolean(record.checkIn);
    const hasCheckedOut = Boolean(record.checkOut);

    let badgeVariant = 'neutral';
    switch (record.status) {
      case 'Present':
        badgeVariant = 'success';
        break;
      case 'Late':
        badgeVariant = 'warning';
        break;
      case 'Half Day':
        badgeVariant = 'info';
        break;
      case 'Absent':
        badgeVariant = 'danger';
        break;
      default:
        badgeVariant = 'neutral';
    }

    return {
      record,
      status: record.status,
      badgeVariant,
      canLiveCheckIn: isToday && !hasCheckedIn && record.status !== 'Absent',
      canLiveCheckOut: isToday && hasCheckedIn && !hasCheckedOut,
      hasCompletedShift: hasCheckedIn && hasCheckedOut,
      reason: record.notes || '',
    };
  }, [getRecord, selectedDate, isToday, isEmployeeOnApprovedLeave, getApprovedLeaveOnDate]);

  // Derive list of eligible personnel for display (supports both ADMIN directory and EMPLOYEE portal)
  const displayEmployees = useMemo(() => {
    if (employees.length > 0) return employees;
    if (user && isEmployee) {
      const parts = (user.name || '').trim().split(/\s+/);
      return [
        {
          id: user.id,
          employeeId: user.employeeCode || 'EMP-1001',
          firstName: parts[0] || 'Employee',
          lastName: parts.slice(1).join(' ') || '',
          email: user.username || user.email,
          department: user.departmentName || user.department || 'Staff',
          jobTitle: user.jobTitle || 'Employee',
          employmentStatus: 'Active',
        },
      ];
    }
    return [];
  }, [employees, user, isEmployee]);

  // Build combined employee attendance list for table
  const combinedAttendanceList = useMemo(() => {
    return displayEmployees
      .map((emp) => {
        const info = getEmployeeAttendanceInfo(emp);
        const record = info.record;

        const checkInTime = record ? record.checkIn : null;
        const checkOutTime = record ? record.checkOut : null;
        const minutes = calculateMinutesWorked(checkInTime, checkOutTime, selectedDate);
        const inProgress = Boolean(checkInTime && !checkOutTime && isToday);
        const durationDisplay = formatWorkingDuration(minutes, inProgress);

        return {
          employee: emp,
          record,
          info,
          checkInTime,
          checkOutTime,
          minutes,
          durationDisplay,
        };
      })
      .filter(({ employee, info }) => {
        // Search query
        if (searchQuery.trim()) {
          const q = searchQuery.trim().toLowerCase();
          const fullName = `${employee.firstName} ${employee.lastName}`.toLowerCase();
          const empId = (employee.employeeId || '').toLowerCase();
          if (!fullName.includes(q) && !empId.includes(q)) {
            return false;
          }
        }

        // Department filter
        if (selectedDept && employee.department !== selectedDept) {
          return false;
        }

        // Status filter
        if (selectedStatus && info.status !== selectedStatus) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'name-asc') {
          const nameA = `${a.employee.firstName} ${a.employee.lastName}`.toLowerCase();
          const nameB = `${b.employee.firstName} ${b.employee.lastName}`.toLowerCase();
          return nameA.localeCompare(nameB);
        }
        if (sortBy === 'name-desc') {
          const nameA = `${a.employee.firstName} ${a.employee.lastName}`.toLowerCase();
          const nameB = `${b.employee.firstName} ${b.employee.lastName}`.toLowerCase();
          return nameB.localeCompare(nameA);
        }
        if (sortBy === 'checkin-asc') {
          const timeA = a.checkInTime || '99:99';
          const timeB = b.checkInTime || '99:99';
          return timeA.localeCompare(timeB);
        }
        if (sortBy === 'checkin-desc') {
          const timeA = a.checkInTime || '00:00';
          const timeB = b.checkInTime || '00:00';
          return timeB.localeCompare(timeA);
        }
        return 0;
      });
  }, [
    displayEmployees,
    selectedDate,
    searchQuery,
    selectedDept,
    selectedStatus,
    sortBy,
    isToday,
    getEmployeeAttendanceInfo,
  ]);

  const handleAdminStatusSubmit = async (formData) => {
    return await updateAttendanceRecord(formData);
  };

  return (
    <div className="attendance-page" id="attendance-page-root">
      {/* Header */}
      <PageHeader
        title="Attendance Management"
        subtitle="Shift logs, daily check-ins, punctuality metrics, and working hour records."
        badge={
          <Badge variant="primary" icon={Sparkles}>
            Phase 8C.3 REST Active
          </Badge>
        }
        actions={
          <div className="attendance-date-picker-group">
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--color-text-secondary)' }}>
              Date:
            </span>
            <input
              id="attendance-date-picker"
              type="date"
              className="attendance-date-input"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              aria-label="Select attendance date"
            />
            {!isToday && (
              <button
                type="button"
                className="attendance-today-btn"
                onClick={() => setSelectedDate(todayDate)}
              >
                Today
              </button>
            )}
            <button
              type="button"
              className="action-icon-btn"
              id="refresh-attendance-btn"
              title="Refresh attendance records from backend"
              onClick={() => refreshAttendance()}
              disabled={isLoading || isMutating}
              aria-label="Refresh attendance records"
              style={{ padding: '0.45rem', borderRadius: 'var(--radius-md)' }}
            >
              <RotateCcw size={15} className={isLoading ? 'spin-icon' : ''} />
            </button>
          </div>
        }
      />

      {/* Error notification banner */}
      {error && (
        <section
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            padding: '0.85rem 1.25rem',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid var(--color-danger, #ef4444)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--color-danger, #ef4444)',
            marginBottom: '1.25rem',
          }}
          role="alert"
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={18} />
            <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>
              Backend Connection Warning: {error}
            </span>
          </div>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => refreshAttendance()}
            style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
          >
            Retry Connection
          </button>
        </section>
      )}

      {/* 5 Summary Stat Cards */}
      <section className="attendance-summary-grid" aria-label="Attendance Overview Statistics">
        <StatCard
          id="stat-attendance-total-employees"
          title="Eligible Staff"
          value={dateStats.totalEligible}
          trend={`${totalCount} in registry`}
          trendType="neutral"
          footerText="Excludes inactive personnel"
          icon={UserCheck}
          colorVariant="blue"
        />

        <StatCard
          id="stat-attendance-present"
          title="Present Today"
          value={dateStats.totalAttending}
          trend={`${dateStats.presentCount} on-time, ${dateStats.lateCount} late`}
          trendType="positive"
          footerText={`${dateStats.halfDayCount} half-day shifts logged`}
          icon={Clock}
          colorVariant="green"
        />

        <StatCard
          id="stat-attendance-absent"
          title="Absent"
          value={dateStats.totalAbsent}
          trend={dateStats.totalAbsent > 0 ? 'Requires attention' : 'Zero unexcused'}
          trendType={dateStats.totalAbsent > 0 ? 'warning' : 'positive'}
          footerText="Unrecorded or marked absent"
          icon={UserX}
          colorVariant="amber"
        />

        <StatCard
          id="stat-attendance-leave"
          title="On Leave"
          value={dateStats.onLeaveCount}
          trend="Approved policy leave"
          trendType="neutral"
          footerText="Exempt from absence penalties"
          icon={CalendarOff}
          colorVariant="purple"
        />

        <StatCard
          id="stat-attendance-rate"
          title="Attendance Rate"
          value={`${dateStats.attendanceRate}%`}
          trend={dateStats.attendanceRate >= 85 ? 'Healthy rate' : 'Below benchmark'}
          trendType={dateStats.attendanceRate >= 85 ? 'positive' : 'warning'}
          footerText="Against eligible staff denominator"
          icon={Percent}
          colorVariant="blue"
        />
      </section>

      {/* Controls Bar: Search, Filters, Sorting */}
      <section className="attendance-controls-card" aria-label="Attendance Filter Controls">
        <div className="attendance-top-controls-row">
          {/* Search Box */}
          <div className="employee-search-box">
            <Search size={16} className="search-icon" aria-hidden="true" />
            <input
              id="attendance-search-input"
              type="search"
              className="employee-search-input"
              placeholder="Search by employee name or ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search employees in attendance"
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

          {/* Sort Control */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="attendance-sort-select">
              Sort By:
            </label>
            <select
              id="attendance-sort-select"
              className="filter-select"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              aria-label="Sort attendance list"
            >
              <option value="name-asc">Name (A → Z)</option>
              <option value="name-desc">Name (Z → A)</option>
              <option value="checkin-asc">Check-In Time (Earliest first)</option>
              <option value="checkin-desc">Check-In Time (Latest first)</option>
            </select>
          </div>
        </div>

        {/* Second Row: Combined Filters */}
        <div className="filters-row">
          {/* Department Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="attendance-dept-filter">
              Department:
            </label>
            <select
              id="attendance-dept-filter"
              className="filter-select"
              value={selectedDept}
              onChange={(e) => setSelectedDept(e.target.value)}
              aria-label="Filter by department"
            >
              <option value="">All Departments</option>
              {departmentOptions.map((dept) => (
                <option key={dept} value={dept}>
                  {dept}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="filter-group">
            <label className="filter-label" htmlFor="attendance-status-filter">
              Status:
            </label>
            <select
              id="attendance-status-filter"
              className="filter-select"
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              aria-label="Filter by attendance status"
            >
              <option value="">All Statuses</option>
              <option value="Present">Present</option>
              <option value="Late">Late</option>
              <option value="Half Day">Half Day</option>
              <option value="Absent">Absent</option>
              <option value="On Leave">On Leave</option>
              {isToday && <option value="Not Checked In">Not Checked In</option>}
            </select>
          </div>

          {/* Clear Filters Button */}
          {isFilterActive && (
            <button
              type="button"
              id="clear-attendance-filters-btn"
              className="clear-filters-btn"
              onClick={clearFilters}
            >
              <RotateCcw size={13} />
              <span>Clear</span>
            </button>
          )}
        </div>
      </section>

      {/* Attendance Table */}
      <section className="table-card" aria-label="Daily Attendance Table">
        {isLoading && combinedAttendanceList.length === 0 ? (
          <div className="empty-state-box" id="attendance-loading-state">
            <div className="empty-state-icon">
              <RotateCcw size={28} className="spin-icon" />
            </div>
            <h3 className="empty-state-title">Loading Attendance Records</h3>
            <p className="empty-state-desc">Synchronizing attendance records from PostgreSQL database...</p>
          </div>
        ) : combinedAttendanceList.length === 0 ? (
          <div className="empty-state-box" id="attendance-empty-state">
            <div className="empty-state-icon">
              <Clock size={28} />
            </div>
            <h3 className="empty-state-title">No attendance records found</h3>
            <p className="empty-state-desc">
              {isFilterActive
                ? 'No employee attendance records match your current search and filter criteria.'
                : `No personnel records configured for ${selectedDate}.`}
            </p>
            {isFilterActive && (
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={clearFilters}
              >
                <RotateCcw size={14} />
                <span>Reset All Filters</span>
              </button>
            )}
          </div>
        ) : (
          <div className="table-responsive-wrapper">
            <table className="employee-table" id="attendance-data-table">
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>ID</th>
                  <th>Department</th>
                  <th>Date</th>
                  <th>Check-In</th>
                  <th>Check-Out</th>
                  <th>Duration</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {combinedAttendanceList.map(
                  ({
                    employee,
                    info,
                    checkInTime,
                    checkOutTime,
                    durationDisplay,
                  }) => {
                    const initials = `${employee.firstName[0]}${employee.lastName[0]}`.toUpperCase();

                    return (
                      <tr key={employee.id} id={`att-row-${employee.id}`}>
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
                              <span className="emp-type-label">{employee.designation}</span>
                            </div>
                          </div>
                        </td>

                        {/* Employee ID */}
                        <td>
                          <span className="emp-id-chip">{employee.employeeId}</span>
                        </td>

                        {/* Department */}
                        <td>
                          <span style={{ fontWeight: 500 }}>{employee.department}</span>
                        </td>

                        {/* Date */}
                        <td>
                          <span style={{ fontSize: '0.8125rem', color: 'var(--color-text-muted)' }}>
                            {selectedDate}
                          </span>
                        </td>

                        {/* Check-In */}
                        <td>
                          <span style={{ fontWeight: 600, color: checkInTime ? 'var(--color-text-primary)' : 'var(--color-text-tertiary)' }}>
                            {formatTimeDisplay(checkInTime)}
                          </span>
                        </td>

                        {/* Check-Out */}
                        <td>
                          <span style={{ fontWeight: 600, color: checkOutTime ? 'var(--color-text-primary)' : 'var(--color-text-tertiary)' }}>
                            {formatTimeDisplay(checkOutTime)}
                          </span>
                        </td>

                        {/* Duration */}
                        <td>
                          <span className={`duration-pill ${checkInTime && !checkOutTime && isToday ? 'active' : ''}`}>
                            <Clock size={12} />
                            {durationDisplay}
                          </span>
                        </td>

                        {/* Status Badge */}
                        <td>
                          <Badge variant={info.badgeVariant}>{info.status}</Badge>
                        </td>

                        {/* Actions */}
                        <td style={{ textAlign: 'right' }}>
                          <div className="att-action-btn-group">
                            {/* Live Check-In Button (Today only) */}
                            {info.canLiveCheckIn && (
                              <button
                                type="button"
                                className="btn-checkin"
                                id={`checkin-btn-${employee.id}`}
                                title={`Check in ${employee.firstName}`}
                                onClick={() => checkIn(employee.id)}
                                disabled={isMutating}
                              >
                                <LogIn size={13} />
                                <span>Check In</span>
                              </button>
                            )}

                            {/* Live Check-Out Button (Today only) */}
                            {info.canLiveCheckOut && (
                              <button
                                type="button"
                                className="btn-checkout"
                                id={`checkout-btn-${employee.id}`}
                                title={`Check out ${employee.firstName}`}
                                onClick={() => checkOut(employee.id)}
                                disabled={isMutating}
                              >
                                <LogOut size={13} />
                                <span>Check Out</span>
                              </button>
                            )}

                            {/* Shift Completed Indicator */}
                            {info.hasCompletedShift && isToday && (
                              <span className="completed-badge">
                                <CheckCircle2 size={13} color="var(--color-success)" />
                                <span>Completed</span>
                              </span>
                            )}

                            {/* Leave or Inactive indicator if applicable */}
                            {(employee.employmentStatus === 'On Leave' || employee.employmentStatus === 'Inactive') && (
                              <span
                                style={{
                                  fontSize: '0.72rem',
                                  color: 'var(--color-text-muted)',
                                  fontStyle: 'italic',
                                }}
                              >
                                {employee.employmentStatus}
                              </span>
                            )}

                            {/* Admin Manual Manage Status Button */}
                            {isAdmin && (
                              <button
                                type="button"
                                className="action-icon-btn"
                                id={`manage-att-btn-${employee.id}`}
                                title={`Manage attendance status for ${employee.firstName}`}
                                onClick={() => setManagingEmployee(employee)}
                                aria-label={`Manage attendance for ${employee.firstName}`}
                              >
                                <SlidersHorizontal size={15} />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  }
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Admin Status Modal */}
      {managingEmployee && (
        <AttendanceStatusModal
          isOpen={Boolean(managingEmployee)}
          onClose={() => setManagingEmployee(null)}
          employee={managingEmployee}
          record={getRecord(managingEmployee.id, selectedDate)}
          date={selectedDate}
          onSubmit={handleAdminStatusSubmit}
        />
      )}
    </div>
  );
}
