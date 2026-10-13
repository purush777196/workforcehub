import { useContext } from 'react';
import { DepartmentContext } from './departmentContextDef';

export function useDepartments() {
  const context = useContext(DepartmentContext);
  if (!context) {
    throw new Error('useDepartments must be used within a DepartmentProvider');
  }
  return context;
}
