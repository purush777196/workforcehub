import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import AccessDeniedPage from '../pages/Auth/AccessDeniedPage';

/**
 * Route guard that checks if the authenticated user possesses the required role.
 * If user lacks the required role, displays AccessDeniedPage instead of granting access.
 * 
 * NOTE: Frontend route guards are user-experience controls, not server-side security boundaries.
 * Genuine security enforcement will be implemented in Phase 8 via Spring Boot.
 */
export default function RoleProtectedRoute({ allowedRoles = [] }) {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles.length > 0 && !allowedRoles.includes(user?.role)) {
    return <AccessDeniedPage />;
  }

  return <Outlet />;
}
