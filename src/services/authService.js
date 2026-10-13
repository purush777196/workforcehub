/**
 * WorkforceHub Authentication Service
 * 
 * PHASE 8B: Production-Quality Authentication & Server-Side Authorization
 * 
 * Integrates with Spring Boot 3.4.3 backend and PostgreSQL:
 * - Real credential verification with BCrypt hashing on the server.
 * - Server-managed session stored in HttpOnly, SameSite cookie (JSESSIONID).
 * - SPA-compatible CSRF token handling for state-changing requests.
 * - Invalidation of legacy demonstration identities while preserving HR datasets.
 */

export const AUTH_STORAGE_KEY = 'workforcehub_auth_session';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Standard User Roles
 */
export const ROLES = {
  ADMIN: 'ADMIN',
  EMPLOYEE: 'EMPLOYEE',
};

/**
 * Bootstrap identities provisioned in PostgreSQL via Spring Boot AdminBootstrapService.
 * Used for instant quick-fill convenience in the UI.
 * Plaintext passwords are NEVER stored in localStorage or transmitted insecurely.
 */
export const DEMO_IDENTITIES = [
  {
    credentials: {
      emails: ['admin@workforcehub.io'],
      passwords: ['Admin@123456'],
    },
    user: {
      id: 'admin-bootstrap-01',
      name: 'Alex Morgan',
      email: 'admin@workforcehub.io',
      role: ROLES.ADMIN,
      department: 'Executive Management',
      avatarInitials: 'AM',
      title: 'System Administrator',
    },
  },
  {
    credentials: {
      emails: ['employee@workforcehub.io'],
      passwords: ['Employee@123456'],
    },
    user: {
      id: 'emp-bootstrap-02',
      name: 'Elena Rostova',
      email: 'employee@workforcehub.io',
      role: ROLES.EMPLOYEE,
      department: 'Engineering',
      employeeId: 'EMP-1001',
      avatarInitials: 'ER',
      title: 'Senior Software Engineer',
    },
  },
];

// In-memory cache for CSRF token
let cachedCsrfToken = null;

/**
 * Invalidate legacy Phase 7 demonstration sessions safely.
 * Stored HR datasets (employees, departments, attendance, leaves) are strictly preserved.
 */
export function sanitizeLegacyStorage() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && (parsed.isDemo || parsed.isDemo === true)) {
        console.info('[WorkforceHub Security] Invalidated obsolete Phase 7 demonstration session.');
        localStorage.removeItem(AUTH_STORAGE_KEY);
      }
    }
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}

/**
 * Fetches CSRF token metadata from the Spring Boot backend.
 * 
 * @returns {Promise<{token: string, headerName: string}|null>}
 */
export async function getCsrfToken() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
      method: 'GET',
      credentials: 'include',
    });

    if (response.ok) {
      const envelope = await response.json();
      if (envelope?.success && envelope?.data?.token) {
        cachedCsrfToken = {
          token: envelope.data.token,
          headerName: envelope.data.headerName || 'X-XSRF-TOKEN',
        };
        return cachedCsrfToken;
      }
    }
  } catch (err) {
    console.warn('[WorkforceHub Security] Unable to retrieve CSRF token from server:', err);
  }
  return null;
}

/**
 * Generates appropriate CSRF headers for state-changing requests.
 */
export async function buildCsrfHeaders() {
  const headers = {};
  let csrf = cachedCsrfToken;
  if (!csrf) {
    csrf = await getCsrfToken();
  }
  if (csrf && csrf.token) {
    headers[csrf.headerName] = csrf.token;
    headers['X-XSRF-TOKEN'] = csrf.token;
  }
  return headers;
}

/**
 * Authenticates user credentials against the Spring Boot PostgreSQL backend.
 * POST /api/auth/login
 * 
 * @param {Object} credentials - { email, password }
 * @returns {Promise<Object>} Sanitized user profile without secrets
 */
export async function login({ email, password }) {
  const normalizedEmail = (email || '').trim().toLowerCase();
  const trimmedPassword = (password || '').trim();

  if (!normalizedEmail || !trimmedPassword) {
    throw new Error('Email address and password are required.');
  }

  // Pre-fetch CSRF token to establish session cookie & CSRF pair
  await getCsrfToken();

  let response;
  try {
    response = await fetch(`${API_BASE_URL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        email: normalizedEmail,
        password: trimmedPassword,
      }),
    });
  } catch {
    throw new Error(
      `Unable to connect to WorkforceHub backend at ${API_BASE_URL}. Ensure the Spring Boot server is running.`
    );
  }

  let data;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const errorMsg = data?.message || data?.error || 'Authentication failed. Please verify credentials.';
    throw new Error(errorMsg);
  }

  if (!data || !data.success || !data.data) {
    throw new Error('Invalid response structure received from authentication server.');
  }

  const authenticatedUser = data.data;

  // Cache safe non-sensitive profile (never passwords, tokens, or hashes)
  try {
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        user: authenticatedUser,
        isRealBackend: true,
        authenticatedAt: new Date().toISOString(),
      })
    );
  } catch (err) {
    console.error('Failed to cache safe profile in localStorage', err);
  }

  return authenticatedUser;
}

/**
 * Invalidates the server-side session and clears the HttpOnly session cookie.
 * POST /api/auth/logout
 */
export async function logout() {
  const csrfHeaders = await buildCsrfHeaders();

  try {
    await fetch(`${API_BASE_URL}/api/auth/logout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...csrfHeaders,
      },
      credentials: 'include',
    });
  } catch (err) {
    console.warn('[WorkforceHub Security] Logout request completed with network warning:', err);
  } finally {
    cachedCsrfToken = null;
    try {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    } catch {
      // Ignore fallback
    }
  }
}

/**
 * Retrieves the currently authenticated user from the backend session.
 * GET /api/auth/me
 * 
 * @returns {Promise<Object|null>} Safe user profile or null if unauthenticated
 */
export async function getCurrentUser() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
      method: 'GET',
      credentials: 'include',
    });

    if (response.status === 401) {
      // Clean unauthenticated state
      try {
        localStorage.removeItem(AUTH_STORAGE_KEY);
      } catch {
        // Ignore fallback
      }
      return null;
    }

    if (!response.ok) {
      return null;
    }

    const envelope = await response.json();
    if (envelope?.success && envelope?.data) {
      return envelope.data;
    }
    return null;
  } catch (err) {
    // Network or server unreachable
    console.warn('[WorkforceHub Security] Failed to reach backend for session verification:', err);
    throw err;
  }
}

/**
 * Restores session on application bootstrap.
 * Validates session against the real Spring Boot backend.
 * 
 * @returns {Promise<Object|null>} Active user profile or null
 */
export async function restoreSession() {
  sanitizeLegacyStorage();
  return await getCurrentUser();
}
