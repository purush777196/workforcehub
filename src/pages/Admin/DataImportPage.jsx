import {
  Database,
  UploadCloud,
  CheckCircle2,
  AlertTriangle,
  AlertCircle,
  ShieldCheck,
  RotateCcw,
  Sparkles,
  Info,
  Lock,
} from 'lucide-react';
import PageHeader from '../../components/common/PageHeader';
import Badge from '../../components/common/Badge';
import { useAuth } from '../../context/useAuth';
import { useEmployees } from '../../context/useEmployees';
import {
  gatherLocalStorageInventory,
  previewImportApi,
  executeImportApi,
  LOCAL_STORAGE_KEYS,
} from '../../services/importService';
import '../../styles/import.css';

export default function DataImportPage() {
  const { isAdmin } = useAuth();
  const { showToast } = useEmployees();

  // Inventory state initialized synchronously
  const [inventory, setInventory] = useState(() => {
    try {
      return gatherLocalStorageInventory();
    } catch {
      return null;
    }
  });
  const [isScanning, setIsScanning] = useState(false);

  // Preview state
  const [previewData, setPreviewData] = useState(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState(null);
  const [activeTab, setActiveTab] = useState('departments');

  // Confirmation & Strategy state
  const [conflictStrategy, setConflictStrategy] = useState('SKIP_EXISTING');
  const [confirmBackup, setConfirmBackup] = useState(false);
  const [confirmExecution, setConfirmExecution] = useState(false);

  // Execution state
  const [isExecuting, setIsExecuting] = useState(false);
  const [executionResult, setExecutionResult] = useState(null);
  const [executionError, setExecutionError] = useState(null);

  // Rescan localStorage on demand
  const scanStorage = useCallback(() => {
    setIsScanning(true);
    try {
      const inv = gatherLocalStorageInventory();
      setInventory(inv);
      showToast('Local storage inventory refreshed.', 'info');
    } catch (err) {
      console.error('[DataImportPage] Failed to scan storage:', err);
      showToast('Error reading local storage inventory.', 'error');
    } finally {
      setIsScanning(false);
    }
  }, [showToast]);

  // Handle Preview Generation
  const handleGeneratePreview = async () => {
    if (!inventory) return;

    setIsPreviewLoading(true);
    setPreviewError(null);
    setExecutionResult(null);
    setExecutionError(null);

    const payload = {
      departments: inventory.departments,
      employees: inventory.employees,
      attendance: inventory.attendance,
      leaves: inventory.leaves,
    };

    try {
      const preview = await previewImportApi(payload);
      setPreviewData(preview);
      showToast('Safe preview generated successfully from server.', 'success');
    } catch (err) {
      console.error('[DataImportPage] Preview error:', err);
      const msg = err.message || 'Failed to generate preview from backend.';
      setPreviewError(msg);
      showToast(msg, 'error');
    } finally {
      setIsPreviewLoading(false);
    }
  };

  // Handle Confirmed Execution
  const handleExecuteImport = async () => {
    if (!confirmBackup || !confirmExecution || !previewData || !inventory) {
      showToast('Please confirm the safety checklist before executing.', 'warning');
      return;
    }

    setIsExecuting(true);
    setExecutionError(null);

    const executeRequest = {
      previewId: previewData.previewId,
      conflictStrategy,
      confirmBackupTaken: true,
      payload: {
        departments: inventory.departments,
        employees: inventory.employees,
        attendance: inventory.attendance,
        leaves: inventory.leaves,
      },
    };

    try {
      const result = await executeImportApi(executeRequest);
      setExecutionResult(result);
      showToast(`Data import completed with status: ${result.status}`, 'success');
    } catch (err) {
      console.error('[DataImportPage] Execution error:', err);
      const msg = err.message || 'Data import execution failed.';
      setExecutionError(msg);
      showToast(msg, 'error');
    } finally {
      setIsExecuting(false);
    }
  };

  if (!isAdmin) {
    return (
      <div className="import-page-container">
        <PageHeader
          title="Data Migration"
          subtitle="Access Restricted"
          badge={<Badge variant="warning">Admin Only</Badge>}
        />
        <div className="import-card" style={{ textAlign: 'center', padding: '3rem' }}>
          <Lock size={48} style={{ color: 'var(--color-warning)', margin: '0 auto 1rem' }} />
          <h3>Administrative Privileges Required</h3>
          <p style={{ color: 'var(--color-text-secondary)' }}>
            Only system administrators can inspect or execute database migration operations.
          </p>
        </div>
      </div>
    );
  }

  const currentStep = executionResult ? 5 : previewData ? (confirmBackup && confirmExecution ? 4 : 3) : 2;

  return (
    <div className="import-page-container" id="data-import-page-root">
      <PageHeader
        title="Data Migration & Safe Import"
        subtitle="Phase 8C.5 · Controlled preview, conflict reconciliation, and explicit administrator import from browser storage into PostgreSQL."
        badge={
          <Badge variant="primary" icon={Sparkles}>
            Phase 8C.5 Migration
          </Badge>
        }
        actions={
          <button
            type="button"
            className="btn btn-secondary"
            onClick={scanStorage}
            disabled={isScanning || isPreviewLoading || isExecuting}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.45rem 0.85rem' }}
          >
            <RotateCcw size={16} /> Rescan Storage
          </button>
        }
      />

      {/* 5-Step Process Tracker */}
      <div className="import-stages-tracker">
        <div className={`import-stage-pill ${currentStep >= 1 ? 'completed active' : ''}`}>
          <span className="import-stage-step-num">1</span>
          <span>Source Scan</span>
        </div>
        <div className={`import-stage-pill ${currentStep >= 2 ? (currentStep > 2 ? 'completed' : 'active') : ''}`}>
          <span className="import-stage-step-num">2</span>
          <span>Safe Preview</span>
        </div>
        <div className={`import-stage-pill ${currentStep >= 3 ? (currentStep > 3 ? 'completed' : 'active') : ''}`}>
          <span className="import-stage-step-num">3</span>
          <span>Conflict Strategy</span>
        </div>
        <div className={`import-stage-pill ${currentStep >= 4 ? (currentStep > 4 ? 'completed' : 'active') : ''}`}>
          <span className="import-stage-step-num">4</span>
          <span>Confirmation</span>
        </div>
        <div className={`import-stage-pill ${currentStep >= 5 ? 'completed active' : ''}`}>
          <span className="import-stage-step-num">5</span>
          <span>Execution & Report</span>
        </div>
      </div>

      {/* Stage 1: Browser Storage Inventory */}
      <div className="import-card">
        <div className="import-card-header">
          <div className="import-card-title">
            <Database size={20} style={{ color: 'var(--color-primary)' }} />
            Browser LocalStorage Inventory
          </div>
          <Badge variant="neutral">Read-Only Safe Scan</Badge>
        </div>

        <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
          WorkforceHub has discovered the following legacy browser records preserved in your browser session.
          These keys are strictly preserved and will never be cleared, reset, or overwritten during this process.
        </p>

        <div className="import-inventory-grid">
          <div className="import-inventory-stat">
            <span className="import-stat-label">Departments</span>
            <span className="import-stat-val">{inventory?.counts.departments ?? 0}</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              Key: {LOCAL_STORAGE_KEYS.DEPARTMENTS}
            </span>
          </div>
          <div className="import-inventory-stat">
            <span className="import-stat-label">Employees</span>
            <span className="import-stat-val">{inventory?.counts.employees ?? 0}</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              Key: {LOCAL_STORAGE_KEYS.EMPLOYEES}
            </span>
          </div>
          <div className="import-inventory-stat">
            <span className="import-stat-label">Attendance Records</span>
            <span className="import-stat-val">{inventory?.counts.attendance ?? 0}</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              Key: {LOCAL_STORAGE_KEYS.ATTENDANCE}
            </span>
          </div>
          <div className="import-inventory-stat">
            <span className="import-stat-label">Leave Requests</span>
            <span className="import-stat-val">{inventory?.counts.leaves ?? 0}</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>
              Key: {LOCAL_STORAGE_KEYS.LEAVES}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
          <button
            type="button"
            className="btn btn-primary"
            onClick={handleGeneratePreview}
            disabled={isPreviewLoading || isExecuting || (inventory && inventory.counts.total === 0)}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.6rem 1.25rem' }}
          >
            <UploadCloud size={18} />
            {isPreviewLoading ? 'Evaluating Server Preview...' : 'Generate Safe Preview'}
          </button>
        </div>
      </div>

      {/* Preview Error Banner */}
      {previewError && (
        <div className="import-card" style={{ borderColor: 'var(--color-danger)', background: 'rgba(239, 68, 68, 0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--color-danger)' }}>
            <AlertCircle size={22} />
            <div>
              <strong>Preview Generation Failed</strong>
              <div style={{ fontSize: '0.85rem' }}>{previewError}</div>
            </div>
          </div>
        </div>
      )}

      {/* Stage 2: Preview Results & Breakdown */}
      {previewData && (
        <div className="import-card">
          <div className="import-card-header">
            <div className="import-card-title">
              <ShieldCheck size={20} style={{ color: 'var(--color-success)' }} />
              Server-Side Preview & Reconciliation Metrics
            </div>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
              Job ID: <code>{previewData.previewId}</code>
            </div>
          </div>

          <div className="import-preview-kpi-grid">
            <div className="import-kpi-badge-card eligible">
              <span className="import-kpi-num">{previewData.totalEligibleAll}</span>
              <span className="import-kpi-title">Eligible for Import</span>
            </div>
            <div className="import-kpi-badge-card existing">
              <span className="import-kpi-num">{previewData.totalExistingAll}</span>
              <span className="import-kpi-title">Already in PostgreSQL</span>
            </div>
            <div className="import-kpi-badge-card conflict">
              <span className="import-kpi-num">{previewData.totalConflictsAll}</span>
              <span className="import-kpi-title">Potential Conflicts</span>
            </div>
            <div className="import-kpi-badge-card unresolved">
              <span className="import-kpi-num">{previewData.totalUnresolvedAll}</span>
              <span className="import-kpi-title">Unresolved References</span>
            </div>
            <div className="import-kpi-badge-card invalid">
              <span className="import-kpi-num">{previewData.totalInvalidAll}</span>
              <span className="import-kpi-title">Invalid Records</span>
            </div>
          </div>

          {/* Module Tab Navigation */}
          <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border-color)', marginBottom: '1rem' }}>
            {['departments', 'employees', 'attendance', 'leaves'].map((mod) => {
              const summary = previewData[mod];
              return (
                <button
                  key={mod}
                  type="button"
                  onClick={() => setActiveTab(mod)}
                  style={{
                    padding: '0.65rem 1rem',
                    border: 'none',
                    background: 'transparent',
                    borderBottom: activeTab === mod ? '2px solid var(--color-primary)' : '2px solid transparent',
                    color: activeTab === mod ? 'var(--color-primary)' : 'var(--color-text-secondary)',
                    fontWeight: activeTab === mod ? 600 : 400,
                    cursor: 'pointer',
                    textTransform: 'capitalize',
                    fontSize: '0.9rem',
                  }}
                >
                  {mod} ({summary?.totalSourceRecords ?? 0})
                </button>
              );
            })}
          </div>

          {/* Active Tab Details */}
          {(() => {
            const modSummary = previewData[activeTab];
            if (!modSummary) return null;

            return (
              <div>
                <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem', fontSize: '0.85rem' }}>
                  <span>Source: <strong>{modSummary.totalSourceRecords}</strong></span>
                  <span style={{ color: 'var(--color-success)' }}>Eligible: <strong>{modSummary.eligibleForImport}</strong></span>
                  <span style={{ color: 'var(--color-primary)' }}>Existing in DB: <strong>{modSummary.existingInDatabase}</strong></span>
                  <span style={{ color: 'var(--color-warning)' }}>Conflicts: <strong>{modSummary.conflicts}</strong></span>
                  <span style={{ color: 'var(--color-danger)' }}>Invalid: <strong>{modSummary.invalidRecords}</strong></span>
                </div>

                {modSummary.issues && modSummary.issues.length > 0 ? (
                  <div style={{ overflowX: 'auto' }}>
                    <table className="data-table" style={{ width: '100%', fontSize: '0.85rem' }}>
                      <thead>
                        <tr>
                          <th>Source ID</th>
                          <th>Identifier</th>
                          <th>Status Type</th>
                          <th>Analysis & Reconciliation Details</th>
                        </tr>
                      </thead>
                      <tbody>
                        {modSummary.issues.map((issue, idx) => (
                          <tr key={idx}>
                            <td><code>{issue.sourceId}</code></td>
                            <td><strong>{issue.identifier}</strong></td>
                            <td>
                              <Badge
                                variant={
                                  issue.issueType === 'EXISTING'
                                    ? 'primary'
                                    : issue.issueType === 'CONFLICT'
                                    ? 'warning'
                                    : issue.issueType === 'UNRESOLVED_REF'
                                    ? 'neutral'
                                    : 'danger'
                                }
                              >
                                {issue.issueType}
                              </Badge>
                            </td>
                            <td>{issue.message}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
                    All source records in this module are eligible for safe import without conflicts.
                  </div>
                )}
              </div>
            );
          })()}

          {/* Stage 3 & 4: Conflict Strategy & Administrator Confirmation */}
          <div className="import-confirmation-box">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, color: '#b45309' }}>
              <AlertTriangle size={20} />
              Pre-Execution Safety Verification Checklist
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <label style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-primary)' }}>
                Conflict Resolution Strategy:
              </label>
              <select
                className="filter-select"
                value={conflictStrategy}
                onChange={(e) => setConflictStrategy(e.target.value)}
                style={{ maxWidth: '400px' }}
                disabled={isExecuting}
              >
                <option value="SKIP_EXISTING">Skip existing database records and reuse DB IDs (Recommended)</option>
                <option value="FAIL_ON_CONFLICT">Abort execution on any unresolved conflict</option>
              </select>
            </div>

            <label className="import-checklist-item">
              <input
                type="checkbox"
                checked={confirmBackup}
                onChange={(e) => setConfirmBackup(e.target.checked)}
                disabled={isExecuting}
              />
              <span>
                <strong>PostgreSQL Backup Confirmed:</strong> I certify that a valid backup of the PostgreSQL database
                has been captured (or an explicit local development exception applies) prior to committing data.
              </span>
            </label>

            <label className="import-checklist-item">
              <input
                type="checkbox"
                checked={confirmExecution}
                onChange={(e) => setConfirmExecution(e.target.checked)}
                disabled={isExecuting}
              />
              <span>
                <strong>Explicit Import Authorization:</strong> I explicitly authorize the transactional import of eligible
                records into PostgreSQL. I understand browser localStorage keys will remain intact for audit safety.
              </span>
            </label>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleExecuteImport}
                disabled={!confirmBackup || !confirmExecution || isExecuting}
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.65rem 1.5rem', fontWeight: 600 }}
              >
                <CheckCircle2 size={18} />
                {isExecuting ? 'Executing Transactional Import...' : 'Execute Confirmed Import'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Execution Error Banner */}
      {executionError && (
        <div className="import-card" style={{ borderColor: 'var(--color-danger)', background: 'rgba(239, 68, 68, 0.05)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--color-danger)' }}>
            <AlertCircle size={22} />
            <div>
              <strong>Import Execution Encountered Errors</strong>
              <div style={{ fontSize: '0.85rem' }}>{executionError}</div>
            </div>
          </div>
        </div>
      )}

      {/* Stage 5 & 6: Execution Result & Audit Reconciliation */}
      {executionResult && (
        <div className="import-result-banner">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', color: 'var(--color-success)' }}>
            <CheckCircle2 size={28} />
            <div>
              <h3 style={{ margin: 0 }}>Import Completed ({executionResult.status})</h3>
              <div style={{ fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
                Job ID: <code>{executionResult.jobId}</code> · Executed by: <strong>{executionResult.executedBy}</strong> · Time: {executionResult.executedAt}
              </div>
            </div>
          </div>

          <div className="import-inventory-grid" style={{ marginBottom: '1rem' }}>
            <div className="import-inventory-stat">
              <span className="import-stat-label">Departments</span>
              <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                Imported: {executionResult.importedCounts?.departments ?? 0} · Skipped: {executionResult.skippedCounts?.departments ?? 0}
              </span>
            </div>
            <div className="import-inventory-stat">
              <span className="import-stat-label">Employees</span>
              <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                Imported: {executionResult.importedCounts?.employees ?? 0} · Skipped: {executionResult.skippedCounts?.employees ?? 0}
              </span>
            </div>
            <div className="import-inventory-stat">
              <span className="import-stat-label">Attendance</span>
              <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                Imported: {executionResult.importedCounts?.attendance ?? 0} · Skipped: {executionResult.skippedCounts?.attendance ?? 0}
              </span>
            </div>
            <div className="import-inventory-stat">
              <span className="import-stat-label">Leaves</span>
              <span style={{ fontSize: '1.25rem', fontWeight: 600 }}>
                Imported: {executionResult.importedCounts?.leaves ?? 0} · Skipped: {executionResult.skippedCounts?.leaves ?? 0}
              </span>
            </div>
          </div>

          {executionResult.auditLog && executionResult.auditLog.length > 0 && (
            <div>
              <div style={{ fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.5rem' }}>Audit Event Log:</div>
              <div className="import-audit-log-panel">
                {executionResult.auditLog.map((logMsg, i) => (
                  <div key={i} className="import-audit-item">{logMsg}</div>
                ))}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem', color: 'var(--color-text-secondary)' }}>
            <Info size={16} />
            <span>
              All original localStorage records (<code>workforcehub_employees</code>, <code>workforcehub_departments</code>, etc.) remain untouched and intact.
            </span>
          </div>
        </div>
      )}
    </div>
  );
}
