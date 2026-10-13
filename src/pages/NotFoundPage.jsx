import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion, Home } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '70vh',
        padding: '2rem 1rem',
        fontFamily: 'var(--font-sans)',
      }}
    >
      <div
        className="card"
        style={{
          maxWidth: '500px',
          width: '100%',
          textAlign: 'center',
          padding: '2.5rem 2rem',
          borderRadius: 'var(--radius-xl)',
          boxShadow: 'var(--shadow-lg)',
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
            backgroundColor: 'var(--color-primary-light)',
            color: 'var(--color-primary)',
            marginBottom: '1.25rem',
            border: '1px solid var(--color-primary-border)',
          }}
        >
          <FileQuestion size={34} strokeWidth={2.2} />
        </div>

        <div style={{ marginBottom: '0.5rem' }}>
          <span
            className="badge badge-neutral"
            style={{ fontSize: '0.75rem', letterSpacing: '0.05em', textTransform: 'uppercase' }}
          >
            HTTP 404 Not Found
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
          Page Not Found
        </h1>

        <p
          style={{
            fontSize: '0.925rem',
            color: 'var(--color-text-secondary)',
            lineHeight: 1.6,
            marginBottom: '1.5rem',
          }}
        >
          The page or route you are attempting to reach does not exist in WorkforceHub or may have moved.
        </p>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'center' }}>
          <Link
            to="/dashboard"
            className="btn btn-primary"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Home size={16} />
            <span>Go to Dashboard</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
