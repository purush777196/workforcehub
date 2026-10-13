import { buildCsrfHeaders } from './authService';

export const STORAGE_KEY = 'workforcehub_employees';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const DEPARTMENTS = [
  'Engineering',
  'Sales & Marketing',
  'Operations & Logistics',
  'Product & UX',
  'People & HR',
  'Finance & Legal',
];

export const EMPLOYMENT_STATUSES = ['Active', 'On Leave', 'Inactive'];

export const EMPLOYMENT_TYPES = ['Full-Time', 'Part-Time', 'Contract'];

/**
 * 16 realistic initial sample employees across departments, statuses, and types
 */
export const INITIAL_EMPLOYEES = [
  {
    id: 'emp-1001',
    employeeId: 'EMP-1001',
    firstName: 'Elena',
    lastName: 'Rostova',
    email: 'elena.rostova@workforcehub.com',
    phone: '+1 (555) 234-5671',
    department: 'Engineering',
    designation: 'Senior Frontend Engineer',
    joiningDate: '2023-03-15',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '742 Evergreen Terrace, Seattle, WA',
  },
  {
    id: 'emp-1002',
    employeeId: 'EMP-1002',
    firstName: 'Marcus',
    lastName: 'Vance',
    email: 'marcus.vance@workforcehub.com',
    phone: '+1 (555) 345-6782',
    department: 'Product & UX',
    designation: 'Staff Product Manager',
    joiningDate: '2022-08-01',
    employmentStatus: 'On Leave',
    employmentType: 'Full-Time',
    address: '1204 Pine Ridge Rd, Austin, TX',
  },
  {
    id: 'emp-1003',
    employeeId: 'EMP-1003',
    firstName: 'Aisha',
    lastName: 'Patel',
    email: 'aisha.patel@workforcehub.com',
    phone: '+1 (555) 456-7893',
    department: 'Product & UX',
    designation: 'Lead UX Designer',
    joiningDate: '2023-01-10',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '45 Kensington High St, London, UK',
  },
  {
    id: 'emp-1004',
    employeeId: 'EMP-1004',
    firstName: 'Liam',
    lastName: "O'Connor",
    email: 'liam.oconnor@workforcehub.com',
    phone: '+1 (555) 567-8904',
    department: 'Operations & Logistics',
    designation: 'Operations Director',
    joiningDate: '2021-11-20',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '88 Grafton Way, Dublin, Ireland',
  },
  {
    id: 'emp-1005',
    employeeId: 'EMP-1005',
    firstName: 'Sophia',
    lastName: 'Chen',
    email: 'sophia.chen@workforcehub.com',
    phone: '+1 (555) 678-9015',
    department: 'Engineering',
    designation: 'Data Scientist',
    joiningDate: '2023-06-12',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '320 Bayview Ave, San Francisco, CA',
  },
  {
    id: 'emp-1006',
    employeeId: 'EMP-1006',
    firstName: 'David',
    lastName: 'Kim',
    email: 'david.kim@workforcehub.com',
    phone: '+1 (555) 789-0126',
    department: 'Engineering',
    designation: 'Cloud Infrastructure Architect',
    joiningDate: '2022-04-18',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '512 Lakeview Blvd, Chicago, IL',
  },
  {
    id: 'emp-1007',
    employeeId: 'EMP-1007',
    firstName: 'Samantha',
    lastName: 'Reed',
    email: 'samantha.reed@workforcehub.com',
    phone: '+1 (555) 890-1237',
    department: 'Sales & Marketing',
    designation: 'VP of Enterprise Sales',
    joiningDate: '2021-09-01',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '90 Park Avenue, New York, NY',
  },
  {
    id: 'emp-1008',
    employeeId: 'EMP-1008',
    firstName: 'Carlos',
    lastName: 'Mendoza',
    email: 'carlos.mendoza@workforcehub.com',
    phone: '+1 (555) 901-2348',
    department: 'Sales & Marketing',
    designation: 'Account Executive',
    joiningDate: '2023-09-15',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '140 Biscayne Blvd, Miami, FL',
  },
  {
    id: 'emp-1009',
    employeeId: 'EMP-1009',
    firstName: 'Amara',
    lastName: 'Okafor',
    email: 'amara.okafor@workforcehub.com',
    phone: '+1 (555) 012-3459',
    department: 'People & HR',
    designation: 'Senior People Partner',
    joiningDate: '2022-10-05',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '22 Peachtree St, Atlanta, GA',
  },
  {
    id: 'emp-1010',
    employeeId: 'EMP-1010',
    firstName: 'Jonathan',
    lastName: 'Bailey',
    email: 'jonathan.bailey@workforcehub.com',
    phone: '+1 (555) 123-4560',
    department: 'Finance & Legal',
    designation: 'Financial Controller',
    joiningDate: '2022-02-14',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '15 Wall Street, New York, NY',
  },
  {
    id: 'emp-1011',
    employeeId: 'EMP-1011',
    firstName: 'Kavita',
    lastName: 'Sharma',
    email: 'kavita.sharma@workforcehub.com',
    phone: '+1 (555) 234-5672',
    department: 'Engineering',
    designation: 'Full Stack Developer',
    joiningDate: '2024-01-15',
    employmentStatus: 'On Leave',
    employmentType: 'Full-Time',
    address: '77 Innovation Way, San Jose, CA',
  },
  {
    id: 'emp-1012',
    employeeId: 'EMP-1012',
    firstName: 'Lucas',
    lastName: 'Dubois',
    email: 'lucas.dubois@workforcehub.com',
    phone: '+1 (555) 345-6783',
    department: 'Operations & Logistics',
    designation: 'Procurement Specialist',
    joiningDate: '2023-04-10',
    employmentStatus: 'Active',
    employmentType: 'Part-Time',
    address: '40 Champs-Élysées, Paris, France',
  },
  {
    id: 'emp-1013',
    employeeId: 'EMP-1013',
    firstName: 'Hannah',
    lastName: 'Schmidt',
    email: 'hannah.schmidt@workforcehub.com',
    phone: '+1 (555) 456-7894',
    department: 'People & HR',
    designation: 'Recruitment Coordinator',
    joiningDate: '2023-11-01',
    employmentStatus: 'Active',
    employmentType: 'Contract',
    address: '18 Friedrichstraße, Berlin, Germany',
  },
  {
    id: 'emp-1014',
    employeeId: 'EMP-1014',
    firstName: 'Tariq',
    lastName: 'Mansour',
    email: 'tariq.mansour@workforcehub.com',
    phone: '+1 (555) 567-8905',
    department: 'Engineering',
    designation: 'QA Automation Engineer',
    joiningDate: '2023-07-20',
    employmentStatus: 'Inactive',
    employmentType: 'Full-Time',
    address: '89 Palm Jumeirah, Dubai, UAE',
  },
  {
    id: 'emp-1015',
    employeeId: 'EMP-1015',
    firstName: 'Olivia',
    lastName: 'Taylor',
    email: 'olivia.taylor@workforcehub.com',
    phone: '+1 (555) 678-9016',
    department: 'Sales & Marketing',
    designation: 'Growth Marketing Manager',
    joiningDate: '2022-05-18',
    employmentStatus: 'Active',
    employmentType: 'Full-Time',
    address: '202 Sunset Blvd, Los Angeles, CA',
  },
  {
    id: 'emp-1016',
    employeeId: 'EMP-1016',
    firstName: 'Vikram',
    lastName: 'Malhotra',
    email: 'vikram.malhotra@workforcehub.com',
    phone: '+1 (555) 789-0127',
    department: 'Finance & Legal',
    designation: 'Corporate Counsel',
    joiningDate: '2023-08-30',
    employmentStatus: 'Active',
    employmentType: 'Contract',
    address: '55 Legal Row, Boston, MA',
  },
];

