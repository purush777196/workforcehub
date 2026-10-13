import React from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';
import { useEmployees } from '../../context/useEmployees';

export default function Toast() {
  const { toast, dismissToast } = useEmployees();

  if (!toast) return null;

  const isSuccess = toast.type === 'success';
  const isError = toast.type === 'error';

  return (
    <div
      className={`toast-notification ${toast.type || 'info'}`}
      role="status"
      aria-live="polite"
      id="app-toast-notification"
    >
      <div className="toast-icon">
        {isSuccess && <CheckCircle2 size={18} />}
        {isError && <AlertCircle size={18} />}
        {!isSuccess && !isError && <Info size={18} />}
      </div>
      <div className="toast-message">{toast.message}</div>
      <button
        type="button"
        className="toast-close-btn"
        onClick={dismissToast}
        aria-label="Dismiss notification"
      >
        <X size={16} />
      </button>
    </div>
  );
}
