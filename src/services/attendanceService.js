import { buildCsrfHeaders } from './authService';

/**
 * WorkforceHub Attendance Data Service
 * 
 * PHASE 8C.3: Attendance Management REST API and Frontend Integration
 * Connects to Spring Boot 3.4.3 backend at /api/attendance backed by PostgreSQL.
 * Manages daily attendance records, clock-in / clock-out calculations,
 * duration formatting, CSRF token handling, and preservation of legacy localStorage.
 */

export const STORAGE_KEY_ATTENDANCE = 'workforcehub_attendance';

export const STANDARD_SHIFT_START = '09:15'; // 09:15 threshold rule: Clock-ins after 09:15 are marked 'Late'

export const ATTENDANCE_STATUSES = ['Present', 'Absent', 'Late', 'Half Day'];

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Map backend UPPERCASE status string to frontend capitalized status
 */
export function mapStatusToFrontend(backendStatus) {
  if (!backendStatus) return 'Present';
  const clean = String(backendStatus).toUpperCase().replace(/\s+/g, '_');
  switch (clean) {
    case 'PRESENT':
      return 'Present';
    case 'LATE':
      return 'Late';
    case 'HALF_DAY':
    case 'HALFDAY':
      return 'Half Day';
    case 'ABSENT':
      return 'Absent';
    default:
      return backendStatus;
  }
}

/**
 * Map frontend status string to backend UPPERCASE enum
 */
export function mapStatusToBackend(frontendStatus) {
  if (!frontendStatus) return 'PRESENT';
  const clean = String(frontendStatus).toUpperCase().replace(/\s+/g, '_');
  switch (clean) {
    case 'PRESENT':
      return 'PRESENT';
    case 'LATE':
      return 'LATE';
    case 'HALF_DAY':
    case 'HALFDAY':
      return 'HALF_DAY';
    case 'ABSENT':
      return 'ABSENT';
    default:
      return clean;
  }
}

/**
 * Normalizes backend AttendanceRecordDto into the shape expected by frontend components.
 */
export function normalizeAttendanceRecord(record) {
  if (!record) return null;
  const rawStatus = record.status;
  const status = mapStatusToFrontend(rawStatus);

  const checkIn = record.checkIn || (record.checkInTime ? String(record.checkInTime).substring(0, 5) : null);
  const checkOut = record.checkOut || (record.checkOutTime ? String(record.checkOutTime).substring(0, 5) : null);
  const date = record.attendanceDate || record.date;

  return {
    ...record,
    id: String(record.id),
    employeeId: record.employeeId ? String(record.employeeId) : null,
    employeeName: record.employeeName || null,
    employeeCode: record.employeeCode || null,
    departmentId: record.departmentId ? String(record.departmentId) : null,
    departmentName: record.departmentName || null,
    date,
    attendanceDate: date,
    checkIn,
    checkInTime: checkIn,
    checkOut,
    checkOutTime: checkOut,
    status,
    notes: record.notes || '',
    minutesWorked: record.minutesWorked ?? null,
    durationDisplay: record.durationDisplay || null,
    createdAt: record.createdAt || null,
    updatedAt: record.updatedAt || null,
  };
}

/**
 * Fetches attendance records from Spring Boot backend REST API.
 */
export async function fetchAttendanceApi(params = {}) {
  const queryParams = new URLSearchParams();
  if (params.all || (!params.page && !params.size && params.page !== 0)) {
    queryParams.set('all', 'true');
  }

  if (params.search) queryParams.set('search', params.search);
  if (params.employeeId) queryParams.set('employeeId', params.employeeId);
  if (params.departmentId) queryParams.set('departmentId', params.departmentId);
  if (params.date) queryParams.set('date', params.date);
  if (params.startDate) queryParams.set('startDate', params.startDate);
  if (params.endDate) queryParams.set('endDate', params.endDate);
  if (params.status && params.status !== 'All Statuses') {
    queryParams.set('status', mapStatusToBackend(params.status));
  }
  if (params.page !== undefined) queryParams.set('page', params.page);
  if (params.size !== undefined) queryParams.set('size', params.size);
  if (params.sortBy) queryParams.set('sortBy', params.sortBy);
  if (params.sortDirection) queryParams.set('sortDirection', params.sortDirection);

  const queryString = queryParams.toString();
  const url = `${API_BASE_URL}/api/attendance${queryString ? `?${queryString}` : ''}`;

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to access attendance records.');
  }

  if (response.status === 403) {
    throw new Error('You do not have permission to access attendance records.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to fetch attendance records (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  if (Array.isArray(envelope?.data)) {
    return envelope.data.map(normalizeAttendanceRecord);
  }

  if (envelope?.data?.content && Array.isArray(envelope.data.content)) {
    return {
      content: envelope.data.content.map(normalizeAttendanceRecord),
      page: envelope.data.page,
      size: envelope.data.size,
      totalElements: envelope.data.totalElements,
      totalPages: envelope.data.totalPages,
      first: envelope.data.first,
      last: envelope.data.last,
    };
  }

  return [];
}

