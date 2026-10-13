import { buildCsrfHeaders } from './authService';

/**
 * WorkforceHub Department Data Service
 * Handles REST communication with Spring Boot backend (/api/departments),
 * normalizing responses, CSRF token handling, and fallback localStorage helpers.
 */

export const STORAGE_KEY_DEPARTMENTS = 'workforcehub_departments';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * 6 sensible initial sample departments matching Phase 2 employee assignments
 */
export const INITIAL_DEPARTMENTS = [
  {
    id: 'dept-1',
    departmentId: 'DEPT-001',
    name: 'Engineering',
    description: 'Core software engineering, cloud architecture, quality engineering, and data platforms.',
    managerEmployeeId: 'emp-1001', // Elena Rostova
    status: 'Active',
    createdAt: '2023-01-15',
  },
  {
    id: 'dept-2',
    departmentId: 'DEPT-002',
    name: 'Sales & Marketing',
    description: 'Enterprise client acquisition, commercial partnerships, demand generation, and brand marketing.',
    managerEmployeeId: 'emp-1007', // Samantha Reed (VP of Enterprise Sales)
    status: 'Active',
    createdAt: '2023-01-20',
  },
  {
    id: 'dept-3',
    departmentId: 'DEPT-003',
    name: 'Operations & Logistics',
    description: 'Global facilities, procurement, workplace operations, and supply chain coordination.',
    managerEmployeeId: 'emp-1004', // Liam O'Connor (Operations Director)
    status: 'Active',
    createdAt: '2023-02-01',
  },
  {
    id: 'dept-4',
    departmentId: 'DEPT-004',
    name: 'Product & UX',
    description: 'Product lifecycle strategy, user research, interaction design, and roadmap execution.',
    managerEmployeeId: 'emp-1002', // Marcus Vance (Staff Product Manager)
    status: 'Active',
    createdAt: '2023-02-10',
  },
  {
    id: 'dept-5',
    departmentId: 'DEPT-005',
    name: 'People & HR',
    description: 'Talent acquisition, employee experience, culture development, compensation, and workplace benefits.',
    managerEmployeeId: 'emp-1009', // Amara Okafor (Senior People Partner)
    status: 'Active',
    createdAt: '2023-02-15',
  },
  {
    id: 'dept-6',
    departmentId: 'DEPT-006',
    name: 'Finance & Legal',
    description: 'Fiscal controllership, payroll management, corporate governance, and statutory regulatory compliance.',
    managerEmployeeId: 'emp-1010', // Jonathan Bailey (Financial Controller)
    status: 'Active',
    createdAt: '2023-03-01',
  },
];

/**
 * Safely load departments from localStorage, initializing sample records if empty
 */
export function getStoredDepartments() {
  try {
    const rawData = localStorage.getItem(STORAGE_KEY_DEPARTMENTS);
    if (!rawData) {
      localStorage.setItem(STORAGE_KEY_DEPARTMENTS, JSON.stringify(INITIAL_DEPARTMENTS));
      return INITIAL_DEPARTMENTS;
    }

    const parsed = JSON.parse(rawData);
    if (!Array.isArray(parsed) || parsed.length === 0) {
      localStorage.setItem(STORAGE_KEY_DEPARTMENTS, JSON.stringify(INITIAL_DEPARTMENTS));
      return INITIAL_DEPARTMENTS;
    }

    return parsed;
  } catch (error) {
    console.error('Failed to parse workforcehub_departments from localStorage:', error);
    try {
      localStorage.setItem(STORAGE_KEY_DEPARTMENTS, JSON.stringify(INITIAL_DEPARTMENTS));
    } catch {
      // ignore storage quota issues
    }
    return INITIAL_DEPARTMENTS;
  }
}

/**
 * Persist departments array to localStorage
 */
export function saveStoredDepartments(departments) {
  try {
    localStorage.setItem(STORAGE_KEY_DEPARTMENTS, JSON.stringify(departments));
  } catch (error) {
    console.error('Failed to write workforcehub_departments to localStorage:', error);
  }
}

/**
 * Generate next unique department business ID (e.g. DEPT-007)
 */
export function generateNextDepartmentId(departments) {
  let maxSeq = 0;
  departments.forEach((dept) => {
    const code = dept.departmentId || dept.departmentCode;
    if (code) {
      const match = code.match(/^DEPT-(\d+)$/i);
      if (match) {
        const num = parseInt(match[1], 10);
        if (!isNaN(num) && num > maxSeq) {
          maxSeq = num;
        }
      }
    }
  });
  const nextNum = maxSeq + 1;
  const padded = String(nextNum).padStart(3, '0');
  return `DEPT-${padded}`;
}

/**
 * Validate department form data
 * @param {Object} data 
 * @param {Array} existingDepartments 
 * @param {string|null} currentId 
 * @returns {Object} { isValid: boolean, errors: Object }
 */
