import React, { useState, useEffect, useCallback } from 'react';
import { AuthContext } from './authContextDef';
import * as authService from '../services/authService';

/**
 * WorkforceHub AuthProvider
 * 
 * Provides session lifecycle, login, logout, and role inspection hooks.
 * Session restoration initializes asynchronously by verifying the active session
 * with the Spring Boot PostgreSQL backend (/api/auth/me).
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isBackendConnected, setIsBackendConnected] = useState(true);

  // Restore session from Spring Boot backend on initial mount
  useEffect(() => {
    let isMounted = true;

    async function initializeSession() {
      setIsLoading(true);
      setError(null);
      try {
        const authenticatedUser = await authService.restoreSession();
        if (isMounted) {
          setUser(authenticatedUser);
          setIsBackendConnected(true);
        }
      } catch (err) {
        if (isMounted) {
          console.warn('[AuthContext] Backend unavailable on initial session restoration:', err);
          setUser(null);
          setIsBackendConnected(false);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    initializeSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (credentials) => {
    setIsLoading(true);
    setError(null);
    try {
      const authenticatedUser = await authService.login(credentials);
      setUser(authenticatedUser);
      setIsBackendConnected(true);
      return authenticatedUser;
    } catch (err) {
      const errorMsg = err.message || 'Authentication failed. Please verify credentials.';
      setError(errorMsg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await authService.logout();
      setUser(null);
      setError(null);
    } catch (err) {
      console.error('Error during logout:', err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const value = {
    user,
    isAuthenticated: !!user,
    isAdmin: user?.role === authService.ROLES.ADMIN,
    isEmployee: user?.role === authService.ROLES.EMPLOYEE,
    role: user?.role || null,
    isLoading,
    error,
    isBackendConnected,
    login,
    logout,
    clearError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