/**
 * Fetches an individual attendance record by UUID.
 */
export async function fetchAttendanceByIdApi(id) {
  const response = await fetch(`${API_BASE_URL}/api/attendance/${id}`, {
    method: 'GET',
    credentials: 'include',
  });

  if (response.status === 404) {
    throw new Error('Attendance record not found.');
  }

  if (response.status === 403) {
    throw new Error('You do not have permission to view this attendance record.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to retrieve attendance record (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeAttendanceRecord(envelope.data);
}

/**
 * Records self-service employee check-in for today via POST /api/attendance/check-in.
 */
export async function checkInApi(requestData = {}) {
  const csrfHeaders = await buildCsrfHeaders();
  const payload = {};
  if (requestData.notes) payload.notes = requestData.notes.trim();
  if (requestData.checkInTime) {
    payload.checkInTime = requestData.checkInTime.length === 5 ? `${requestData.checkInTime}:00` : requestData.checkInTime;
  }

  const response = await fetch(`${API_BASE_URL}/api/attendance/check-in`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to check in.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Check-in failed (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeAttendanceRecord(envelope.data);
}

/**
 * Records self-service employee check-out for today via POST /api/attendance/check-out.
 */
export async function checkOutApi(requestData = {}) {
  const csrfHeaders = await buildCsrfHeaders();
  const payload = {};
  if (requestData.notes) payload.notes = requestData.notes.trim();
  if (requestData.checkOutTime) {
    payload.checkOutTime = requestData.checkOutTime.length === 5 ? `${requestData.checkOutTime}:00` : requestData.checkOutTime;
  }

  const response = await fetch(`${API_BASE_URL}/api/attendance/check-out`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to check out.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Check-out failed (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeAttendanceRecord(envelope.data);
}

/**
 * Creates manual attendance record (Admin only) via POST /api/attendance.
 */
export async function createAttendanceApi(formData) {
  const csrfHeaders = await buildCsrfHeaders();
  const payload = {
    employeeId: formData.employeeId,
    attendanceDate: formData.date || formData.attendanceDate,
    status: mapStatusToBackend(formData.status),
    checkInTime: formData.checkInTime ? (formData.checkInTime.length === 5 ? `${formData.checkInTime}:00` : formData.checkInTime) : null,
    checkOutTime: formData.checkOutTime ? (formData.checkOutTime.length === 5 ? `${formData.checkOutTime}:00` : formData.checkOutTime) : null,
    notes: formData.notes ? formData.notes.trim() : null,
  };

  const response = await fetch(`${API_BASE_URL}/api/attendance`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to create attendance records.');
  }

  if (response.status === 403) {
    throw new Error('Only administrators may create manual attendance records.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to create attendance record (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeAttendanceRecord(envelope.data);
}

/**
 * Updates attendance record / status override (Admin only) via PUT /api/attendance/{id}.
 */
export async function updateAttendanceApi(id, formData) {
  const csrfHeaders = await buildCsrfHeaders();
  const payload = {
    status: mapStatusToBackend(formData.status),
    checkInTime: formData.checkInTime ? (formData.checkInTime.length === 5 ? `${formData.checkInTime}:00` : formData.checkInTime) : null,
    checkOutTime: formData.checkOutTime ? (formData.checkOutTime.length === 5 ? `${formData.checkOutTime}:00` : formData.checkOutTime) : null,
    notes: formData.notes !== undefined && formData.notes !== null ? formData.notes.trim() : null,
  };

  const response = await fetch(`${API_BASE_URL}/api/attendance/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to update attendance records.');
  }

  if (response.status === 403) {
    throw new Error('Only administrators may override attendance records.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to update attendance record (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeAttendanceRecord(envelope.data);
}

/**
 * Retrieves aggregate daily attendance summary metrics via GET /api/attendance/summary.
 */
export async function fetchAttendanceSummaryApi(date) {
  const query = date ? `?date=${encodeURIComponent(date)}` : '';
  const response = await fetch(`${API_BASE_URL}/api/attendance/summary${query}`, {
    method: 'GET',
    credentials: 'include',
  });

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to fetch attendance summary (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return envelope.data;
}

/**
 * Return current local date string in YYYY-MM-DD
 */
export function getLocalTodayDateString() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Return current local time string in HH:MM (24-hour)
 */
export function getLocalCurrentTimeString() {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
}

/**
 * Generate a realistic initial attendance dataset for today's date (preserved for legacy localStorage)
 */
export function generateInitialAttendanceRecords(todayDate) {
  return [
    {
      id: `att-${todayDate}-1001`,
      employeeId: 'emp-1001', // Elena Rostova
      date: todayDate,
      checkIn: '08:45',
      checkOut: '17:15',
      status: 'Present',
      notes: 'Completed full engineering sprint tasks.',
      createdAt: `${todayDate}T08:45:00`,
      updatedAt: `${todayDate}T17:15:00`,
    },
    {
      id: `att-${todayDate}-1003`,
      employeeId: 'emp-1003', // Aisha Patel
      date: todayDate,
      checkIn: '09:05',
      checkOut: null,
      status: 'Present',
      notes: 'Remote design sprint check-in.',
      createdAt: `${todayDate}T09:05:00`,
      updatedAt: `${todayDate}T09:05:00`,
    },
    {
      id: `att-${todayDate}-1004`,
      employeeId: 'emp-1004', // Liam O'Connor
      date: todayDate,
      checkIn: '08:30',
      checkOut: '16:45',
      status: 'Present',
      notes: 'Operations facility opening shift.',
      createdAt: `${todayDate}T08:30:00`,
      updatedAt: `${todayDate}T16:45:00`,
    },
    {
      id: `att-${todayDate}-1005`,
      employeeId: 'emp-1005', // Sophia Chen
      date: todayDate,
      checkIn: '09:40',
      checkOut: null,
      status: 'Late',
      notes: 'Highway traffic delay reported to supervisor.',
      createdAt: `${todayDate}T09:40:00`,
      updatedAt: `${todayDate}T09:40:00`,
    },
    {
      id: `att-${todayDate}-1006`,
      employeeId: 'emp-1006', // David Kim
      date: todayDate,
      checkIn: '08:50',
      checkOut: null,
      status: 'Present',
      notes: 'Security audit on-site check-in.',
      createdAt: `${todayDate}T08:50:00`,
      updatedAt: `${todayDate}T08:50:00`,
    },
    {
      id: `att-${todayDate}-1007`,
      employeeId: 'emp-1007', // Samantha Reed
      date: todayDate,
      checkIn: '09:00',
      checkOut: '13:00',
      status: 'Half Day',
      notes: 'Approved half-day for client demonstration.',
      createdAt: `${todayDate}T09:00:00`,
      updatedAt: `${todayDate}T13:00:00`,
    },
    {
      id: `att-${todayDate}-1008`,
      employeeId: 'emp-1008', // Carlos Mendoza
      date: todayDate,
      checkIn: '09:50',
      checkOut: null,
      status: 'Late',
      notes: 'Subway signal failure.',
      createdAt: `${todayDate}T09:50:00`,
      updatedAt: `${todayDate}T09:50:00`,
    },
    {
      id: `att-${todayDate}-1009`,
      employeeId: 'emp-1009', // Amara Okafor
      date: todayDate,
      checkIn: '08:55',
      checkOut: null,
      status: 'Present',
      notes: 'HR onboarding session morning check-in.',
      createdAt: `${todayDate}T08:55:00`,
      updatedAt: `${todayDate}T08:55:00`,
    },
    {
      id: `att-${todayDate}-1010`,
      employeeId: 'emp-1010', // Jonathan Bailey
      date: todayDate,
      checkIn: '08:40',
      checkOut: '17:00',
      status: 'Present',
      notes: 'Finance closing cycle shift.',
      createdAt: `${todayDate}T08:40:00`,
      updatedAt: `${todayDate}T17:00:00`,
    },
    {
      id: `att-${todayDate}-1012`,
      employeeId: 'emp-1012', // Lucas Dubois
      date: todayDate,
      checkIn: null,
      checkOut: null,
      status: 'Absent',
      notes: 'Unexcused absence logged by department manager.',
      createdAt: `${todayDate}T09:00:00`,
      updatedAt: `${todayDate}T09:00:00`,
    },
  ];
}

/**
 * Safely load attendance records from localStorage (Preserved for backward-compatibility)
 */
export function getStoredAttendance() {
  const today = getLocalTodayDateString();
  try {
    const rawData = localStorage.getItem(STORAGE_KEY_ATTENDANCE);
    if (!rawData) {
      const initial = generateInitialAttendanceRecords(today);
      localStorage.setItem(STORAGE_KEY_ATTENDANCE, JSON.stringify(initial));
      return initial;
    }

    const parsed = JSON.parse(rawData);
    if (!Array.isArray(parsed) || parsed.length === 0) {
      const initial = generateInitialAttendanceRecords(today);
      localStorage.setItem(STORAGE_KEY_ATTENDANCE, JSON.stringify(initial));
      return initial;
    }

    return parsed;
  } catch (error) {
    console.error('Failed to parse workforcehub_attendance from localStorage:', error);
    const initial = generateInitialAttendanceRecords(today);
    try {
      localStorage.setItem(STORAGE_KEY_ATTENDANCE, JSON.stringify(initial));
    } catch {
      // ignore storage quota issues
    }
    return initial;
  }
}

/**
 * Save attendance records to localStorage (Preserved for backward-compatibility)
 */
export function saveStoredAttendance(records) {
  try {
    localStorage.setItem(STORAGE_KEY_ATTENDANCE, JSON.stringify(records));
  } catch (error) {
    console.error('Failed to write workforcehub_attendance to localStorage:', error);
  }
}

/**
 * Check if a check-in time exceeds the lateness threshold (09:15)
 */
export function isCheckInLate(timeStr) {
  if (!timeStr) return false;
  return timeStr > STANDARD_SHIFT_START;
}

/**
 * Calculate working duration in minutes
 * @param {string|null} checkIn - "HH:MM"
 * @param {string|null} checkOut - "HH:MM"
 * @param {string} recordDate - "YYYY-MM-DD"
 * @returns {number|null} Duration in minutes or null
 */
export function calculateMinutesWorked(checkIn, checkOut, recordDate) {
  if (!checkIn) return null;

  const [inHours, inMinutes] = checkIn.split(':').map(Number);
  const inTotalMins = inHours * 60 + inMinutes;

  if (checkOut) {
    const [outHours, outMinutes] = checkOut.split(':').map(Number);
    const outTotalMins = outHours * 60 + outMinutes;
    return Math.max(0, outTotalMins - inTotalMins);
  }

  // If currently checked in today with no check out: compute elapsed
  const todayStr = getLocalTodayDateString();
  if (recordDate === todayStr) {
    const now = new Date();
    const currentTotalMins = now.getHours() * 60 + now.getMinutes();
    return Math.max(0, currentTotalMins - inTotalMins);
  }

  return null;
}

/**
 * Format minutes into a friendly string: e.g. "8h 15m" or "45m"
 */
export function formatWorkingDuration(minutes, inProgress = false) {
  if (minutes === null || minutes === undefined) return '—';
  if (minutes === 0) return inProgress ? '0m (In Progress)' : '0m';

  const hrs = Math.floor(minutes / 60);
  const mins = minutes % 60;

  let text = '';
  if (hrs > 0 && mins > 0) {
    text = `${hrs}h ${mins}m`;
  } else if (hrs > 0) {
    text = `${hrs}h`;
  } else {
    text = `${mins}m`;
  }

  return inProgress ? `${text} (In Progress)` : text;
}

/**
 * Convert 24-hr time (HH:MM) to friendly display format (e.g. 08:45 AM)
 */
export function formatTimeDisplay(timeStr) {
  if (!timeStr) return '—';
  const parts = timeStr.split(':');
  if (parts.length < 2) return timeStr;

  const hours = parseInt(parts[0], 10);
  const minutes = parts[1];
  const ampm = hours >= 12 ? 'PM' : 'AM';
  const displayHours = hours % 12 || 12;

  return `${String(displayHours).padStart(2, '0')}:${minutes} ${ampm}`;
}
