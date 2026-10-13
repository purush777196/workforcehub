import React, { useState, useMemo } from 'react';
import {
  Calendar,
  Info,
  Users,
  Search,
} from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';
import { useLeaves } from '../../context/useLeaves';
import { useAuth } from '../../context/useAuth';
import { LEAVE_POLICIES } from '../../services/leaveService';
import Badge from '../../components/common/Badge';
import '../../styles/leaves.css';

export default function LeaveBalancesView() {
  const { employees } = useEmployees();
  const { getEmployeeBalances } = useLeaves();
  const { user, isAdmin, isEmployee } = useAuth();

  const [selectedYear, setSelectedYear] = useState(2026);
  const [searchTerm, setSearchTerm] = useState('');
  const [deptFilter, setDeptFilter] = useState('');

  const currentEmployee = useMemo(() => {
    if (!user) return null;
    return employees.find(
      (e) => (user.email && e.email?.toLowerCase() === user.email.toLowerCase()) || e.id === user.employeeId
    );
  }, [user, employees]);

  // Available year choices
  const availableYears = [2025, 2026, 2027];

  // Unique departments
  const departments = useMemo(() => {
    return Array.from(new Set(employees.map((e) => e.department).filter(Boolean)));
  }, [employees]);

  // Compute balances for all active & on-leave employees
  const employeeBalancesList = useMemo(() => {
    let targetEmployees = employees.filter((e) => e.employmentStatus !== 'Inactive');
    if (isEmployee && !isAdmin) {
      if (currentEmployee) {
        targetEmployees = targetEmployees.filter((e) => e.id === currentEmployee.id);
      } else {
        targetEmployees = [];
      }
    }

    return targetEmployees
      .map((emp) => {
        const balances = getEmployeeBalances(emp.id, selectedYear);
        return {
          employee: emp,
          balances,
        };
      })
      .filter(({ employee }) => {
        if (searchTerm.trim()) {
          const q = searchTerm.toLowerCase();
          const name = `${employee.firstName} ${employee.lastName}`.toLowerCase();
          const empId = (employee.employeeId || '').toLowerCase();
          if (!name.includes(q) && !empId.includes(q)) return false;
        }
        if (deptFilter && employee.department !== deptFilter) {
          return false;
        }
        return true;
      });
  }, [employees, isEmployee, isAdmin, currentEmployee, selectedYear, getEmployeeBalances, searchTerm, deptFilter]);

  return (
    <div className="leave-balances-view">
      {/* Demonstration Policy Banner */}
      <div className="modal-notice-box info" style={{ marginBottom: '1.5rem' }}>
        <Info size={18} style={{ minWidth: 18 }} />
        <div>
          <strong>Configurable Demonstration Leave Policy ({selectedYear})</strong>
          <p style={{ margin: '0.2rem 0 0 0', fontSize: '0.8rem' }}>
            Allowances are defined in calendar days per employee per year. Only <strong>Approved</strong> requests
            within {selectedYear} deduct from allowances. Pending and Rejected applications do not affect balances.
          </p>
        </div>
      </div>

      {/* 4 Policy Allowance Cards */}
      <div className="policy-cards-grid">
        {Object.entries(LEAVE_POLICIES).map(([typeKey, policy]) => (
          <div key={typeKey} className="policy-card">
            <div className="policy-card-header">
              <span className="policy-card-title">{policy.label}</span>
              <Badge variant={policy.badgeVariant}>{typeKey}</Badge>
            </div>
            <div className="policy-card-allowance">
              {policy.allowanceDays} <span style={{ fontSize: '0.85rem', fontWeight: 500, color: 'var(--color-text-secondary)' }}>days/yr</span>
            </div>
            <p className="policy-card-desc">{policy.description}</p>
          </div>
        ))}
      </div>

      {/* Filter / Year Control Row */}
      <div className="leaves-filter-bar" style={{ padding: '0.85rem 1.15rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          {/* Search */}
          <div className="leaves-search-wrap" style={{ minWidth: 220 }}>
            <Search size={16} className="leaves-search-icon" />
            <input
              type="text"
              className="leaves-search-input"
              placeholder="Filter by employee name or ID..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* Department Filter */}
          <div className="leaves-select-control">
            <label htmlFor="bal-dept">Dept:</label>
            <select
              id="bal-dept"
              className="leaves-select"
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
            >
              <option value="">All Departments</option>
              {departments.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </div>

          {/* Year Selector */}
          <div className="leaves-select-control" style={{ marginLeft: 'auto' }}>
            <label htmlFor="bal-year">
              <Calendar size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
              Calendar Year:
            </label>
            <select
              id="bal-year"
              className="leaves-select"
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
            >
              {availableYears.map((yr) => (
                <option key={yr} value={yr}>
                  {yr} Allowance
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Balances Table */}
      <div className="table-responsive" style={{ background: 'var(--color-bg-surface)', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-color)', boxShadow: 'var(--shadow-sm)' }}>
        <table className="data-table" aria-label="Employee Leave Balances">
          <thead>
            <tr>
              <th>Employee</th>
              <th>Department</th>
              <th>Annual Leave (20d)</th>
              <th>Sick Leave (12d)</th>
              <th>Personal Leave (5d)</th>
              <th>Unpaid Leave (30d)</th>
            </tr>
          </thead>
          <tbody>
            {employeeBalancesList.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', padding: '2.5rem' }}>
                  <Users size={32} style={{ color: 'var(--color-text-tertiary)', margin: '0 auto 0.5rem' }} />
                  <p style={{ color: 'var(--color-text-secondary)', fontWeight: 500 }}>
                    No matching employee balances found.
                  </p>
                </td>
              </tr>
            ) : (
              employeeBalancesList.map(({ employee, balances }) => {
                const initials = `${employee.firstName[0]}${employee.lastName[0]}`.toUpperCase();
                const annual = balances.Annual;
                const sick = balances.Sick;
                const personal = balances.Personal;
                const unpaid = balances.Unpaid;

                const annualPct = Math.min(100, Math.round((annual.usedDays / annual.allowance) * 100));

                return (
                  <tr key={employee.id} id={`bal-row-${employee.id}`}>
                    {/* Employee Profile */}
                    <td>
                      <div className="emp-name-cell">
                        <div className="emp-avatar">{initials}</div>
                        <div className="emp-name-text">
                          <span className="emp-full-name">
                            {employee.firstName} {employee.lastName}
                          </span>
                          <span className="emp-id-label">{employee.employeeId}</span>
                        </div>
                      </div>
                    </td>

                    {/* Department */}
                    <td>
                      <span className="emp-dept-tag">{employee.department}</span>
                    </td>

                    {/* Annual Leave */}
                    <td>
                      <div style={{ minWidth: 140 }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.785rem', fontWeight: 600 }}>
                          <span style={{ color: annual.remainingDays <= 5 ? '#d97706' : 'var(--color-text-primary)' }}>
                            {annual.remainingDays}d rem
                          </span>
                          <span style={{ color: 'var(--color-text-tertiary)', fontWeight: 400 }}>
                            {annual.usedDays}d used
                          </span>
                        </div>
                        <div className="balance-progress-wrap">
                          <div
                            className={`balance-progress-bar ${annualPct > 80 ? 'danger' : annualPct > 50 ? 'warning' : 'primary'}`}
                            style={{ width: `${annualPct}%` }}
                          />
                        </div>
                      </div>
                    </td>

                    {/* Sick Leave */}
                    <td>
                      <div style={{ fontSize: '0.825rem' }}>
                        <strong>{sick.remainingDays}d</strong>{' '}
                        <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.75rem' }}>
                          rem ({sick.usedDays}d used)
                        </span>
                      </div>
                    </td>

                    {/* Personal Leave */}
                    <td>
                      <div style={{ fontSize: '0.825rem' }}>
                        <strong>{personal.remainingDays}d</strong>{' '}
                        <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.75rem' }}>
                          rem ({personal.usedDays}d used)
                        </span>
                      </div>
                    </td>

                    {/* Unpaid Leave */}
                    <td>
                      <div style={{ fontSize: '0.825rem' }}>
                        <strong>{unpaid.usedDays}d</strong>{' '}
                        <span style={{ color: 'var(--color-text-secondary)', fontSize: '0.75rem' }}>
                          taken
                        </span>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
