import { buildCsrfHeaders } from './authService';

/**
 * WorkforceHub Leave Management Service
 * Persistence, policy calculation, date validation, and overlap detection.
 * PHASE 8C.4: Leave Management REST API and Frontend Integration
 */

export const LEAVES_STORAGE_KEY = 'workforcehub_leaves';

/**
 * Configurable demonstration leave policies
 * Calendar days per employee per calendar year
 */
export const LEAVE_POLICIES = {
  Annual: {
    label: 'Annual Leave',
    allowanceDays: 20,
    description: 'Vacation and paid rest days (demonstration standard: 20 calendar days/year).',
    badgeVariant: 'primary',
  },
  Sick: {
    label: 'Sick Leave',
    allowanceDays: 12,
    description: 'Medical appointments, recovery, and illness (12 calendar days/year).',
    badgeVariant: 'warning',
  },
  Personal: {
    label: 'Personal Leave',
    allowanceDays: 5,
    description: 'Urgent personal or family commitments (5 calendar days/year).',
    badgeVariant: 'info',
  },
  Unpaid: {
    label: 'Unpaid Leave',
    allowanceDays: 30,
    description: 'Authorized leave without pay requiring special managerial sign-off (30 calendar days/year).',
    badgeVariant: 'neutral',
  },
};

export const LEAVE_TYPES = ['Annual', 'Sick', 'Personal', 'Unpaid'];
export const LEAVE_STATUSES = ['Pending', 'Approved', 'Rejected'];

/**
 * Calculates calendar days between two dates inclusive (demonstration policy)
 * Example: 2026-10-15 to 2026-10-17 is 3 calendar days.
 */
export function calculateCalendarDays(startDate, endDate) {
  if (!startDate || !endDate) return 0;
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);

  if (isNaN(start.getTime()) || isNaN(end.getTime()) || end < start) {
    return 0;
  }

  const diffMs = end.getTime() - start.getTime();
  return Math.round(diffMs / (1000 * 60 * 60 * 24)) + 1;
}

/**
 * Checks if two date intervals overlap inclusive
 */
export function isDateRangeOverlapping(startA, endA, startB, endB) {
  if (!startA || !endA || !startB || !endB) return false;
  return startA <= endB && endA >= startB;
}

/**
 * Checks for overlapping Pending or Approved leave requests for the same employee
 */
export function findOverlappingRequest(employeeId, startDate, endDate, allRequests, excludeRequestId = null) {
  if (!employeeId || !startDate || !endDate || !Array.isArray(allRequests)) return null;

  return allRequests.find((req) => {
    if (req.employeeId !== employeeId) return false;
    if (excludeRequestId && req.id === excludeRequestId) return false;
    if (req.status !== 'Pending' && req.status !== 'Approved') return false;
    return isDateRangeOverlapping(startDate, endDate, req.startDate, req.endDate);
  }) || null;
}

/**
 * Calculates approved leave balances and remaining days for an employee in a given year
 */
export function getEmployeeLeaveBalances(employeeId, year = new Date().getFullYear(), allRequests = []) {
  const currentYear = Number(year);

  // Consider only Approved requests for this employee in the specified calendar year
  const approvedThisYear = allRequests.filter((req) => {
    if (req.employeeId !== employeeId || req.status !== 'Approved') return false;
    const reqYear = new Date(`${req.startDate}T00:00:00`).getFullYear();
    return reqYear === currentYear;
  });

  const balances = {};
  for (const [type, policy] of Object.entries(LEAVE_POLICIES)) {
    const usedDays = approvedThisYear
      .filter((r) => r.leaveType === type)
      .reduce((sum, r) => sum + (Number(r.requestedDays) || 0), 0);

    const allowance = policy.allowanceDays;
    const remainingDays = Math.max(0, allowance - usedDays);

    balances[type] = {
      allowance,
      usedDays,
      remainingDays,
      isExceeded: usedDays > allowance,
    };
  }

  return balances;
}

