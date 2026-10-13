import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  fetchDepartmentsApi,
  createDepartmentApi,
  updateDepartmentApi,
  deleteDepartmentApi,
  validateDepartmentData,
} from '../services/departmentService';
import { DepartmentContext } from './departmentContextDef';
import { useEmployees } from './useEmployees';
import { useAuth } from './useAuth';

export function DepartmentProvider({ children }) {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const [departments, setDepartments] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const {
    employees,
    showToast,
    updateEmployeesDepartment,
    unassignEmployeesFromDepartment,
  } = useEmployees();

  /**
   * Helper: calculate current dynamic headcount for a department
   */
  const getDepartmentEmployeeCount = useCallback(
    (deptName) => {
      if (!deptName) return 0;
      if (employees && employees.length > 0) {
        return employees.filter((e) => e.department === deptName).length;
      }
      const found = departments.find((d) => d.name === deptName);
      return found?.headcount ?? found?.employeeCount ?? 0;
    },
    [employees, departments]
  );

  /**
   * Helper: retrieve list of employees assigned to a department
   */
  const getDepartmentEmployees = useCallback(
    (deptName) => {
      if (!deptName) return [];
      return employees.filter((e) => e.department === deptName);
    },
    [employees]
  );

  /**
   * Helper: resolve manager employee object from employee ID
   */
  const getManagerEmployee = useCallback(
    (managerEmployeeId) => {
      if (!managerEmployeeId) return null;
      return (
        employees.find(
          (e) => e.id === managerEmployeeId || e.employeeId === managerEmployeeId
        ) || null
      );
    },
    [employees]
  );

  /**
   * Loads departments from Spring Boot REST API
   */
  const loadDepartments = useCallback(async () => {
    if (!isAuthenticated || (user && user.role !== 'ADMIN')) {
      setDepartments([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await fetchDepartmentsApi({ all: true });
      setDepartments(Array.isArray(data) ? data : []);
      setError(null);
    } catch (err) {
      console.error('[DepartmentContext] Failed to load departments from backend:', err);
      setError(
        err.message ||
          'Failed to load department records from backend. Please verify that the Spring Boot server is online.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated, user]);

  useEffect(() => {
    let ignore = false;

    async function syncDepartments() {
      if (isAuthLoading) return;

      if (!isAuthenticated || (user && user.role !== 'ADMIN')) {
        if (!ignore) {
          setDepartments([]);
          setIsLoading(false);
          setError(null);
        }
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const data = await fetchDepartmentsApi({ all: true });
        if (!ignore) {
          setDepartments(Array.isArray(data) ? data : []);
          setError(null);
        }
      } catch (err) {
        if (!ignore) {
          console.error('[DepartmentContext] Failed to load departments from backend:', err);
          setError(
            err.message ||
              'Failed to load department records from backend. Please verify that the Spring Boot server is online.'
          );
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    syncDepartments();

    return () => {
      ignore = true;
    };
  }, [isAuthLoading, isAuthenticated, user]);

  /**
   * Add a new department via Spring Boot REST API
   */
  const addDepartment = useCallback(
    async (formData) => {
      // 1. Client-side pre-validation
      const { isValid, errors } = validateDepartmentData(formData, departments, null);
      if (!isValid) {
        return { success: false, errors };
      }

      try {
        const created = await createDepartmentApi(formData);
        setDepartments((prev) => [...prev, created]);
        showToast(
          `Department "${created.name}" (${created.departmentId || created.departmentCode}) created successfully!`,
          'success'
        );
        return { success: true, department: created };
      } catch (err) {
        console.error('[DepartmentContext] Create department failed:', err);
        const fieldErrors = err.validationErrors || {};
        if (err.message && Object.keys(fieldErrors).length === 0) {
          fieldErrors.general = err.message;
        }
        showToast(err.message || 'Failed to create department.', 'error');
        return { success: false, errors: fieldErrors };
      }
    },
    [departments, showToast]
  );

  /**
   * Update an existing department via Spring Boot REST API
   */
  const updateDepartment = useCallback(
    async (id, formData) => {
      const existing = departments.find((d) => d.id === id);
      if (!existing) {
        return { success: false, errors: { general: 'Department not found.' } };
      }

      const { isValid, errors } = validateDepartmentData(formData, departments, id);
      if (!isValid) {
        return { success: false, errors };
      }

      try {
        const updated = await updateDepartmentApi(id, formData);
        setDepartments((prev) => prev.map((d) => (d.id === id ? updated : d)));

        // If department name was changed, sync with employee records
        if (existing.name !== updated.name && updateEmployeesDepartment) {
          updateEmployeesDepartment(existing.name, updated.name);
        }

        showToast(`Department "${updated.name}" updated successfully!`, 'success');
        return { success: true, department: updated };
      } catch (err) {
        console.error('[DepartmentContext] Update department failed:', err);
        const fieldErrors = err.validationErrors || {};
        if (err.message && Object.keys(fieldErrors).length === 0) {
          fieldErrors.general = err.message;
        }
        showToast(err.message || 'Failed to update department.', 'error');
        return { success: false, errors: fieldErrors };
      }
    },
    [departments, showToast, updateEmployeesDepartment]
  );

  /**
   * Delete a department safely via Spring Boot REST API.
   * Enforces business rule: departments with assigned employees cannot be deleted.
   */
  const deleteDepartment = useCallback(
    async (id) => {
      const target = departments.find((d) => d.id === id);
      if (!target) return { success: false, count: 0 };

      // Pre-check if department currently has assigned personnel in frontend state
      const assignedCount = employees.filter((e) => e.department === target.name).length;
      if (assignedCount > 0) {
        const errorMsg = `Cannot delete department "${target.name}": ${assignedCount} employee(s) are still assigned. Please reassign them first.`;
        showToast(errorMsg, 'error');
        return { success: false, error: errorMsg };
      }

      try {
        await deleteDepartmentApi(id);
        setDepartments((prev) => prev.filter((d) => d.id !== id));
        showToast(`Department "${target.name}" deleted successfully.`, 'info');
        return { success: true, count: 0 };
      } catch (err) {
        console.error('[DepartmentContext] Delete department failed:', err);
        showToast(err.message || 'Failed to delete department.', 'error');
        return { success: false, error: err.message };
      }
    },
    [departments, employees, showToast]
  );

  // Derived summaries
  const totalDepartmentCount = departments.length;
  const activeDepartmentCount = useMemo(
    () => departments.filter((d) => d.status === 'Active').length,
    [departments]
  );

  const activeDepartmentNames = useMemo(
    () => departments.filter((d) => d.status === 'Active').map((d) => d.name),
    [departments]
  );

  const departmentsWithManagersCount = useMemo(
    () =>
      departments.filter(
        (d) =>
          Boolean(d.managerEmployeeId || d.managerId) &&
          employees.some((e) => e.id === (d.managerEmployeeId || d.managerId))
      ).length,
    [departments, employees]
  );

  const totalEmployeesInDepartments = useMemo(
    () => employees.filter((e) => activeDepartmentNames.includes(e.department)).length,
    [employees, activeDepartmentNames]
  );

  const unassignedEmployeesCount = useMemo(
    () => employees.filter((e) => !activeDepartmentNames.includes(e.department)).length,
    [employees, activeDepartmentNames]
  );

  const value = useMemo(
    () => ({
      departments,
      totalDepartmentCount,
      activeDepartmentCount,
      departmentsWithManagersCount,
      totalEmployeesInDepartments,
      unassignedEmployeesCount,
      isLoading,
      error,
      refreshDepartments: loadDepartments,
      getDepartmentEmployeeCount,
      getDepartmentEmployees,
      getManagerEmployee,
      addDepartment,
      updateDepartment,
      deleteDepartment,
      unassignEmployeesFromDepartment,
    }),
    [
      departments,
      totalDepartmentCount,
      activeDepartmentCount,
      departmentsWithManagersCount,
      totalEmployeesInDepartments,
      unassignedEmployeesCount,
      isLoading,
      error,
      loadDepartments,
      getDepartmentEmployeeCount,
      getDepartmentEmployees,
      getManagerEmployee,
      addDepartment,
      updateDepartment,
      deleteDepartment,
      unassignEmployeesFromDepartment,
    ]
  );

  return (
    <DepartmentContext.Provider value={value}>
      {children}
    </DepartmentContext.Provider>
  );
}
