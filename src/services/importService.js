import { buildCsrfHeaders } from './authService';
import { STORAGE_KEY as EMPLOYEES_KEY } from './employeeService';
import { STORAGE_KEY_DEPARTMENTS as DEPARTMENTS_KEY } from './departmentService';
import { STORAGE_KEY_ATTENDANCE as ATTENDANCE_KEY } from './attendanceService';
import { LEAVES_STORAGE_KEY } from './leaveService';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const LOCAL_STORAGE_KEYS = {
  EMPLOYEES: EMPLOYEES_KEY,
  DEPARTMENTS: DEPARTMENTS_KEY,
  ATTENDANCE: ATTENDANCE_KEY,
  LEAVES: LEAVES_STORAGE_KEY,
};

/**
 * Safely gathers browser localStorage inventory without modifying, resetting, or deleting records.
 * Returns arrays of records and count metrics.
 */
export function gatherLocalStorageInventory() {
  const result = {
    departments: [],
    employees: [],
    attendance: [],
    leaves: [],
    counts: {
      departments: 0,
      employees: 0,
      attendance: 0,
      leaves: 0,
      total: 0,
    },
    keysFound: {
      departments: false,
      employees: false,
      attendance: false,
      leaves: false,
    },
  };

  try {
    const rawDepts = localStorage.getItem(DEPARTMENTS_KEY);
    if (rawDepts) {
      const parsed = JSON.parse(rawDepts);
      if (Array.isArray(parsed)) {
        result.departments = parsed;
        result.counts.departments = parsed.length;
        result.keysFound.departments = true;
      }
    }
  } catch (err) {
    console.warn('[importService] Error reading departments from localStorage:', err);
  }

  try {
    const rawEmps = localStorage.getItem(EMPLOYEES_KEY);
    if (rawEmps) {
      const parsed = JSON.parse(rawEmps);
      if (Array.isArray(parsed)) {
        result.employees = parsed;
        result.counts.employees = parsed.length;
        result.keysFound.employees = true;
      }
    }
  } catch (err) {
    console.warn('[importService] Error reading employees from localStorage:', err);
  }

  try {
    const rawAtt = localStorage.getItem(ATTENDANCE_KEY);
    if (rawAtt) {
      const parsed = JSON.parse(rawAtt);
      if (Array.isArray(parsed)) {
        result.attendance = parsed;
        result.counts.attendance = parsed.length;
        result.keysFound.attendance = true;
      }
    }
  } catch (err) {
    console.warn('[importService] Error reading attendance from localStorage:', err);
  }

  try {
    const rawLeaves = localStorage.getItem(LEAVES_STORAGE_KEY);
    if (rawLeaves) {
      const parsed = JSON.parse(rawLeaves);
      if (Array.isArray(parsed)) {
        result.leaves = parsed;
        result.counts.leaves = parsed.length;
        result.keysFound.leaves = true;
      }
    }
  } catch (err) {
    console.warn('[importService] Error reading leaves from localStorage:', err);
  }

  result.counts.total =
    result.counts.departments +
    result.counts.employees +
    result.counts.attendance +
    result.counts.leaves;

  return result;
}

/**
 * Initiates non-destructive backend preview of data import.
 * Calls POST /api/admin/import/preview with session credentials and CSRF.
 */
export async function previewImportApi(payload) {
  const response = await fetch(`${API_BASE_URL}/api/admin/import/preview`, {
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
    let errorMsg = `Preview request failed (HTTP ${response.status})`;
    try {
      const errJson = await response.json();
      if (errJson.message) errorMsg = errJson.message;
    } catch {
      // ignore
    }
    const err = new Error(errorMsg);
    err.status = response.status;
    throw err;
  }

  const envelope = await response.json();
  return envelope.data;
}

/**
 * Executes confirmed transactional data import into PostgreSQL.
 * Calls POST /api/admin/import/execute.
 */
export async function executeImportApi(executeRequest) {
  const response = await fetch(`${API_BASE_URL}/api/admin/import/execute`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...buildCsrfHeaders(),
    },
    body: JSON.stringify(executeRequest),
  });

  if (!response.ok) {
    let errorMsg = `Import execution failed (HTTP ${response.status})`;
    try {
      const errJson = await response.json();
      if (errJson.message) errorMsg = errJson.message;
    } catch {
      // ignore
    }
    const err = new Error(errorMsg);
    err.status = response.status;
    throw err;
  }

  const envelope = await response.json();
  return envelope.data;
}

/**
 * Retrieves audit status for a specific historical import job.
 * Calls GET /api/admin/import/jobs/{jobId}.
 */
export async function fetchImportJobApi(jobId) {
  const response = await fetch(`${API_BASE_URL}/api/admin/import/jobs/${jobId}`, {
    method: 'GET',
    credentials: 'include',
    headers: {
      Accept: 'application/json',
    },
  });

  if (!response.ok) {
    let errorMsg = `Failed to fetch import job ${jobId} (HTTP ${response.status})`;
    try {
      const errJson = await response.json();
      if (errJson.message) errorMsg = errJson.message;
    } catch {
      // ignore
    }
    const err = new Error(errorMsg);
    err.status = response.status;
    throw err;
  }

  const envelope = await response.json();
  return envelope.data;
}