/**
 * Helper to determine if an employee is on approved leave on a specific calendar date (YYYY-MM-DD)
 */
export function isEmployeeOnApprovedLeave(employeeId, dateStr, allRequests = []) {
  if (!employeeId || !dateStr || !Array.isArray(allRequests)) return false;
  return allRequests.some((req) => {
    if (req.employeeId !== employeeId || req.status !== 'Approved') return false;
    return dateStr >= req.startDate && dateStr <= req.endDate;
  });
}

/**
 * Helper to retrieve the approved leave request for an employee on a specific date
 */
export function getApprovedLeaveForEmployee(employeeId, dateStr, allRequests = []) {
  if (!employeeId || !dateStr || !Array.isArray(allRequests)) return null;
  return (
    allRequests.find((req) => {
      if (req.employeeId !== employeeId || req.status !== 'Approved') return false;
      return dateStr >= req.startDate && dateStr <= req.endDate;
    }) || null
  );
}

/**
 * Initial seed requests when workforcehub_leaves has never been initialized
 */
export const INITIAL_LEAVE_REQUESTS = [
  {
    id: 'leave-1001',
    employeeId: 'emp-1002', // Marcus Vance (Staff Product Manager)
    leaveType: 'Annual',
    startDate: '2026-10-12',
    endDate: '2026-10-16',
    reason: 'Annual family autumn retreat in Colorado.',
    status: 'Approved',
    requestedDays: 5,
    submittedAt: '2026-10-05T09:30:00.000Z',
    reviewedAt: '2026-10-06T11:15:00.000Z',
    reviewedBy: 'Alex Morgan (HR Administrator)',
    reviewerComment: 'Approved. Product handoff to Aisha Patel confirmed.',
    updatedAt: '2026-10-06T11:15:00.000Z',
  },
  {
    id: 'leave-1002',
    employeeId: 'emp-1011', // Kavita Sharma (Full Stack Developer)
    leaveType: 'Sick',
    startDate: '2026-10-12',
    endDate: '2026-10-14',
    reason: 'Viral fever recuperation with doctor certificate.',
    status: 'Approved',
    requestedDays: 3,
    submittedAt: '2026-10-11T08:15:00.000Z',
    reviewedAt: '2026-10-11T09:40:00.000Z',
    reviewedBy: 'Alex Morgan (HR Administrator)',
    reviewerComment: 'Approved under standard medical policy. Rest well.',
    updatedAt: '2026-10-11T09:40:00.000Z',
  },
  {
    id: 'leave-1003',
    employeeId: 'emp-1005', // Sophia Chen (Data Scientist)
    leaveType: 'Annual',
    startDate: '2026-10-20',
    endDate: '2026-10-24',
    reason: 'Attending sibling wedding in Seattle followed by personal rest days.',
    status: 'Pending',
    requestedDays: 5,
    submittedAt: '2026-10-10T14:20:00.000Z',
    reviewedAt: null,
    reviewedBy: null,
    reviewerComment: null,
    updatedAt: '2026-10-10T14:20:00.000Z',
  },
  {
    id: 'leave-1004',
    employeeId: 'emp-1001', // Elena Rostova (Senior Frontend Engineer)
    leaveType: 'Personal',
    startDate: '2026-10-19',
    endDate: '2026-10-19',
    reason: 'Scheduled home utility installation and maintenance.',
    status: 'Pending',
    requestedDays: 1,
    submittedAt: '2026-10-12T10:05:00.000Z',
    reviewedAt: null,
    reviewedBy: null,
    reviewerComment: null,
    updatedAt: '2026-10-12T10:05:00.000Z',
  },
  {
    id: 'leave-1005',
    employeeId: 'emp-1007', // Samantha Reed (VP of Enterprise Sales)
    leaveType: 'Annual',
    startDate: '2026-11-02',
    endDate: '2026-11-06',
    reason: 'Mid-quarter rest and family trip to Lake Tahoe.',
    status: 'Pending',
    requestedDays: 5,
    submittedAt: '2026-10-09T16:45:00.000Z',
    reviewedAt: null,
    reviewedBy: null,
    reviewerComment: null,
    updatedAt: '2026-10-09T16:45:00.000Z',
  },
  {
    id: 'leave-1006',
    employeeId: 'emp-1004', // Liam O'Connor (Operations Director)
    leaveType: 'Personal',
    startDate: '2026-10-02',
    endDate: '2026-10-03',
    reason: 'Family anniversary celebration and travel.',
    status: 'Approved',
    requestedDays: 2,
    submittedAt: '2026-09-28T11:00:00.000Z',
    reviewedAt: '2026-09-29T14:10:00.000Z',
    reviewedBy: 'Alex Morgan (HR Administrator)',
    reviewerComment: 'Approved. Operations coverage arranged with Lucas Dubois.',
    updatedAt: '2026-09-29T14:10:00.000Z',
  },
  {
    id: 'leave-1007',
    employeeId: 'emp-1006', // David Kim (Cloud Infrastructure Architect)
    leaveType: 'Personal',
    startDate: '2026-10-05',
    endDate: '2026-10-06',
    reason: 'Extended weekend personal vacation.',
    status: 'Rejected',
    requestedDays: 2,
    submittedAt: '2026-10-01T09:10:00.000Z',
    reviewedAt: '2026-10-02T13:30:00.000Z',
    reviewedBy: 'Alex Morgan (HR Administrator)',
    reviewerComment: 'Cannot approve: scheduled core AWS multi-region failover testing requires cloud architect on call. Please submit for the following week.',
    updatedAt: '2026-10-02T13:30:00.000Z',
  },
  {
    id: 'leave-1008',
    employeeId: 'emp-1012', // Lucas Dubois (Procurement Specialist)
    leaveType: 'Unpaid',
    startDate: '2026-11-16',
    endDate: '2026-11-20',
    reason: 'Extended international travel to assist relatives.',
    status: 'Pending',
    requestedDays: 5,
    submittedAt: '2026-10-11T15:30:00.000Z',
    reviewedAt: null,
    reviewedBy: null,
    reviewerComment: null,
    updatedAt: '2026-10-11T15:30:00.000Z',
  },
];

