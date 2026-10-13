import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  fetchAttendanceApi,
  checkInApi,
  checkOutApi,
  createAttendanceApi,
  updateAttendanceApi,
  fetchAttendanceSummaryApi,
  getLocalTodayDateString,
  getLocalCurrentTimeString,
  isCheckInLate,
  formatWorkingDuration,
  calculateMinutesWorked,
} from '../services/attendanceService';
import { AttendanceContext } from './attendanceContextDef';
import { useEmployees } from './useEmployees';
import { useLeaves } from './useLeaves';
import { useAuth } from './useAuth';

export function AttendanceProvider({ children }) {
  const [records, setRecords] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isMutating, setIsMutating] = useState(false);
  const [error, setError] = useState(null);
  const [backendSummary, setBackendSummary] = useState(null);

  const { employees, showToast } = useEmployees();
  const { isEmployeeOnApprovedLeave, getApprovedLeaveOnDate } = useLeaves();
  const { user, isAuthenticated, isAdmin, isEmployee, isLoading: isAuthLoading } = useAuth();

  const todayDate = useMemo(() => getLocalTodayDateString(), []);

  /**
   * Refreshes summary metrics from backend REST API
   */
  const refreshSummary = useCallback(async (date) => {
    try {
      const summary = await fetchAttendanceSummaryApi(date);
      setBackendSummary(summary);
      return summary;
    } catch (err) {
      console.warn('[AttendanceContext] Could not refresh backend summary:', err);
      return null;
    }
  }, []);

  /**
   * Loads attendance records from Spring Boot backend REST API
   */
  const loadAttendance = useCallback(async () => {
    if (!isAuthenticated) {
      setRecords([]);
      setIsLoading(false);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const data = await fetchAttendanceApi({ all: true });
      const recordList = Array.isArray(data) ? data : (data?.content || []);
      setRecords(recordList);
      setError(null);

      const today = getLocalTodayDateString();
      try {
        const summary = await fetchAttendanceSummaryApi(today);
        setBackendSummary(summary);
      } catch (sumErr) {
        console.warn('[AttendanceContext] Summary load warning:', sumErr);
      }
    } catch (err) {
      console.error('[AttendanceContext] Failed to load attendance from backend:', err);
      setError(err.message || 'Unable to connect to attendance service.');
    } finally {
      setIsLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    let ignore = false;

    async function syncAttendance() {
      if (isAuthLoading) return;

      if (!isAuthenticated) {
        if (!ignore) {
          setRecords([]);
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
        const data = await fetchAttendanceApi({ all: true });
        const recordList = Array.isArray(data) ? data : (data?.content || []);
        if (!ignore) {
          setRecords(recordList);
          setError(null);
        }

        const today = getLocalTodayDateString();
        try {
          const summary = await fetchAttendanceSummaryApi(today);
          if (!ignore) {
            setBackendSummary(summary);
          }
        } catch (sumErr) {
          console.warn('[AttendanceContext] Summary load warning:', sumErr);
        }
      } catch (err) {
        if (!ignore) {
          console.error('[AttendanceContext] Failed to load attendance from backend:', err);
          setError(err.message || 'Unable to connect to attendance service.');
        }
      } finally {
        if (!ignore) {
          setIsLoading(false);
        }
      }
    }

    syncAttendance();

    return () => {
      ignore = true;
    };
  }, [isAuthLoading, isAuthenticated]);

  /**
   * Helper: Retrieve attendance record for an employee on a specific date
   */
  const getRecord = useCallback(
    (employeeId, date) => {
      if (!employeeId || !date) return null;
      return (
        records.find(
          (r) =>
            (r.employeeId === employeeId || (r.employeeCode && r.employeeCode === employeeId)) &&
            r.date === date
        ) || null
      );
    },
    [records]
  );

  /**
   * Clock-In Action for today via REST API
   */
  const checkIn = useCallback(
    async (targetEmployeeId, options = {}) => {
      const today = getLocalTodayDateString();
      const currentTime = getLocalCurrentTimeString();

      const employee = employees.find(
        (e) => e.id === targetEmployeeId || e.employeeId === targetEmployeeId
      );

      // Validate inactive employee
      if (employee && employee.employmentStatus === 'Inactive') {
        showToast(`Cannot check in: ${employee.firstName} ${employee.lastName} is inactive.`, 'error');
        return { success: false, message: 'Employee is inactive.' };
      }

      // Validate approved leave
      if (employee && (employee.employmentStatus === 'On Leave' || isEmployeeOnApprovedLeave(employee.id, today))) {
        const approvedReq = getApprovedLeaveOnDate(employee.id, today);
        const reasonDetail = approvedReq
          ? ` (${approvedReq.leaveType} Leave: ${approvedReq.startDate} to ${approvedReq.endDate})`
          : '';
        showToast(
          `Cannot check in: ${employee.firstName} ${employee.lastName} is on approved leave${reasonDetail}.`,
          'error'
        );
        return { success: false, message: 'Employee is on leave.' };
      }

      setIsMutating(true);
      try {
        let resultRecord;

        // If employee self-service check-in
        if (isEmployee || (user && targetEmployeeId && user.id === targetEmployeeId)) {
          resultRecord = await checkInApi({
            notes: options.notes,
            checkInTime: options.checkInTime,
          });
        } else if (isAdmin) {
          // Administrator manually recording check-in for an employee
          const isLate = isCheckInLate(currentTime);
          resultRecord = await createAttendanceApi({
            employeeId: employee?.id || targetEmployeeId,
            date: today,
            status: isLate ? 'Late' : 'Present',
            checkInTime: currentTime,
            notes: options.notes || (isLate ? 'Clock-in exceeded standard morning shift threshold.' : ''),
          });
        } else {
          resultRecord = await checkInApi({
            notes: options.notes,
            checkInTime: options.checkInTime,
          });
        }

        setRecords((prev) => {
          const filtered = prev.filter(
            (r) => !(r.employeeId === resultRecord.employeeId && r.date === resultRecord.date)
          );
          return [resultRecord, ...filtered];
        });

        const isLate = resultRecord.status === 'Late';
        const lateNotice = isLate ? ' (Marked Late: exceeded 09:15 AM threshold)' : '';
        const empName = resultRecord.employeeName || (employee ? `${employee.firstName} ${employee.lastName}` : 'Employee');

        showToast(
          `Checked in ${empName} at ${resultRecord.checkIn || currentTime}${lateNotice}!`,
          isLate ? 'info' : 'success'
        );

        await refreshSummary(today);
        return { success: true, record: resultRecord };
      } catch (err) {
        const msg = err.message || 'Check-in failed.';
        showToast(msg, 'error');
        return { success: false, message: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [
      employees,
      isEmployee,
      isAdmin,
      user,
      showToast,
      isEmployeeOnApprovedLeave,
      getApprovedLeaveOnDate,
      refreshSummary,
    ]
  );

  /**
   * Clock-Out Action for today via REST API
   */
  const checkOut = useCallback(
    async (targetEmployeeId, options = {}) => {
      const today = getLocalTodayDateString();
      const currentTime = getLocalCurrentTimeString();

      const employee = employees.find(
        (e) => e.id === targetEmployeeId || e.employeeId === targetEmployeeId
      );

      setIsMutating(true);
      try {
        let resultRecord;

        // If employee self-service check-out
        if (isEmployee || (user && targetEmployeeId && user.id === targetEmployeeId)) {
          resultRecord = await checkOutApi({
            notes: options.notes,
            checkOutTime: options.checkOutTime,
          });
        } else if (isAdmin) {
          // Administrator check-out: find existing record
          const existing = records.find(
            (r) =>
              (r.employeeId === targetEmployeeId || r.employeeCode === targetEmployeeId) &&
              r.date === today
          );
          if (!existing) {
            throw new Error('Cannot check out: employee has not checked in today.');
          }

          resultRecord = await updateAttendanceApi(existing.id, {
            status: existing.status,
            checkInTime: existing.checkIn,
            checkOutTime: currentTime,
            notes: options.notes || existing.notes,
          });
        } else {
          resultRecord = await checkOutApi({
            notes: options.notes,
            checkOutTime: options.checkOutTime,
          });
        }

        setRecords((prev) => {
          const filtered = prev.filter(
            (r) => !(r.employeeId === resultRecord.employeeId && r.date === resultRecord.date)
          );
          return [resultRecord, ...filtered];
        });

        const mins = calculateMinutesWorked(resultRecord.checkIn, resultRecord.checkOut, today);
        const durationStr = formatWorkingDuration(mins);
        const empName = resultRecord.employeeName || (employee ? `${employee.firstName} ${employee.lastName}` : 'Employee');

        showToast(
          `Checked out ${empName} at ${resultRecord.checkOut || currentTime} (Worked: ${durationStr})!`,
          'success'
        );

        await refreshSummary(today);
        return { success: true, record: resultRecord };
      } catch (err) {
        const msg = err.message || 'Check-out failed.';
        showToast(msg, 'error');
        return { success: false, message: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [
      employees,
      isEmployee,
      isAdmin,
      user,
      records,
      showToast,
      refreshSummary,
    ]
  );

  /**
   * Admin Status Override / Manual Record Management via REST API
   */
  const updateAttendanceRecord = useCallback(
    async ({ employeeId, date, status, checkInTime, checkOutTime, notes }) => {
      const employee = employees.find(
        (e) => e.id === employeeId || e.employeeId === employeeId
      );

      // Validate times
      let finalCheckIn = checkInTime || null;
      let finalCheckOut = checkOutTime || null;

      if (status === 'Absent') {
        finalCheckIn = null;
        finalCheckOut = null;
      } else if ((status === 'Present' || status === 'Late' || status === 'Half Day') && !finalCheckIn) {
        finalCheckIn = '09:00';
      }

      if (finalCheckIn && finalCheckOut && finalCheckOut < finalCheckIn) {
        showToast('Check-out time cannot precede check-in time.', 'error');
        return { success: false, message: 'Check-out precedes check-in.' };
      }

      setIsMutating(true);
      try {
        const existing = records.find(
          (r) =>
            (r.employeeId === employeeId || r.employeeCode === employeeId) &&
            r.date === date
        );

        let resultRecord;
        if (existing && existing.id && !existing.id.startsWith('att-')) {
          resultRecord = await updateAttendanceApi(existing.id, {
            status,
            checkInTime: finalCheckIn,
            checkOutTime: finalCheckOut,
            notes,
          });
        } else {
          resultRecord = await createAttendanceApi({
            employeeId: employee?.id || employeeId,
            date,
            status,
            checkInTime: finalCheckIn,
            checkOutTime: finalCheckOut,
            notes,
          });
        }

        setRecords((prev) => {
          const filtered = prev.filter(
            (r) => !(r.employeeId === resultRecord.employeeId && r.date === resultRecord.date)
          );
          return [resultRecord, ...filtered];
        });

        const empName = resultRecord.employeeName || (employee ? `${employee.firstName} ${employee.lastName}` : 'Employee');
        showToast(`Updated attendance status to "${status}" for ${empName}.`, 'success');

        await refreshSummary(date);
        return { success: true, record: resultRecord };
      } catch (err) {
        const msg = err.message || 'Failed to update attendance record.';
        showToast(msg, 'error');
        return { success: false, message: msg };
      } finally {
        setIsMutating(false);
      }
    },
    [employees, records, showToast, refreshSummary]
  );

  /**
   * Calculate summary statistics for any selected date
   */
  const getDateStats = useCallback(
    (date) => {
      const activeOrLeaveEmployees = employees.filter((e) => e.employmentStatus !== 'Inactive');
      const totalEligible = activeOrLeaveEmployees.length;
      const activeEmpIdSet = new Set(activeOrLeaveEmployees.map((e) => e.id));

      const onLeaveEmployees = activeOrLeaveEmployees.filter(
        (e) => e.employmentStatus === 'On Leave' || isEmployeeOnApprovedLeave(e.id, date)
      );
      const onLeaveCount = onLeaveEmployees.length;
      const onLeaveEmpIds = new Set(onLeaveEmployees.map((e) => e.id));

      const dateRecords = records.filter(
        (r) => r.date === date && (activeEmpIdSet.size === 0 || activeEmpIdSet.has(r.employeeId))
      );

      const attendingRecords = dateRecords.filter((r) => !onLeaveEmpIds.has(r.employeeId));
      const presentCount = attendingRecords.filter((r) => r.status === 'Present').length;
      const lateCount = attendingRecords.filter((r) => r.status === 'Late').length;
      const halfDayCount = attendingRecords.filter((r) => r.status === 'Half Day').length;
      const totalAttending = presentCount + lateCount + halfDayCount;

      const explicitlyAbsentCount = attendingRecords.filter((r) => r.status === 'Absent').length;
      const activeUnmarkedCount = activeOrLeaveEmployees.filter((emp) => {
        if (onLeaveEmpIds.has(emp.id)) return false;
        return !dateRecords.some((r) => r.employeeId === emp.id);
      }).length;

      const totalAbsent = explicitlyAbsentCount + activeUnmarkedCount;

      const attendanceDenominator = Math.max(0, totalEligible - onLeaveCount);
      const attendanceRate =
        attendanceDenominator > 0
          ? Math.round((totalAttending / attendanceDenominator) * 100)
          : 0;

      // When backendSummary is available for this date, combine with frontend leave resolution
      if (backendSummary && backendSummary.date === date && totalEligible > 0) {
        return {
          totalEligible: backendSummary.totalEligible || totalEligible,
          totalEmployees: backendSummary.totalEmployees || employees.length,
          totalAttending: backendSummary.totalAttending || totalAttending,
          presentCount: backendSummary.presentCount || presentCount,
          lateCount: backendSummary.lateCount || lateCount,
          halfDayCount: backendSummary.halfDayCount || halfDayCount,
          totalAbsent: totalAbsent,
          onLeaveCount: onLeaveCount || backendSummary.onLeaveCount,
          attendanceRate: attendanceRate,
        };
      }

      return {
        totalEligible,
        totalEmployees: employees.length,
        totalAttending,
        presentCount,
        lateCount,
        halfDayCount,
        totalAbsent,
        onLeaveCount,
        attendanceRate,
      };
    },
    [employees, records, isEmployeeOnApprovedLeave, backendSummary]
  );

  const todayStats = useMemo(() => getDateStats(todayDate), [getDateStats, todayDate]);

  const value = useMemo(
    () => ({
      records,
      todayDate,
      todayStats,
      isLoading,
      isMutating,
      error,
      refreshAttendance: loadAttendance,
      getRecord,
      checkIn,
      checkOut,
      updateAttendanceRecord,
      getDateStats,
    }),
    [
      records,
      todayDate,
      todayStats,
      isLoading,
      isMutating,
      error,
      loadAttendance,
      getRecord,
      checkIn,
      checkOut,
      updateAttendanceRecord,
      getDateStats,
    ]
  );

  return (
    <AttendanceContext.Provider value={value}>
      {children}
    </AttendanceContext.Provider>
  );
}