/**
 * Safely load employees from localStorage, seeding with INITIAL_EMPLOYEES only if empty
 */
export function getStoredEmployees() {
  try {
    const rawData = localStorage.getItem(STORAGE_KEY);
    if (!rawData) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_EMPLOYEES));
      return INITIAL_EMPLOYEES;
    }

    const parsed = JSON.parse(rawData);
    if (!Array.isArray(parsed) || parsed.length === 0) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_EMPLOYEES));
      return INITIAL_EMPLOYEES;
    }

    return parsed;
  } catch (error) {
    console.error('Failed to parse workforcehub_employees from localStorage, using initial sample:', error);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_EMPLOYEES));
    } catch {
      // ignore storage quota issues
    }
    return INITIAL_EMPLOYEES;
  }
}

/**
 * Persist employees array to localStorage
 */
export function saveStoredEmployees(employees) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(employees));
  } catch (error) {
    console.error('Failed to write workforcehub_employees to localStorage:', error);
  }
}

/**
 * Generate next unique business employee ID (e.g., EMP-1017)
 */
export function generateNextEmployeeId(employees) {
  let maxSeq = 1000;
  employees.forEach((emp) => {
    if (emp.employeeId) {
      const match = emp.employeeId.match(/^EMP-(\d+)$/i);
      if (match) {
        const num = parseInt(match[1], 10);
        if (!isNaN(num) && num > maxSeq) {
          maxSeq = num;
        }
      }
    }
  });
  return `EMP-${maxSeq + 1}`;
}

