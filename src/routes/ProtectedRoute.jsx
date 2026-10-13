import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

/**
 * Route wrapper that ensures the user is authenticated.
 * Unauthenticated users are redirected to /login with requested location preserved.
 */
export default function ProtectedRoute() {
  const { isAuthenticated, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          backgroundColor: 'var(--color-bg-app)',
          color: 'var(--color-text-secondary)',
          fontFamily: 'var(--font-sans)',
        }}
      >
        <div
          style={{
            width: '36px',
            height: '36px',
            border: '3px solid var(--color-bg-muted)',
            borderTopColor: 'var(--color-primary)',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
            marginBottom: '1rem',
          }}
        />
        <p style={{ fontSize: '0.875rem', fontWeight: 500 }}>
          Verifying session credentials...
        </p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
