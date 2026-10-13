import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  fetchLeavesApi,
  applyLeaveApi,
  updateLeaveApi,
  approveLeaveApi,
  rejectLeaveApi,
  cancelLeaveApi,
  fetchLeaveSummaryApi,
  fetchMyLeaveBalancesApi,
  fetchEmployeeLeaveBalancesApi,
  getEmployeeLeaveBalances,
  isEmployeeOnApprovedLeave,
  getApprovedLeaveForEmployee,
} from '../services/leaveService';
import { LeaveContext } from './leaveContextDef';
import { useEmployees } from './useEmployees';
import { useAuth } from './useAuth';
import { getLocalTodayDateString } from '../services/attendanceService';

export function LeaveProvider({ children }) {
  const [leaves, setLeaves] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isMutating, setIsMutating] = useState(false);
  const [error, setError] = useState(null);
  const [backendSummary, setBackendSummary] = useState(null);

  const { showToast } = useEmployees();
  const { isAuthenticated, isAdmin, isLoading: isAuthLoading } = useAuth();

  /**
   * Refreshes leave summary counters from backend
   */
  const refreshSummary = useCallback(async () => {
    try {
      const summary = await fetchLeaveSummaryApi();
      setBackendSummary(summary);
      return summary;
    } catch (err) {
      console.warn('[LeaveContext] Could not refresh leave summary:', err);
      return null;
    }
  }, []);

  /**
   * Loads all leave requests from Spring Boot backend REST API
   */
  const loadLeaves = useCallback(async () => {
    if (!isAuthenticated) {
      setLeaves([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await fetchLeavesApi({ all: true });
      const records = Array.isArray(data) ? data : (data?.content || []);
      setLeaves(records);
      setError(null);

      // Attempt to load summary
      try {
        const sum = await fetchLeaveSummaryApi();
        setBackendSummary(sum);
      } catch (sumErr) {
        console.warn('[LeaveContext] Summary fetch warning:', sumErr);
      }
    } catch (err) {
      console.error('[LeaveContext] Failed to load leaves from backend:', err);
      setError(err.message || 'Unable to connect to leave management service.');
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    let ignore = false;

    async function syncLeaves() {
      if (isAuthLoading) return;

      if (!isAuthenticated) {
        if (!ignore) {
          setLeaves([]);
          setIsLoading(false);
          setError(null);
        }
        return;
      }

      if (!ignore) {
        setIsLoading(true);
        setError(null);
      }

      try {
        const data = await fetchLeavesApi({ all: true });
        if (!ignore) {
          const records = Array.isArray(data) ? data : (data?.content || []);
          setLeaves(records);
          setError(null);
        }

        try {
          const sum = await fetchLeaveSummaryApi();
          if (!ignore) {
            setBackendSummary(sum);
          }
        } catch {
          // ignore summary fallback
        }
      } catch (err) {
        if (!ignore) {
          console.error('[LeaveContext] Initial sync error:', err);
          setError(err.message || 'Unable to retrieve leaves from server.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    syncLeaves();

    return () => {
      ignore = true;
    };
  }, [isAuthenticated, isAuthLoading]);

  /**
   * Calculate leave balances for a specific employee and year
   */
  const getEmployeeBalances = useCallback(
    (employeeId, year = new Date().getFullYear()) => {
      return getEmployeeLeaveBalances(employeeId, year, leaves);
    },
    [leaves]
  );

  /**
   * Fetches official balance from backend REST API
   */
  const fetchRemoteBalances = useCallback(
    async (employeeId, year) => {
      try {
        if (isAdmin && employeeId) {
          return await fetchEmployeeLeaveBalancesApi(employeeId, year);
        } else {
          return await fetchMyLeaveBalancesApi(year);
        }
      } catch (err) {
        console.warn('[LeaveContext] Could not fetch remote balances:', err);
        return null;
      }
    },
    [isAdmin]
  );

  /**
   * Helper: Check if an employee is on approved leave on a given calendar date (YYYY-MM-DD)
   */
  const checkIsEmployeeOnApprovedLeave = useCallback(
    (employeeId, dateStr) => {
      return isEmployeeOnApprovedLeave(employeeId, dateStr, leaves);
    },
    [leaves]
  );

  /**
   * Helper: Retrieve the specific approved leave request for an employee on a date
   */
  const getApprovedLeaveOnDate = useCallback(
    (employeeId, dateStr) => {
      return getApprovedLeaveForEmployee(employeeId, dateStr, leaves);
    },
    [leaves]
  );

  /**
   * Calculate total employees currently on approved leave for any given date
   */
  const getCurrentlyOnLeaveCount = useCallback(
    (dateStr) => {
      if (!dateStr) return 0;
      const onLeaveEmpIds = new Set();
      leaves.forEach((req) => {
        if (req.status === 'Approved' && dateStr >= req.startDate && dateStr <= req.endDate) {
          if (req.employeeId) onLeaveEmpIds.add(req.employeeId);
        }
      });
      return onLeaveEmpIds.size;
    },
    [leaves]
  );

  /**
   * Today's on-leave count
   */
  const todayDate = getLocalTodayDateString();
  const todayOnLeaveCount = useMemo(
    () => getCurrentlyOnLeaveCount(todayDate),
    [getCurrentlyOnLeaveCount, todayDate]
  );

  /**
   * Apply for Leave (New Request)
   */
  const applyLeave = useCallback(
    async ({ employeeId, leaveType, startDate, endDate, reason }) => {
      if (!leaveType || !startDate || !endDate || !reason?.trim()) {
        showToast('Please complete all mandatory fields.', 'error');
        return { success: false, error: 'All fields are required.' };
      }

      if (endDate < startDate) {
        showToast('End date cannot precede start date.', 'error');
        return { success: false, error: 'End date cannot precede start date.' };
      }

      setIsMutating(true);
      try {
        const created = await applyLeaveApi({
          employeeId,
          leaveType,
          startDate,
          endDate,
          reason,
        });

        setLeaves((prev) => [created, ...prev.filter((r) => r.id !== created.id)]);
        showToast(`Leave request submitted successfully (${created.requestedDays} days)!`, 'success');
        loadLeaves();
        return { success: true, request: created };
      } catch (err) {
        console.error('[LeaveContext] applyLeave failed:', err);
        const msg = err.message || 'Failed to submit leave request.';
        showToast(msg, 'error');
        return { success: false, error: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [showToast, loadLeaves]
  );

  /**
   * Edit eligible Pending leave request
   */
  const updateLeave = useCallback(
    async (id, { leaveType, startDate, endDate, reason }) => {
      if (!leaveType || !startDate || !endDate || !reason?.trim()) {
        showToast('Please complete all mandatory fields.', 'error');
        return { success: false, error: 'All fields are required.' };
      }

      if (endDate < startDate) {
        showToast('End date cannot precede start date.', 'error');
        return { success: false, error: 'End date cannot precede start date.' };
      }

      setIsMutating(true);
      try {
        const updated = await updateLeaveApi(id, {
          leaveType,
          startDate,
          endDate,
          reason,
        });

        setLeaves((prev) => prev.map((r) => (r.id === id ? updated : r)));
        showToast('Leave request updated successfully.', 'success');
        loadLeaves();
        return { success: true, request: updated };
      } catch (err) {
        console.error('[LeaveContext] updateLeave failed:', err);
        const msg = err.message || 'Failed to update leave request.';
        showToast(msg, 'error');
        return { success: false, error: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [showToast, loadLeaves]
  );

  /**
   * Approve a Pending leave request (ADMIN only)
   */
  const approveLeave = useCallback(
    async (id, { reviewerComment = '' } = {}) => {
      setIsMutating(true);
      try {
        const approved = await approveLeaveApi(id, { reviewerComment });
        setLeaves((prev) => prev.map((r) => (r.id === id ? approved : r)));
        showToast(`Approved ${approved.leaveType} leave for ${approved.employeeName || 'employee'}!`, 'success');
        loadLeaves();
        return { success: true, request: approved };
      } catch (err) {
        console.error('[LeaveContext] approveLeave failed:', err);
        const msg = err.message || 'Failed to approve leave request.';
        showToast(msg, 'error');
        return { success: false, error: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [showToast, loadLeaves]
  );

  /**
   * Reject a Pending leave request (ADMIN only)
   */
  const rejectLeave = useCallback(
    async (id, { reviewerComment } = {}) => {
      if (!reviewerComment?.trim()) {
        showToast('A rejection reason / comment is required.', 'error');
        return { success: false, error: 'Rejection comment is required.' };
      }

      setIsMutating(true);
      try {
        const rejected = await rejectLeaveApi(id, { reviewerComment });
        setLeaves((prev) => prev.map((r) => (r.id === id ? rejected : r)));
        showToast(`Rejected leave request for ${rejected.employeeName || 'employee'}.`, 'info');
        loadLeaves();
        return { success: true, request: rejected };
      } catch (err) {
        console.error('[LeaveContext] rejectLeave failed:', err);
        const msg = err.message || 'Failed to reject leave request.';
        showToast(msg, 'error');
        return { success: false, error: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [showToast, loadLeaves]
  );

  /**
   * Cancel / Delete a Pending leave request
   */
  const cancelLeave = useCallback(
    async (id) => {
      setIsMutating(true);
      try {
        await cancelLeaveApi(id);
        setLeaves((prev) => prev.filter((r) => r.id !== id));
        showToast('Pending leave request cancelled and removed from queue.', 'info');
        loadLeaves();
        return { success: true };
      } catch (err) {
        console.error('[LeaveContext] cancelLeave failed:', err);
        const msg = err.message || 'Failed to cancel leave request.';
        showToast(msg, 'error');
        return { success: false, error: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [showToast, loadLeaves]
  );

  // Derived counts
  const totalRequestsCount = backendSummary ? backendSummary.totalRequests : leaves.length;
  const pendingRequests = useMemo(() => leaves.filter((r) => r.status === 'Pending'), [leaves]);
  const pendingCount = backendSummary ? backendSummary.pendingCount : pendingRequests.length;
  const approvedCount = backendSummary
    ? backendSummary.approvedCount
    : leaves.filter((r) => r.status === 'Approved').length;
  const rejectedCount = backendSummary
    ? backendSummary.rejectedCount
    : leaves.filter((r) => r.status === 'Rejected').length;

  const value = useMemo(
    () => ({
      leaves,
      isLoading,
      isMutating,
      error,
      refreshLeaves: loadLeaves,
      refreshSummary,
      backendSummary,
      totalRequestsCount,
      pendingRequests,
      pendingCount,
      approvedCount,
      rejectedCount,
      todayOnLeaveCount,
      getCurrentlyOnLeaveCount,
      getEmployeeBalances,
      fetchRemoteBalances,
      isEmployeeOnApprovedLeave: checkIsEmployeeOnApprovedLeave,
      getApprovedLeaveOnDate,
      applyLeave,
      updateLeave,
      approveLeave,
      rejectLeave,
      cancelLeave,
    }),
    [
      leaves,
      isLoading,
      isMutating,
      error,
      loadLeaves,
      refreshSummary,
      backendSummary,
      totalRequestsCount,
      pendingRequests,
      pendingCount,
      approvedCount,
      rejectedCount,
      todayOnLeaveCount,
      getCurrentlyOnLeaveCount,
      getEmployeeBalances,
      fetchRemoteBalances,
      checkIsEmployeeOnApprovedLeave,
      getApprovedLeaveOnDate,
      applyLeave,
      updateLeave,
      approveLeave,
      rejectLeave,
      cancelLeave,
    ]
  );

  return <LeaveContext.Provider value={value}>{children}</LeaveContext.Provider>;
}
