import { useContext } from 'react';
import { LeaveContext } from './leaveContextDef';

export function useLeaves() {
  const context = useContext(LeaveContext);
  if (!context) {
    throw new Error('useLeaves must be used within a LeaveProvider');
  }
  return context;
}
