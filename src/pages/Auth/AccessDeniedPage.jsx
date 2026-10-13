import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShieldAlert, ArrowLeft, LogOut, Info } from 'lucide-react';
import { useAuth } from '../../context/useAuth';

export default function AccessDeniedPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleSignOut = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '75vh',
        padding: '2rem 1rem',
        fontFamily: 'var(--font-sans)',
      }}
    >
      <div
        className="card"
        style={{
          maxWidth: '540px',
          width: '100%',
          textAlign: 'center',
          padding: '2.5rem 2rem',
          borderRadius: 'var(--radius-xl)',
          boxShadow: 'var(--shadow-xl)',
          border: '1px solid var(--border-color)',
        }}
      >
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            backgroundColor: 'var(--color-danger-bg)',
            color: 'var(--color-danger)',
            marginBottom: '1.25rem',
            border: '1px solid var(--color-danger-border)',
          }}
        >
          <ShieldAlert size={34} strokeWidth={2.2} />
        </div>

        <div style={{ marginBottom: '0.5rem' }}>
          <span
            className="badge badge-danger"
            style={{ fontSize: '0.75rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}
          >
            HTTP 403 Forbidden
          </span>
        </div>

        <h1
          style={{
            fontSize: '1.65rem',
            fontWeight: 700,
            color: 'var(--color-text-primary)',
            marginBottom: '0.75rem',
          }}
        >
          Access Denied
        </h1>

        <p
          style={{
            fontSize: '0.925rem',
            color: 'var(--color-text-secondary)',
            lineHeight: 1.6,
            marginBottom: '1.5rem',
          }}
        >
          You do not have administrative privileges to access this page. This module is restricted to system administrators.
        </p>

        {user && (
          <div
            style={{
              backgroundColor: 'var(--color-bg-subtle)',
              borderRadius: 'var(--radius-md)',
              padding: '0.875rem 1rem',
              marginBottom: '1.5rem',
              border: '1px solid var(--border-color)',
              fontSize: '0.85rem',
              textAlign: 'left',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Authenticated As:</span>
              <strong style={{ color: 'var(--color-text-primary)' }}>{user.name}</strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.35rem' }}>
              <span style={{ color: 'var(--color-text-muted)' }}>Assigned Role:</span>
              <span className={`badge ${user.role === 'ADMIN' ? 'badge-primary' : 'badge-neutral'}`}>
                {user.role}
              </span>
            </div>
            {user.department && (
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Department:</span>
                <span style={{ color: 'var(--color-text-secondary)' }}>{user.department}</span>
              </div>
            )}
          </div>
        )}

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center', flexWrap: 'wrap' }}>
          <Link to="/dashboard" className="btn btn-primary" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}>
            <ArrowLeft size={16} />
            <span>Return to Dashboard</span>
          </Link>
          <button
            type="button"
            className="btn btn-outline"
            onClick={handleSignOut}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <LogOut size={16} />
            <span>Switch Account</span>
          </button>
        </div>

        {/* Architectural notice */}
        <div
          style={{
            marginTop: '2rem',
            paddingTop: '1rem',
            borderTop: '1px solid var(--border-color-light)',
            fontSize: '0.75rem',
            color: 'var(--color-text-muted)',
            lineHeight: 1.5,
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.5rem',
            textAlign: 'left',
          }}
        >
          <Info size={16} style={{ flexShrink: 0, marginTop: '2px', color: 'var(--color-info)' }} />
          <span>
            <strong>Architectural Notice:</strong> This route protection is enforced at the frontend layer for user experience. In Phase 8, Spring Boot with Spring Security role-based method security will enforce backend authorization on all protected endpoints.
          </span>
        </div>
      </div>
    </div>
  );
}
