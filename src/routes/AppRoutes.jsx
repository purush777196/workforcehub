import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import AppLayout from '../components/layout/AppLayout';
import ProtectedRoute from './ProtectedRoute';
import RoleProtectedRoute from './RoleProtectedRoute';
import LoginPage from '../pages/Auth/LoginPage';
import AccessDeniedPage from '../pages/Auth/AccessDeniedPage';
import NotFoundPage from '../pages/NotFoundPage';
import Dashboard from '../pages/Dashboard/Dashboard';
import EmployeesPage from '../pages/Employees/EmployeesPage';
import DepartmentsPage from '../pages/Departments/DepartmentsPage';
import AttendancePage from '../pages/Attendance/AttendancePage';
import LeavesPage from '../pages/Leaves/LeavesPage';
import SettingsPage from '../pages/Settings/SettingsPage';
import DataImportPage from '../pages/Admin/DataImportPage';

/**
 * Main application routing configuration
 * Integrates authentication guards, role restrictions, and fallback handling.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* Public Route */}
      <Route path="/login" element={<LoginPage />} />

      {/* Authenticated Application Shell */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          {/* Redirect root / to /dashboard */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          {/* Routes accessible to both ADMIN and EMPLOYEE */}
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/attendance" element={<AttendancePage />} />
          <Route path="/leaves" element={<LeavesPage />} />
          <Route path="/access-denied" element={<AccessDeniedPage />} />

          {/* Administrative Routes restricted to ADMIN role */}
          <Route element={<RoleProtectedRoute allowedRoles={['ADMIN']} />}>
            <Route path="/employees" element={<EmployeesPage />} />
            <Route path="/departments" element={<DepartmentsPage />} />
            <Route path="/admin/data-import" element={<DataImportPage />} />
            <Route path="/settings" element={<SettingsPage />} />
          </Route>

          {/* Catch-all 404 for unknown paths */}
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
