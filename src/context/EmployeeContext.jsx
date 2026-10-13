import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  fetchEmployeesApi,
  createEmployeeApi,
  updateEmployeeApi,
  deleteEmployeeApi,
  validateEmployeeData,
  DEPARTMENTS,
} from '../services/employeeService';
import { useAuth } from './useAuth';
import { EmployeeContext } from './employeeContextDef';

export function EmployeeProvider({ children }) {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const [employees, setEmployees] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);

  // Auto-dismiss toast after 4 seconds
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => {
      setToast(null);
    }, 4000);
    return () => clearTimeout(timer);
  }, [toast]);

  const showToast = useCallback((message, type = 'success') => {
    setToast({
      id: Date.now(),
      message,
      type,
    });
  }, []);

  const dismissToast = useCallback(() => {
    setToast(null);
  }, []);

  /**
   * Loads employees from Spring Boot REST API
   */
  const loadEmployees = useCallback(async () => {
    if (!isAuthenticated || (user && user.role !== 'ADMIN')) {
      setEmployees([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await fetchEmployeesApi({ all: true });
      setEmployees(Array.isArray(data) ? data : []);
      setError(null);
    } catch (err) {
      console.error('[EmployeeContext] Failed to load employees from backend:', err);
      setError(err.message || 'Failed to load employee records from backend. Please verify that the Spring Boot server is online.');
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user]);

  useEffect(() => {
    let ignore = false;

    async function syncEmployees() {
      if (isAuthLoading) return;

      if (!isAuthenticated || (user && user.role !== 'ADMIN')) {
        if (!ignore) {
          setEmployees([]);
          setIsLoading(false);
          setError(null);
        }
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const data = await fetchEmployeesApi({ all: true });
        if (!ignore) {
          setEmployees(Array.isArray(data) ? data : []);
          setError(null);
        }
      } catch (err) {
        if (!ignore) {
          console.error('[EmployeeContext] Failed to load employees from backend:', err);
          setError(err.message || 'Failed to load employee records from backend. Please verify that the Spring Boot server is online.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    syncEmployees();

    return () => {
      ignore = true;
    };
  }, [isAuthLoading, isAuthenticated, user]);

  /**
   * Add a new employee via Spring Boot REST API
   */
  const addEmployee = useCallback(async (formData) => {
    // 1. Client-side pre-validation
    const { isValid, errors } = validateEmployeeData(formData, employees, null);
    if (!isValid) {
      return { success: false, errors };
    }

    try {
      const created = await createEmployeeApi(formData);
      setEmployees((prev) => [created, ...prev]);
      showToast(
        `Employee ${created.firstName} ${created.lastName} (${created.employeeId}) created successfully!`,
        'success'
      );
      return { success: true, employee: created };
    } catch (err) {
      console.error('[EmployeeContext] Create employee failed:', err);
      const fieldErrors = err.validationErrors || {};
      if (err.message && Object.keys(fieldErrors).length === 0) {
        fieldErrors.general = err.message;
      }
      showToast(err.message || 'Failed to create employee.', 'error');
      return { success: false, errors: fieldErrors };
    }
  }, [employees, showToast]);

  /**
   * Update an existing employee via Spring Boot REST API
   */
  const updateEmployee = useCallback(async (id, formData) => {
    const existing = employees.find((e) => e.id === id);
    if (!existing) {
      return { success: false, errors: { general: 'Employee not found.' } };
    }

    // 1. Client-side pre-validation
    const { isValid, errors } = validateEmployeeData(formData, employees, id);
    if (!isValid) {
      return { success: false, errors };
    }

    try {
      const updated = await updateEmployeeApi(id, formData);
      setEmployees((prev) => prev.map((emp) => (emp.id === id ? updated : emp)));
      showToast(
        `Employee ${updated.firstName} ${updated.lastName} updated successfully!`,
        'success'
      );
      return { success: true, employee: updated };
    } catch (err) {
      console.error('[EmployeeContext] Update employee failed:', err);
      const fieldErrors = err.validationErrors || {};
      if (err.message && Object.keys(fieldErrors).length === 0) {
        fieldErrors.general = err.message;
      }
      showToast(err.message || 'Failed to update employee.', 'error');
      return { success: false, errors: fieldErrors };
    }
  }, [employees, showToast]);

  /**
   * Delete an existing employee via Spring Boot REST API
   */
  const deleteEmployee = useCallback(async (id) => {
    const target = employees.find((e) => e.id === id);
    if (!target) return false;

    try {
      await deleteEmployeeApi(id);
      setEmployees((prev) => prev.filter((e) => e.id !== id));
      showToast(
        `Employee ${target.firstName} ${target.lastName} (${target.employeeId}) has been removed.`,
        'info'
      );
      return true;
    } catch (err) {
      console.error('[EmployeeContext] Delete employee failed:', err);
      showToast(err.message || 'Failed to delete employee.', 'error');
      return false;
    }
  }, [employees, showToast]);

  /**
   * Update department name across affected employees when a department is renamed
   */
  const updateEmployeesDepartment = useCallback((oldDeptName, newDeptName) => {
    if (!oldDeptName || !newDeptName || oldDeptName === newDeptName) return;
    setEmployees((prev) =>
      prev.map((emp) => (emp.department === oldDeptName ? { ...emp, department: newDeptName } : emp))
    );
  }, []);

  /**
   * Set department to 'Unassigned' for all employees in a deleted department
   */
  const unassignEmployeesFromDepartment = useCallback((deptName) => {
    if (!deptName) return 0;
    let affectedCount = 0;
    setEmployees((prev) =>
      prev.map((emp) => {
        if (emp.department === deptName) {
          affectedCount += 1;
          return { ...emp, department: 'Unassigned' };
        }
        return emp;
      })
    );
    return affectedCount;
  }, []);

  // Derived metrics
  const totalCount = employees.length;
  const activeCount = useMemo(() => employees.filter((e) => e.employmentStatus === 'Active').length, [employees]);
  const onLeaveCount = useMemo(() => employees.filter((e) => e.employmentStatus === 'On Leave').length, [employees]);
  const inactiveCount = useMemo(() => employees.filter((e) => e.employmentStatus === 'Inactive').length, [employees]);

  // Derived department breakdown with percentages
  const departmentStats = useMemo(() => {
    const deptColorMap = {
      Engineering: '#2563eb',
      'Sales & Marketing': '#059669',
      'Operations & Logistics': '#d97706',
      'Product & UX': '#7c3aed',
      'People & HR': '#0284c7',
      'Finance & Legal': '#64748b',
    };

    return DEPARTMENTS.map((deptName) => {
      const count = employees.filter((e) => e.department === deptName).length;
      const percentage = totalCount > 0 ? Math.round((count / totalCount) * 100) : 0;
      return {
        name: deptName,
        count,
        percentage,
        color: deptColorMap[deptName] || '#64748b',
      };
    });
  }, [employees, totalCount]);

  const value = useMemo(() => ({
    employees,
    totalCount,
    activeCount,
    onLeaveCount,
    inactiveCount,
    departmentStats,
    isLoading,
    error,
    refreshEmployees: loadEmployees,
    addEmployee,
    updateEmployee,
    deleteEmployee,
    updateEmployeesDepartment,
    unassignEmployeesFromDepartment,
    toast,
    showToast,
    dismissToast,
  }), [
    employees,
    totalCount,
    activeCount,
    onLeaveCount,
    inactiveCount,
    departmentStats,
    isLoading,
    error,
    loadEmployees,
    addEmployee,
    updateEmployee,
    deleteEmployee,
    updateEmployeesDepartment,
    unassignEmployeesFromDepartment,
    toast,
    showToast,
    dismissToast,
  ]);

  return (
    <EmployeeContext.Provider value={value}>
      {children}
    </EmployeeContext.Provider>
  );
}