/**
 * Validate employee form data
 * @param {Object} data 
 * @param {Array} existingEmployees 
 * @param {string|null} currentId - ID of employee being updated, if any
 * @returns {Object} { isValid: boolean, errors: Object }
 */
export function validateEmployeeData(data, existingEmployees = [], currentId = null) {
  const errors = {};

  if (!data.firstName || !data.firstName.trim()) {
    errors.firstName = 'First name is required.';
  } else if (data.firstName.trim().length < 2) {
    errors.firstName = 'First name must be at least 2 characters.';
  }

  if (!data.lastName || !data.lastName.trim()) {
    errors.lastName = 'Last name is required.';
  } else if (data.lastName.trim().length < 2) {
    errors.lastName = 'Last name must be at least 2 characters.';
  }

  // Email validation
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!data.email || !data.email.trim()) {
    errors.email = 'Email address is required.';
  } else if (!emailRegex.test(data.email.trim())) {
    errors.email = 'Please provide a valid email address.';
  } else {
    // Duplicate check case-insensitively
    const normalizedEmail = data.email.trim().toLowerCase();
    const isDuplicate = existingEmployees.some((emp) => {
      if (currentId && emp.id === currentId) return false;
      return emp.email && emp.email.trim().toLowerCase() === normalizedEmail;
    });

    if (isDuplicate) {
      errors.email = 'This email address is already in use by another employee.';
    }
  }

  if (!data.phone || !data.phone.trim()) {
    errors.phone = 'Phone number is required.';
  }

  if (!data.department || !data.department.trim()) {
    errors.department = 'Department selection is required.';
  }

  if (!data.designation || !data.designation.trim()) {
    errors.designation = 'Designation/Job title is required.';
  }

  if (!data.joiningDate || !data.joiningDate.trim()) {
    errors.joiningDate = 'Joining date is required.';
  }

  if (!data.employmentStatus) {
    errors.employmentStatus = 'Employment status is required.';
  }

  if (!data.employmentType) {
    errors.employmentType = 'Employment type is required.';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors,
  };
}

/**
 * Normalizes backend EmployeeDto / entity representations into the shape expected by frontend components.
 */
export function normalizeEmployee(emp) {
  if (!emp) return null;

  const statusMap = {
    ACTIVE: 'Active',
    ON_LEAVE: 'On Leave',
    INACTIVE: 'Inactive',
  };

  const typeMap = {
    FULL_TIME: 'Full-Time',
    PART_TIME: 'Part-Time',
    CONTRACT: 'Contract',
  };

  return {
    ...emp,
    id: emp.id,
    employeeId: emp.employeeId || emp.employeeCode,
    employeeCode: emp.employeeCode || emp.employeeId,
    firstName: emp.firstName || '',
    lastName: emp.lastName || '',
    fullName: emp.fullName || `${emp.firstName || ''} ${emp.lastName || ''}`.trim(),
    email: emp.email || '',
    phone: emp.phone || '',
    department: emp.department || emp.departmentName || 'Unassigned',
    departmentName: emp.departmentName || emp.department || 'Unassigned',
    departmentId: emp.departmentId,
    designation: emp.designation || emp.jobTitle || '',
    jobTitle: emp.jobTitle || emp.designation || '',
    managerId: emp.managerId,
    managerName: emp.managerName,
    employmentStatus: statusMap[emp.employmentStatus] || emp.employmentStatus || 'Active',
    employmentType: typeMap[emp.employmentType] || emp.employmentType || 'Full-Time',
    joiningDate: emp.joiningDate || '',
    address: emp.address || '',
  };
}

/**
 * Fetches employees from the Spring Boot backend REST API.
 * 
 * Supports optional search, department, status, type, sorting, and pagination.
 * By default requests all employees (all=true) to preserve seamless client-side
 * consistency for dashboard metrics and cross-module contexts.
 */