export function validateDepartmentData(data, existingDepartments = [], currentId = null) {
  const errors = {};

  if (!data.name || !data.name.trim()) {
    errors.name = 'Department name is required.';
  } else if (data.name.trim().length < 2) {
    errors.name = 'Department name must be at least 2 characters.';
  } else {
    // Check duplicate name case-insensitively and trimmed
    const normalizedName = data.name.trim().toLowerCase();
    const isDuplicate = existingDepartments.some((dept) => {
      if (currentId && dept.id === currentId) return false;
      return dept.name && dept.name.trim().toLowerCase() === normalizedName;
    });

    if (isDuplicate) {
      errors.name = 'A department with this name already exists.';
    }
  }

  if (!data.status) {
    errors.status = 'Department status is required.';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}

/**
 * Normalizes backend DepartmentDto representation into the shape expected by frontend components.
 */
export function normalizeDepartment(dept) {
  if (!dept) return null;

  return {
    ...dept,
    id: dept.id,
    departmentId: dept.departmentId || dept.departmentCode || '',
    departmentCode: dept.departmentCode || dept.departmentId || '',
    name: dept.name || '',
    description: dept.description || '',
    managerEmployeeId: dept.managerEmployeeId || dept.managerId || null,
    managerId: dept.managerId || dept.managerEmployeeId || null,
    managerName: dept.managerName || null,
    managerEmployeeCode: dept.managerEmployeeCode || null,
    employeeCount: dept.employeeCount ?? dept.headcount ?? 0,
    headcount: dept.headcount ?? dept.employeeCount ?? 0,
    status: dept.status || 'Active',
    createdAt: dept.createdAt || '',
    updatedAt: dept.updatedAt || '',
  };
}

/**
 * Fetches departments from the Spring Boot backend REST API.
 * 
 * Supports optional search, status, sorting, and pagination.
 * By default requests all departments (all=true) to preserve seamless client-side
 * consistency for dropdowns, dashboard metrics, and cross-module contexts.
 */
export async function fetchDepartmentsApi(params = {}) {
  const queryParams = new URLSearchParams();
  if (params.all || (!params.page && !params.size && params.page !== 0)) {
    queryParams.set('all', 'true');
  }

  if (params.search) queryParams.set('search', params.search);
  if (params.status) queryParams.set('status', params.status);
  if (params.page !== undefined) queryParams.set('page', params.page);
  if (params.size !== undefined) queryParams.set('size', params.size);
  if (params.sortBy) queryParams.set('sortBy', params.sortBy);
  if (params.sortDirection) queryParams.set('sortDirection', params.sortDirection);

  const queryString = queryParams.toString();
  const url = `${API_BASE_URL}/api/departments${queryString ? `?${queryString}` : ''}`;

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to access department records.');
  }

  if (response.status === 403) {
    throw new Error('You do not have administrative privileges to manage departments.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to fetch departments (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  if (Array.isArray(envelope?.data)) {
    return envelope.data.map(normalizeDepartment);
  }

  if (envelope?.data?.content && Array.isArray(envelope.data.content)) {
    return {
      content: envelope.data.content.map(normalizeDepartment),
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
 * Fetches an individual department by UUID from the Spring Boot backend REST API.
 */
export async function fetchDepartmentByIdApi(id) {
  const response = await fetch(`${API_BASE_URL}/api/departments/${id}`, {
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
    throw new Error(errPayload?.message || `Department not found (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeDepartment(envelope.data);
}

/**
 * Creates a new department in PostgreSQL via the Spring Boot REST API.
 */
export async function createDepartmentApi(formData) {
  const csrfHeaders = await buildCsrfHeaders();

  const payload = {
    departmentCode: formData.departmentId || formData.departmentCode || null,
    departmentId: formData.departmentId || formData.departmentCode || null,
    name: formData.name?.trim(),
    description: formData.description?.trim() || '',
    managerId: formData.managerEmployeeId || formData.managerId || null,
    managerEmployeeId: formData.managerEmployeeId || formData.managerId || null,
    status: formData.status || 'Active',
  };

  const response = await fetch(`${API_BASE_URL}/api/departments`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    const error = new Error(errPayload?.message || 'Failed to create department record');
    error.status = response.status;
    error.validationErrors = errPayload?.validationErrors || {};
    throw error;
  }

  const envelope = await response.json();
  return normalizeDepartment(envelope.data);
}

/**
 * Updates an existing department in PostgreSQL via the Spring Boot REST API.
 */
export async function updateDepartmentApi(id, formData) {
  const csrfHeaders = await buildCsrfHeaders();

  const payload = {
    departmentCode: formData.departmentId || formData.departmentCode || null,
    departmentId: formData.departmentId || formData.departmentCode || null,
    name: formData.name?.trim(),
    description: formData.description?.trim() || '',
    managerId: formData.managerEmployeeId || formData.managerId || null,
    managerEmployeeId: formData.managerEmployeeId || formData.managerId || null,
    status: formData.status || 'Active',
  };

  const response = await fetch(`${API_BASE_URL}/api/departments/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...csrfHeaders,
    },
    credentials: 'include',
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    const error = new Error(errPayload?.message || 'Failed to update department record');
    error.status = response.status;
    error.validationErrors = errPayload?.validationErrors || {};
    throw error;
  }

  const envelope = await response.json();
  return normalizeDepartment(envelope.data);
}

/**
 * Deletes an existing department from PostgreSQL via the Spring Boot REST API.
 */
export async function deleteDepartmentApi(id) {
  const csrfHeaders = await buildCsrfHeaders();

  const response = await fetch(`${API_BASE_URL}/api/departments/${id}`, {
    method: 'DELETE',
    headers: {
      ...csrfHeaders,
    },
    credentials: 'include',
  });

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    const error = new Error(errPayload?.message || 'Failed to delete department record');
    error.status = response.status;
    throw error;
  }

  return true;
}