/**
 * Retrieve leave requests from localStorage with safe fallback and seeding
 */
export function getStoredLeaves() {
  try {
    const raw = localStorage.getItem(LEAVES_STORAGE_KEY);
    if (!raw) {
      saveStoredLeaves(INITIAL_LEAVE_REQUESTS);
      return INITIAL_LEAVE_REQUESTS;
    }
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed;
    }
    console.warn('[leaveService] Invalid leaves array in localStorage, reseeding default records.');
    saveStoredLeaves(INITIAL_LEAVE_REQUESTS);
    return INITIAL_LEAVE_REQUESTS;
  } catch (err) {
    console.error('[leaveService] Failed to read leaves from localStorage:', err);
    return INITIAL_LEAVE_REQUESTS;
  }
}

/**
 * Save leave requests to localStorage
 */
export function saveStoredLeaves(leaves) {
  try {
    localStorage.setItem(LEAVES_STORAGE_KEY, JSON.stringify(leaves));
  } catch (err) {
    console.error('[leaveService] Failed to persist leaves to localStorage:', err);
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Maps backend leave status (UPPERCASE) to UI format (Capitalized).
 */
export function mapLeaveStatusToFrontend(backendStatus) {
  if (!backendStatus) return 'Pending';
  const clean = String(backendStatus).toUpperCase().trim();
  switch (clean) {
    case 'PENDING':
      return 'Pending';
    case 'APPROVED':
      return 'Approved';
    case 'REJECTED':
      return 'Rejected';
    default:
      return backendStatus;
  }
}

/**
 * Maps UI leave status to backend enum.
 */
export function mapLeaveStatusToBackend(frontendStatus) {
  if (!frontendStatus) return 'PENDING';
  const clean = String(frontendStatus).toUpperCase().trim();
  switch (clean) {
    case 'PENDING':
      return 'PENDING';
    case 'APPROVED':
      return 'APPROVED';
    case 'REJECTED':
      return 'REJECTED';
    default:
      return clean;
  }
}

/**
 * Maps backend leave type (UPPERCASE) to UI format (Capitalized).
 */
export function mapLeaveTypeToFrontend(backendType) {
  if (!backendType) return 'Annual';
  const clean = String(backendType).toUpperCase().trim();
  switch (clean) {
    case 'ANNUAL':
      return 'Annual';
    case 'SICK':
      return 'Sick';
    case 'PERSONAL':
      return 'Personal';
    case 'UNPAID':
      return 'Unpaid';
    default:
      return backendType;
  }
}

/**
 * Maps UI leave type to backend enum.
 */
export function mapLeaveTypeToBackend(frontendType) {
  if (!frontendType) return 'ANNUAL';
  const clean = String(frontendType).toUpperCase().trim();
  switch (clean) {
    case 'ANNUAL':
      return 'ANNUAL';
    case 'SICK':
      return 'SICK';
    case 'PERSONAL':
      return 'PERSONAL';
    case 'UNPAID':
      return 'UNPAID';
    default:
      return clean;
  }
}

/**
 * Normalizes backend LeaveRequestDto into the shape expected by frontend components.
 */
export function normalizeLeaveRecord(record) {
  if (!record) return null;
  const status = mapLeaveStatusToFrontend(record.status);
  const leaveType = mapLeaveTypeToFrontend(record.leaveType);
  const requestedDays = Number(record.requestedDays || record.daysCount || 0);

  return {
    ...record,
    id: String(record.id),
    employeeId: record.employeeId ? String(record.employeeId) : null,
    employeeName: record.employeeName || null,
    employeeCode: record.employeeCode || null,
    departmentName: record.departmentName || null,
    leaveType,
    status,
    startDate: record.startDate,
    endDate: record.endDate,
    requestedDays,
    daysCount: requestedDays,
    reason: record.reason || '',
    submittedAt: record.submittedAt || record.createdAt || null,
    reviewedAt: record.reviewedAt || null,
    reviewedBy: record.reviewedBy || record.reviewedByName || null,
    reviewerComment: record.reviewerComment || record.reviewComment || null,
    createdAt: record.createdAt || null,
    updatedAt: record.updatedAt || null,
  };
}

/**
 * Fetches leave requests from Spring Boot REST API.
 */
export async function fetchLeavesApi(params = {}) {
  const query = new URLSearchParams();
  if (params.search) query.append('search', params.search);
  if (params.employeeId) query.append('employeeId', params.employeeId);
  if (params.status && params.status !== 'All' && params.status !== 'All Statuses') {
    query.append('status', mapLeaveStatusToBackend(params.status));
  }
  if (params.leaveType && params.leaveType !== 'All' && params.leaveType !== 'All Types') {
    query.append('leaveType', mapLeaveTypeToBackend(params.leaveType));
  }
  if (params.startDate) query.append('startDate', params.startDate);
  if (params.endDate) query.append('endDate', params.endDate);
  if (params.page !== undefined) query.append('page', params.page);
  if (params.size !== undefined) query.append('size', params.size);
  if (params.sortBy) query.append('sortBy', params.sortBy);
  if (params.sortDirection) query.append('sortDirection', params.sortDirection);
  if (params.all) query.append('all', 'true');

  const url = `${API_BASE_URL}/api/leaves${query.toString() ? `?${query.toString()}` : ''}`;
  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to fetch leave requests (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  const rawData = json.data;

  if (Array.isArray(rawData)) {
    return {
      content: rawData.map(normalizeLeaveRecord),
      totalElements: rawData.length,
      totalPages: 1,
      page: 0,
      size: rawData.length,
    };
  }

  if (rawData && Array.isArray(rawData.content)) {
    return {
      ...rawData,
      content: rawData.content.map(normalizeLeaveRecord),
    };
  }

  return {
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 10,
  };
}

/**
 * Fetches a single leave request by ID.
 */
export async function fetchLeaveByIdApi(id) {
  const response = await fetch(`${API_BASE_URL}/api/leaves/${id}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to fetch leave request ${id} (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return normalizeLeaveRecord(json.data);
}

/**
 * Applies for leave via Spring Boot REST API.
 */
export async function applyLeaveApi(leaveData) {
  const payload = {
    employeeId: leaveData.employeeId,
    leaveType: mapLeaveTypeToBackend(leaveData.leaveType),
    startDate: leaveData.startDate,
    endDate: leaveData.endDate,
    reason: leaveData.reason?.trim(),
  };

  const response = await fetch(`${API_BASE_URL}/api/leaves`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to submit leave request (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return normalizeLeaveRecord(json.data);
}

/**
 * Updates a pending leave request via Spring Boot REST API.
 */
export async function updateLeaveApi(id, leaveData) {
  const payload = {
    leaveType: mapLeaveTypeToBackend(leaveData.leaveType),
    startDate: leaveData.startDate,
    endDate: leaveData.endDate,
    reason: leaveData.reason?.trim(),
  };

  const response = await fetch(`${API_BASE_URL}/api/leaves/${id}`, {
    method: 'PUT',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to update leave request (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return normalizeLeaveRecord(json.data);
}

/**
 * Approves a pending leave request via Spring Boot REST API.
 */
export async function approveLeaveApi(id, { reviewerComment } = {}) {
  const payload = {
    reviewComment: reviewerComment || 'Approved by HR Administrator',
  };

  const response = await fetch(`${API_BASE_URL}/api/leaves/${id}/approve`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to approve leave request (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return normalizeLeaveRecord(json.data);
}

/**
 * Rejects a pending leave request via Spring Boot REST API.
 */
export async function rejectLeaveApi(id, { reviewerComment } = {}) {
  const payload = {
    reviewComment: reviewerComment,
  };

  const response = await fetch(`${API_BASE_URL}/api/leaves/${id}/reject`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to reject leave request (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return normalizeLeaveRecord(json.data);
}

/**
 * Cancels a pending leave request via Spring Boot REST API.
 */
export async function cancelLeaveApi(id) {
  const response = await fetch(`${API_BASE_URL}/api/leaves/${id}`, {
    method: 'DELETE',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to cancel leave request (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  return true;
}

/**
 * Fetches current authenticated employee's leave balances.
 */
export async function fetchMyLeaveBalancesApi(year) {
  const query = year ? `?year=${year}` : '';
  const response = await fetch(`${API_BASE_URL}/api/leaves/balance/me${query}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to fetch leave balances (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return json.data;
}

/**
 * Fetches specific employee's leave balances (ADMIN-only).
 */
export async function fetchEmployeeLeaveBalancesApi(employeeId, year) {
  const query = year ? `?year=${year}` : '';
  const response = await fetch(`${API_BASE_URL}/api/leaves/balance/${employeeId}${query}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to fetch employee leave balances (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return json.data;
}

/**
 * Fetches summary statistics for leaves.
 */
export async function fetchLeaveSummaryApi() {
  const response = await fetch(`${API_BASE_URL}/api/leaves/summary`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const message = errorData.message || `Failed to fetch leave summary (HTTP ${response.status})`;
    const err = new Error(message);
    err.status = response.status;
    throw err;
  }

  const json = await response.json();
  return json.data;
}