export async function fetchEmployeesApi(params = {}) {
  const queryParams = new URLSearchParams();
  if (params.all || (!params.page && !params.size && params.page !== 0)) {
    queryParams.set('all', 'true');
  }

  if (params.search) queryParams.set('search', params.search);
  if (params.departmentId) queryParams.set('departmentId', params.departmentId);
  if (params.department && params.department !== 'All Departments') queryParams.set('department', params.department);
  if (params.status) queryParams.set('status', params.status);
  if (params.type) queryParams.set('type', params.type);
  if (params.page !== undefined) queryParams.set('page', params.page);
  if (params.size !== undefined) queryParams.set('size', params.size);
  if (params.sortBy) queryParams.set('sortBy', params.sortBy);
  if (params.sortDirection) queryParams.set('sortDirection', params.sortDirection);

  const queryString = queryParams.toString();
  const url = `${API_BASE_URL}/api/employees${queryString ? `?${queryString}` : ''}`;

  const response = await fetch(url, {
    method: 'GET',
    credentials: 'include',
  });

  if (response.status === 401) {
    throw new Error('Authentication is required to access employee records.');
  }

  if (response.status === 403) {
    throw new Error('You do not have administrative privileges to manage employee records.');
  }

  if (!response.ok) {
    let errPayload;
    try {
      errPayload = await response.json();
    } catch {
      // ignore
    }
    throw new Error(errPayload?.message || `Failed to fetch employees (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  if (Array.isArray(envelope?.data)) {
    return envelope.data.map(normalizeEmployee);
  }

  if (envelope?.data?.content && Array.isArray(envelope.data.content)) {
    return {
      content: envelope.data.content.map(normalizeEmployee),
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
 * Fetches an individual employee by UUID from the Spring Boot backend REST API.
 */
export async function fetchEmployeeByIdApi(id) {
  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
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
    throw new Error(errPayload?.message || `Employee not found (HTTP ${response.status})`);
  }

  const envelope = await response.json();
  return normalizeEmployee(envelope.data);
}

/**
 * Creates a new employee in PostgreSQL via the Spring Boot REST API.
 */
export async function createEmployeeApi(formData) {
  const csrfHeaders = await buildCsrfHeaders();

  const payload = {
    employeeCode: formData.employeeId || formData.employeeCode,
    employeeId: formData.employeeId || formData.employeeCode,
    firstName: formData.firstName?.trim(),
    lastName: formData.lastName?.trim(),
    email: formData.email?.trim().toLowerCase(),
    phone: formData.phone?.trim(),
    jobTitle: formData.designation?.trim() || formData.jobTitle?.trim(),
    designation: formData.designation?.trim() || formData.jobTitle?.trim(),
    department: formData.department?.trim(),
    departmentId: formData.departmentId || null,
    managerId: formData.managerId || null,
    employmentStatus: formData.employmentStatus,
    employmentType: formData.employmentType,
    joiningDate: formData.joiningDate,
    address: formData.address?.trim() || '',
  };

  const response = await fetch(`${API_BASE_URL}/api/employees`, {
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
    const error = new Error(errPayload?.message || 'Failed to create employee record');
    error.status = response.status;
    error.validationErrors = errPayload?.validationErrors || {};
    throw error;
  }

  const envelope = await response.json();
  return normalizeEmployee(envelope.data);
}

/**
 * Updates an existing employee in PostgreSQL via the Spring Boot REST API.
 */
export async function updateEmployeeApi(id, formData) {
  const csrfHeaders = await buildCsrfHeaders();

  const payload = {
    employeeCode: formData.employeeId || formData.employeeCode,
    employeeId: formData.employeeId || formData.employeeCode,
    firstName: formData.firstName?.trim(),
    lastName: formData.lastName?.trim(),
    email: formData.email?.trim().toLowerCase(),
    phone: formData.phone?.trim(),
    jobTitle: formData.designation?.trim() || formData.jobTitle?.trim(),
    designation: formData.designation?.trim() || formData.jobTitle?.trim(),
    department: formData.department?.trim(),
    departmentId: formData.departmentId || null,
    managerId: formData.managerId || null,
    employmentStatus: formData.employmentStatus,
    employmentType: formData.employmentType,
    joiningDate: formData.joiningDate,
    address: formData.address?.trim() || '',
  };

  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
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
    const error = new Error(errPayload?.message || 'Failed to update employee record');
    error.status = response.status;
    error.validationErrors = errPayload?.validationErrors || {};
    throw error;
  }

  const envelope = await response.json();
  return normalizeEmployee(envelope.data);
}

/**
 * Deletes an existing employee from PostgreSQL via the Spring Boot REST API.
 */
export async function deleteEmployeeApi(id) {
  const csrfHeaders = await buildCsrfHeaders();

  const response = await fetch(`${API_BASE_URL}/api/employees/${id}`, {
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
    const error = new Error(errPayload?.message || 'Failed to delete employee record');
    error.status = response.status;
    throw error;
  }

  return true;
}
