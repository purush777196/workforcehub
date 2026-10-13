import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { EmployeeProvider } from './context/EmployeeContext';
import { DepartmentProvider } from './context/DepartmentContext';
import { LeaveProvider } from './context/LeaveContext';
import { AttendanceProvider } from './context/AttendanceContext';
import AppRoutes from './routes/AppRoutes';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <EmployeeProvider>
          <DepartmentProvider>
            <LeaveProvider>
              <AttendanceProvider>
                <AppRoutes />
              </AttendanceProvider>
            </LeaveProvider>
          </DepartmentProvider>
        </EmployeeProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

