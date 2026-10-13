import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Layers,
  Lock,
  Mail,
  Eye,
  EyeOff,
  ArrowRight,
  ShieldCheck,
  Users,
  Calendar,
  AlertCircle,
  Info
} from 'lucide-react';
import { useAuth } from '../../context/useAuth';
import { DEMO_IDENTITIES } from '../../services/authService';
import '../../styles/auth.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  // Destination to redirect back to after login
  const from = location.state?.from?.pathname || '/dashboard';

  // If already authenticated, redirect immediately
  React.useEffect(() => {
    if (isAuthenticated) {
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, from]);

  const validateForm = () => {
    const errors = {};
    const trimmedEmail = email.trim();

    if (!trimmedEmail) {
      errors.email = 'Email address is required.';
    } else if (!EMAIL_REGEX.test(trimmedEmail)) {
      errors.email = 'Please enter a valid email address.';
    }

    if (!password) {
      errors.password = 'Password is required.';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    setErrorMessage('');

    if (!validateForm()) {
      return;
    }

    setIsSubmitting(true);
    try {
      await login({ email, password });
      // Redirect to destination
      navigate(from, { replace: true });
    } catch (err) {
      setErrorMessage(
        err.message || 'Authentication failed. Please verify credentials.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleQuickFill = (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setFieldErrors({});
    setErrorMessage('');
  };

  return (
    <div className="auth-page-container">
      <div className="auth-card-wrapper">
        {/* Left: Showcase & Brand Panel */}
        <div className="auth-showcase-panel">
          <div>
            <div className="auth-showcase-brand">
              <div className="auth-showcase-logo-icon">
                <Layers size={24} strokeWidth={2.5} />
              </div>
              <div className="auth-showcase-brand-name">
                Workforce<span>Hub</span>
              </div>
            </div>

            <h1 className="auth-showcase-hero-title">
              Unified Workforce Intelligence & Human Operations
            </h1>
            <p className="auth-showcase-hero-desc">
              Centralized platform for employee lifecycle management, attendance
              tracking, leave orchestration, and organizational governance.
            </p>

            <ul className="auth-feature-list">
              <li className="auth-feature-item">
                <div className="auth-feature-icon-badge">
                  <Users size={18} />
                </div>
                <div className="auth-feature-text">
                  <h4>Directory & Hierarchy</h4>
                  <p>Comprehensive employee and department structure with headcounts</p>
                </div>
              </li>
              <li className="auth-feature-item">
                <div className="auth-feature-icon-badge">
                  <Calendar size={18} />
                </div>
                <div className="auth-feature-text">
                  <h4>Smart Attendance & Leaves</h4>
                  <p>Cross-validated absence handling with automated threshold rules</p>
                </div>
              </li>
              <li className="auth-feature-item">
                <div className="auth-feature-icon-badge">
                  <ShieldCheck size={18} />
                </div>
                <div className="auth-feature-text">
                  <h4>Role-Based Access Control</h4>
                  <p>Fine-grained navigation separation for Admins and Employees</p>
                </div>
              </li>
            </ul>
          </div>

          <div className="auth-showcase-footer">
            <span className="auth-phase-pill">
              <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#10b981' }} />
              Phase 8B Security
            </span>
            <span>Spring Boot & PostgreSQL Active</span>
          </div>
        </div>

        {/* Right: Login Form Panel */}
        <div className="auth-form-panel">
          <div className="auth-form-header">
            <h2>Welcome Back</h2>
            <p>Sign in to your organizational account or select a provisioned identity</p>
          </div>

          {/* Provisioned Database Accounts Quick Fill Selector */}
          <div className="demo-accounts-card" aria-label="Provisioned Database Accounts">
            <div className="demo-accounts-title">
              <span>Provisioned Accounts (PostgreSQL)</span>
              <span className="badge badge-neutral" style={{ fontSize: '0.68rem' }}>
                Instant Fill
              </span>
            </div>

            <div className="demo-chips-grid">
              {DEMO_IDENTITIES.map((demo) => {
                const isAdmin = demo.user.role === 'ADMIN';
                const primaryEmail = demo.credentials.emails[0];
                const primaryPassword = demo.credentials.passwords[0];

                return (
                  <button
                    key={demo.user.id}
                    type="button"
                    className="demo-chip-btn"
                    onClick={() => handleQuickFill(primaryEmail, primaryPassword)}
                    aria-label={`Fill credentials for ${demo.user.name} (${demo.user.role})`}
                  >
                    <div className="demo-chip-header">
                      <span className="demo-chip-name">{demo.user.name}</span>
                      <span className={`demo-chip-role ${isAdmin ? 'admin' : 'employee'}`}>
                        {demo.user.role}
                      </span>
                    </div>
                    <span className="demo-chip-cred">{primaryEmail}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Error Banner */}
          {errorMessage && (
            <div className="auth-alert-banner" role="alert" style={{ marginBottom: '1.25rem' }}>
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong>Authentication Failed</strong>
                <div>{errorMessage}</div>
              </div>
            </div>
          )}

          {/* Login Form */}
          <form className="auth-form" onSubmit={handleSubmit} noValidate>
            {/* Email Field */}
            <div className="auth-form-group">
              <label htmlFor="auth-email-input" className="auth-form-label">
                <span>Email Address</span>
                {fieldErrors.email && (
                  <span className="auth-error-msg">{fieldErrors.email}</span>
                )}
              </label>
              <div className="auth-input-wrapper">
                <Mail size={16} className="auth-input-icon" aria-hidden="true" />
                <input
                  id="auth-email-input"
                  type="email"
                  className={`auth-input ${fieldErrors.email ? 'has-error' : ''}`}
                  placeholder="name@workforcehub.io"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (fieldErrors.email) {
                      setFieldErrors((prev) => ({ ...prev, email: null }));
                    }
                  }}
                  autoComplete="username"
                  required
                  aria-invalid={!!fieldErrors.email}
                  aria-describedby={fieldErrors.email ? 'auth-email-error' : undefined}
                />
              </div>
            </div>

            {/* Password Field */}
            <div className="auth-form-group">
              <label htmlFor="auth-password-input" className="auth-form-label">
                <span>Password</span>
                {fieldErrors.password && (
                  <span className="auth-error-msg">{fieldErrors.password}</span>
                )}
              </label>
              <div className="auth-input-wrapper">
                <Lock size={16} className="auth-input-icon" aria-hidden="true" />
                <input
                  id="auth-password-input"
                  type={showPassword ? 'text' : 'password'}
                  className={`auth-input ${fieldErrors.password ? 'has-error' : ''}`}
                  placeholder="••••••••••••"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (fieldErrors.password) {
                      setFieldErrors((prev) => ({ ...prev, password: null }));
                    }
                  }}
                  autoComplete="current-password"
                  required
                  aria-invalid={!!fieldErrors.password}
                />
                <button
                  type="button"
                  className="auth-pw-toggle"
                  onClick={() => setShowPassword((prev) => !prev)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  tabIndex={0}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              id="auth-submit-btn"
              className="btn btn-primary auth-submit-btn"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span
                    style={{
                      width: '16px',
                      height: '16px',
                      border: '2px solid rgba(255,255,255,0.3)',
                      borderTopColor: '#ffffff',
                      borderRadius: '50%',
                      animation: 'spin 0.8s linear infinite',
                      display: 'inline-block',
                    }}
                  />
                  <span>Signing in...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight size={16} />
                </>
              )}
            </button>
          </form>

          {/* Real Backend Authentication Notice Footer */}
          <div className="auth-notice-footer">
            <Info size={15} style={{ flexShrink: 0, marginTop: '2px', color: 'var(--color-info)' }} />
            <span>
              <strong>Production Security Active:</strong> Authenticated against Spring Boot 3.4.3 backend with PostgreSQL, BCrypt password verification, and server-managed HttpOnly sessions.
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
