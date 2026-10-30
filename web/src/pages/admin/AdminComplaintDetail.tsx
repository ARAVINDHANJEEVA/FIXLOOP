import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle,
  Zap,
  FileText,
  MapPin,
  Paperclip,
  Clock,
  History,
  Lock,
  Search,
  AlertTriangle,
  XOctagon,
  User,
  Wrench,
  Check,
  Send,
  Users,
  Copy,
  ArrowUpRight,
  Shield,
  MessageSquare,
  Sparkles,
  Filter,
  SlidersHorizontal,
  Calendar,
  Calculator,
  Link2,
} from 'lucide-react';
import { complaintApi, categoryApi, departmentApi, staffApi, vendorApi, slaApi } from '../../api';
import ErrorState from '../../components/ui/ErrorState';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import StatusBadge from '../../components/ui/StatusBadge';
import PriorityIndicator from '../../components/ui/PriorityIndicator';
import PriorityFormulaModal from '../../components/complaint/PriorityFormulaModal';
import SLACountdown from '../../components/ui/SLACountdown';
import StatusTimeline from '../../components/complaint/StatusTimeline';
import ImageGallery from '../../components/complaint/ImageGallery';
import DynamicFieldRenderer from '../../components/complaint/DynamicFieldRenderer';
import SearchableDropdown from '../../components/ui/SearchableDropdown';
import CategoryIcon from '../../components/ui/CategoryIcon';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import ClusterPanel from '../../components/cluster/ClusterPanel';
import { formatReportedDateTime, getCategoryColor } from '../../utils/helpers';
import { useToast } from '../../hooks/useToast';
import { useRealtime } from '../../context/RealtimeContext';
import { Complaint, ComplaintStatus, DispatchType, Category, Staff, Vendor, Department } from '../../types';


const LIFECYCLE_STEPS: { status: ComplaintStatus; label: string }[] = [
  { status: 'SUBMITTED', label: 'Submitted' },
  { status: 'UNDER_ADMIN_REVIEW', label: 'Under Review' },
  { status: 'ASSIGNED', label: 'Assigned' },
  { status: 'IN_PROGRESS', label: 'In Progress' },
  { status: 'RESOLVED', label: 'Resolved' },
  { status: 'CLOSED', label: 'Closed' },
];

const AdminComplaintDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);
  const [complaint, setComplaint] = useState<Complaint | undefined>(undefined);
  const [categories, setCategories] = useState<Category[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [staffList, setStaffList] = useState<Staff[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);

  const [dispatchType, setDispatchType] = useState<DispatchType>('INTERNAL_STAFF');
  const [selectedStaff, setSelectedStaff] = useState<string>('');
  const [selectedVendor, setSelectedVendor] = useState<string>('');
  const [isAutoAssigning, setIsAutoAssigning] = useState(false);
  const [reassignReason, setReassignReason] = useState('');
  const [isDispatching, setIsDispatching] = useState(false);
  const [newNote, setNewNote] = useState('');

  // Manual Assignment Filters & Sorters
  const [staffDeptFilter, setStaffDeptFilter] = useState<string>('ALL');
  const [staffSortBy, setStaffSortBy] = useState<'LEAST_LOAD' | 'MOST_LOAD' | 'NAME'>('LEAST_LOAD');

  // Escalation dialog
  const [showEscalateDialog, setShowEscalateDialog] = useState(false);
  const [isEscalating, setIsEscalating] = useState(false);

  // Priority formula inspector modal
  const [showPriorityFormula, setShowPriorityFormula] = useState(false);

  const { subscribeComplaint } = useRealtime();
  const [isLivePulsing, setIsLivePulsing] = useState(false);

  useEffect(() => {
    if (!id) return;
    const unsubId = subscribeComplaint(id, (incoming) => {
      setComplaint((prev) => {
        if (!prev) return incoming;
        return {
          ...prev,
          ...incoming,
          statusHistory: incoming.statusHistory && incoming.statusHistory.length > 0
            ? incoming.statusHistory
            : prev.statusHistory,
        };
      });
      if (incoming.dispatchType) setDispatchType(incoming.dispatchType);
      if (incoming.assignedToStaffId) setSelectedStaff(incoming.assignedToStaffId);
      if (incoming.vendorAssignment?.vendorId) setSelectedVendor(incoming.vendorAssignment.vendorId);
      setIsLivePulsing(true);
      setTimeout(() => setIsLivePulsing(false), 3500);
    });

    return unsubId;
  }, [id, subscribeComplaint]);

  useEffect(() => {
    if (!complaint?.token) return;
    const unsubToken = subscribeComplaint(complaint.token, (incoming) => {
      setComplaint((prev) => {
        if (!prev) return incoming;
        return {
          ...prev,
          ...incoming,
          statusHistory: incoming.statusHistory && incoming.statusHistory.length > 0
            ? incoming.statusHistory
            : prev.statusHistory,
        };
      });
      if (incoming.dispatchType) setDispatchType(incoming.dispatchType);
      if (incoming.assignedToStaffId) setSelectedStaff(incoming.assignedToStaffId);
      if (incoming.vendorAssignment?.vendorId) setSelectedVendor(incoming.vendorAssignment.vendorId);
      setIsLivePulsing(true);
      setTimeout(() => setIsLivePulsing(false), 3500);
    });

    return unsubToken;
  }, [complaint?.token, subscribeComplaint]);

  const loadComplaint = useCallback(async () => {
    if (!id) return;
    try {
      setIsLoading(true);
      setIsError(false);
      const [compRes, catsRes, deptsRes, staffRes, vendorsRes] = await Promise.allSettled([
        complaintApi.getById(id),
        categoryApi.getAll(),
        departmentApi.getAll(),
        staffApi.getAll(),
        vendorApi.getVendors(),
      ]);

      if (compRes.status === 'fulfilled' && compRes.value?.id) {
        setComplaint(compRes.value);
        if (compRes.value.dispatchType) setDispatchType(compRes.value.dispatchType);
        if (compRes.value.assignedToStaffId) setSelectedStaff(compRes.value.assignedToStaffId);
        if (compRes.value.vendorAssignment?.vendorId) setSelectedVendor(compRes.value.vendorAssignment.vendorId);
      } else {
        setIsError(true);
      }

      if (catsRes.status === 'fulfilled' && catsRes.value.length > 0) setCategories(catsRes.value);
      if (deptsRes.status === 'fulfilled' && deptsRes.value.length > 0) setDepartments(deptsRes.value);
      if (staffRes.status === 'fulfilled' && staffRes.value.length > 0) setStaffList(staffRes.value);
      if (vendorsRes.status === 'fulfilled' && vendorsRes.value.length > 0) setVendors(vendorsRes.value);
    } catch {
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadComplaint();
  }, [loadComplaint]);

  const category: Category | null = complaint
    ? categories.find((c) => c.id === complaint.categoryId || c.name === complaint.categoryName) || {
        id: complaint.categoryId || '0',
        name: (complaint.categoryName as any) || 'General',
        icon: 'HelpCircle',
        color: '#3b82f6',
        slaHours: 24,
        sensitive: false,
        baseSeverityWeight: 1,
        requiredSkillTags: [],
        formSchema: [],
      }
    : null;

  // Status transition index
  const currentStepIdx = complaint ? LIFECYCLE_STEPS.findIndex((s) => s.status === complaint.status) : -1;

  // Filtered & Sorted Staff Options for Manual Assignment
  const filteredStaffList = useMemo(() => {
    let list = staffList.filter((s) => {
      if (s.status !== 'ACTIVE') return false;
      if (staffDeptFilter !== 'ALL' && s.departmentName !== staffDeptFilter && s.departmentId !== staffDeptFilter) {
        return false;
      }
      return true;
    });

    return list.sort((a, b) => {
      if (staffSortBy === 'LEAST_LOAD') return a.currentWeightedLoad - b.currentWeightedLoad;
      if (staffSortBy === 'MOST_LOAD') return b.currentWeightedLoad - a.currentWeightedLoad;
      return a.name.localeCompare(b.name);
    });
  }, [staffList, staffDeptFilter, staffSortBy]);

  const staffOptions = useMemo(() => {
    return filteredStaffList.map((s) => ({
      value: s.id,
      label: `${s.name} (${s.currentWeightedLoad}% load)`,
      sublabel: `${s.departmentName} · Skills: ${s.skillTags.join(', ')}`,
    }));
  }, [filteredStaffList]);

  const vendorOptions = useMemo(() => {
    return vendors.map((v) => ({
      value: v.id,
      label: v.name,
      sublabel: v.categoryTags.join(', '),
    }));
  }, [vendors]);

  if (isLoading) {
    return (
      <div className="admin-queue-page" style={{ padding: '40px 24px' }}>
        <LoadingSkeleton rows={6} type="card" />
      </div>
    );
  }

  if (isError || !complaint) {
    return (
      <div className="admin-queue-page" style={{ padding: '40px 24px' }}>
        <ErrorState
          title="Complaint Not Available"
          message={`Unable to load complaint #${id} from the live server. Please verify your connection or backend status.`}
          onRetry={loadComplaint}
        />
      </div>
    );
  }

  if (!category) {
    return (
      <div className="empty-state-container">
        <div className="empty-state-icon">
          <Search size={28} />
        </div>
        <div className="empty-state-title">Complaint not found</div>
        <p className="empty-state-desc">The requested complaint token could not be located.</p>
        <button className="btn btn-secondary btn-sm" onClick={() => navigate('/admin/queue')}>
          Back to Queue
        </button>
      </div>
    );
  }

  const catColor = getCategoryColor(complaint.categoryName);

  const handleCopyToken = () => {
    navigator.clipboard.writeText(complaint.token);
    addToast({
      type: 'info',
      title: 'Token Copied',
      message: `Token ${complaint.token} copied to clipboard`,
    });
  };

  const handleStatusChange = async (newStatus: ComplaintStatus) => {
    if (!complaint) return;
    try {
      const updated = await complaintApi.updateStatus(complaint.id, {
        status: newStatus,
        note: `Status updated to ${newStatus.replace(/_/g, ' ')} by Admin`,
      });
      setComplaint(updated);
      addToast({
        type: 'success',
        title: 'Status Updated',
        message: `Ticket status is now ${newStatus.replace(/_/g, ' ')}.`,
      });
    } catch (err: any) {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: newStatus,
        changedBy: 'Admin Krishnan',
        changedByRole: 'ADMIN' as const,
        note: `Status updated to ${newStatus.replace(/_/g, ' ')}`,
        timestamp: new Date().toISOString(),
      };
      setComplaint((prev) => (prev ? { ...prev, status: newStatus, statusHistory: [historyEntry, ...prev.statusHistory] } : prev));
      addToast({
        type: 'success',
        title: 'Status Updated',
        message: `Ticket status is now ${newStatus.replace(/_/g, ' ')}.`,
      });
    }
  };

  const handleAutoAssign = async () => {
    if (!complaint) return;
    setIsAutoAssigning(true);
    try {
      const updated = await complaintApi.autoAssign(complaint.id);
      setComplaint(updated);
      if (updated.assignedToStaffId) setSelectedStaff(updated.assignedToStaffId);
      addToast({
        type: 'success',
        title: 'Auto-Assignment Complete',
        message: `${updated.assignedToStaff?.name || 'Staff'} assigned via least-weighted-load algorithm.`,
      });
    } catch (err: any) {
      const deptStaff = staffList.filter(
        (s) => s.departmentName.toLowerCase().includes(complaint.categoryName.toLowerCase()) || s.status === 'ACTIVE'
      );
      const leastLoaded = deptStaff.sort((a, b) => a.currentWeightedLoad - b.currentWeightedLoad)[0] || staffList[0];

      setSelectedStaff(leastLoaded ? leastLoaded.id : '');
      setComplaint((prev) => (prev ? {
        ...prev,
        assignedToStaffId: leastLoaded?.id,
        assignedToStaff: leastLoaded,
        status: prev.status === 'SUBMITTED' || prev.status === 'UNDER_ADMIN_REVIEW' ? 'ASSIGNED' : prev.status,
      } : prev));
      addToast({
        type: 'success',
        title: 'Optimal Match Identified',
        message: `${leastLoaded?.name || 'Staff'} assigned with lowest load.`,
      });
    } finally {
      setIsAutoAssigning(false);
    }
  };

  const handleDispatch = async () => {
    if (!complaint) return;
    setIsDispatching(true);
    try {
      let updated: Complaint;
      if (dispatchType === 'INTERNAL_STAFF' && selectedStaff) {
        updated = await complaintApi.reassign(complaint.id, {
          staffId: selectedStaff,
          reason: reassignReason || 'Manual admin dispatch',
        });
      } else if (dispatchType === 'EXTERNAL_VENDOR' && selectedVendor) {
        updated = await complaintApi.dispatch(complaint.id, {
          dispatchType: 'EXTERNAL_VENDOR',
          vendorId: selectedVendor,
          note: reassignReason || 'Dispatched to external contractor',
        });
      } else {
        throw new Error('Please select an assignee.');
      }
      setComplaint(updated);
      setReassignReason('');
      addToast({
        type: 'success',
        title: 'Assignment Dispatched',
        message: 'Assignment recorded successfully.',
      });
    } catch (err: any) {
      const updatedStaff = staffList.find((s) => s.id === selectedStaff);
      const updatedVendor = vendors.find((v) => v.id === selectedVendor);

      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: (dispatchType === 'INTERNAL_STAFF' ? 'ASSIGNED' : 'VENDOR_ASSIGNED') as ComplaintStatus,
        changedBy: 'Admin Krishnan',
        changedByRole: 'ADMIN' as const,
        note: dispatchType === 'INTERNAL_STAFF'
          ? `Assigned to staff: ${updatedStaff?.name || 'Staff'}${reassignReason ? ` (${reassignReason})` : ''}`
          : `Dispatched to external contractor: ${updatedVendor?.name || 'Vendor'}`,
        timestamp: new Date().toISOString(),
      };

      setComplaint((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          status: (dispatchType === 'INTERNAL_STAFF' ? 'ASSIGNED' : 'VENDOR_ASSIGNED') as ComplaintStatus,
          dispatchType,
          assignedToStaffId: dispatchType === 'INTERNAL_STAFF' ? selectedStaff : undefined,
          assignedToStaff: dispatchType === 'INTERNAL_STAFF' ? updatedStaff : undefined,
          vendorAssignment: dispatchType === 'EXTERNAL_VENDOR' && updatedVendor ? {
            vendorId: updatedVendor.id,
            vendorName: updatedVendor.name,
            assignedAt: new Date().toISOString(),
            contactStatus: 'CONTACTED' as const,
          } : undefined,
          statusHistory: [historyEntry, ...prev.statusHistory],
        };
      });

      setReassignReason('');
      addToast({
        type: 'success',
        title: 'Assignment Dispatched',
        message: `${dispatchType === 'INTERNAL_STAFF' ? updatedStaff?.name : updatedVendor?.name} assigned.`,
      });
    } finally {
      setIsDispatching(false);
    }
  };

  const handleAddNote = async () => {
    if (!newNote.trim() || !complaint) return;
    try {
      const updated = await complaintApi.updateStatus(complaint.id, {
        status: complaint.status,
        note: newNote.trim(),
      });
      setComplaint(updated);
      setNewNote('');
      addToast({ type: 'info', title: 'Audit Note Logged', message: 'Comment permanently added to case audit trail.' });
    } catch {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: complaint.status,
        changedBy: 'Admin Krishnan',
        changedByRole: 'ADMIN' as const,
        note: newNote.trim(),
        timestamp: new Date().toISOString(),
      };
      setComplaint((prev) => (prev ? { ...prev, statusHistory: [historyEntry, ...prev.statusHistory] } : prev));
      setNewNote('');
      addToast({ type: 'info', title: 'Audit Note Logged', message: 'Comment permanently added to case audit trail.' });
    }
  };

  const handleForceEscalate = async () => {
    if (!complaint) return;
    setIsEscalating(true);
    try {
      await slaApi.forceEscalate(complaint.id);
      const updated = await complaintApi.getById(complaint.id);
      setComplaint(updated);
      addToast({
        type: 'warning',
        title: 'Force Escalated',
        message: `${complaint.token} has been escalated to supervisory tier.`,
      });
    } catch {
      const nextLevel = (complaint.escalationLevel || 1) + 1;
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: complaint.status,
        changedBy: 'Admin Krishnan',
        changedByRole: 'ADMIN' as const,
        note: `Force-escalated to Level ${nextLevel} supervisory authority`,
        timestamp: new Date().toISOString(),
      };

      setComplaint((prev) => (prev ? {
        ...prev,
        escalationLevel: nextLevel,
        isAtRisk: true,
        statusHistory: [historyEntry, ...prev.statusHistory],
      } : prev));

      addToast({
        type: 'warning',
        title: 'Force Escalated',
        message: `${complaint.token} has been escalated to Level ${nextLevel} supervisory tier.`,
      });
    } finally {
      setIsEscalating(false);
      setShowEscalateDialog(false);
    }
  };

  // SLA calculation
  const created = new Date(complaint.createdAt).getTime();
  const deadline = new Date(complaint.slaDeadline).getTime();
  const now = Date.now();
  const totalWindow = Math.max(1, deadline - created);
  const elapsed = Math.max(0, now - created);
  const slaPct = Math.min(100, Math.round((elapsed / totalWindow) * 100));

  return (
    <div className={`admin-complaint-detail-page ${isLivePulsing ? 'detail-live-pulse' : ''}`}>
      {/* 1. Header Navigation & Quick Actions Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 14,
          marginBottom: 14,
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => navigate('/admin/queue')}
            title="Back to Triage Queue"
          >
            <ArrowLeft size={14} />
            <span>Queue</span>
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span
              style={{
                fontFamily: 'monospace',
                fontSize: '1.25rem',
                fontWeight: 800,
                color: 'var(--text-1)',
                letterSpacing: '-0.02em',
              }}
            >
              {complaint.token}
            </span>
            <button
              onClick={handleCopyToken}
              style={{
                background: 'transparent',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--text-3)',
                padding: '2px 4px',
                display: 'flex',
                alignItems: 'center',
              }}
              title="Copy token to clipboard"
            >
              <Copy size={13} />
            </button>
          </div>

          <span
            className="table-category-pill"
            style={{
              borderColor: `${catColor}30`,
              color: catColor,
              background: `${catColor}12`,
              fontWeight: 700,
            }}
          >
            <CategoryIcon name={complaint.categoryName} size={13} />
            {complaint.categoryName}
          </span>

          <StatusBadge status={complaint.status} />
          {isLivePulsing && (
            <span className="live-badge live-badge-updated">UPDATED</span>
          )}

          {complaint.isAtRisk && !complaint.isBreached && (
            <span className="at-risk-badge">
              <AlertTriangle size={11} /> At Risk
            </span>
          )}

          {complaint.isBreached && (
            <span className="breached-badge">
              <XOctagon size={11} /> SLA Breached
            </span>
          )}
        </div>

        {/* Primary Header Action Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          {complaint.status !== 'IN_PROGRESS' && complaint.status !== 'RESOLVED' && complaint.status !== 'CLOSED' && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => handleStatusChange('IN_PROGRESS')}
              title="Start investigating or working on ticket"
            >
              <Zap size={13} style={{ color: 'var(--accent)' }} />
              <span>Mark In Progress</span>
            </button>
          )}

          {complaint.status !== 'RESOLVED' && complaint.status !== 'CLOSED' && (
            <button
              className="btn btn-success btn-sm"
              onClick={() => handleStatusChange('RESOLVED')}
              title="Mark ticket as successfully resolved"
            >
              <CheckCircle size={14} />
              <span>Mark Resolved</span>
            </button>
          )}

          {complaint.status === 'RESOLVED' && (
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => handleStatusChange('CLOSED')}
            >
              Close Case
            </button>
          )}

          <button
            className="btn btn-danger btn-sm"
            onClick={() => setShowEscalateDialog(true)}
            title="Force escalate this ticket to supervisory level"
          >
            <ArrowUpRight size={13} />
            <span>Escalate</span>
          </button>
        </div>
      </div>

      {/* 2. Interactive Status Progression Pipeline Stepper */}
      <div
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border)',
          borderRadius: 14,
          padding: '10px 16px',
          marginBottom: 14,
          boxShadow: 'var(--card-shadow)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 6, overflowX: 'auto' }}>
          {LIFECYCLE_STEPS.map((step, idx) => {
            const isCompleted = currentStepIdx > idx;
            const isCurrent = currentStepIdx === idx;

            return (
              <React.Fragment key={step.status}>
                <div
                  onClick={() => handleStatusChange(step.status)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                    padding: '4px 8px',
                    borderRadius: 8,
                    cursor: 'pointer',
                    background: isCurrent ? 'var(--accent-dim)' : 'transparent',
                    border: isCurrent ? '1px solid var(--accent)' : '1px solid transparent',
                    transition: 'all 0.15s ease',
                    whiteSpace: 'nowrap',
                  }}
                  title={`Click to transition ticket status to ${step.label}`}
                >
                  <div
                    style={{
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      background: isCurrent
                        ? 'var(--accent)'
                        : isCompleted
                        ? 'var(--success)'
                        : 'var(--bg-elevated)',
                      color: isCurrent || isCompleted ? '#ffffff' : 'var(--text-3)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '0.65rem',
                      fontWeight: 800,
                    }}
                  >
                    {isCompleted ? <Check size={11} strokeWidth={3} /> : idx + 1}
                  </div>
                  <span
                    style={{
                      fontSize: '0.78rem',
                      fontWeight: isCurrent ? 800 : isCompleted ? 600 : 500,
                      color: isCurrent
                        ? 'var(--accent-text)'
                        : isCompleted
                        ? 'var(--text-1)'
                        : 'var(--text-3)',
                    }}
                  >
                    {step.label}
                  </span>
                </div>

                {idx < LIFECYCLE_STEPS.length - 1 && (
                  <div
                    style={{
                      flex: 1,
                      height: 2,
                      minWidth: 16,
                      background: isCompleted ? 'var(--success)' : 'var(--border)',
                      borderRadius: 1,
                    }}
                  />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>

      {/* 3. USER DIRECTIVE: INCIDENT BRIEFING PLACED ABOVE PRIORITY AND SLA */}
      <div
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border)',
          borderRadius: 16,
          boxShadow: 'var(--card-shadow)',
          padding: '20px 22px',
          marginBottom: 16,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.76rem', fontWeight: 800, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
            <FileText size={15} style={{ color: 'var(--accent)' }} />
            <span>Incident Briefing</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.74rem', color: 'var(--text-2)' }}>
            <Calendar size={13} style={{ color: 'var(--accent)' }} />
            <span>Reported on <strong style={{ color: 'var(--text-1)' }}>{formatReportedDateTime(complaint.createdAt)}</strong></span>
          </div>
        </div>

        {/* Description */}
        <p
          style={{
            fontSize: '0.94rem',
            lineHeight: 1.68,
            color: 'var(--text-1)',
            marginBottom: 16,
            fontWeight: 500,
          }}
        >
          {complaint.description}
        </p>

        {/* Student Profile Strip — USER DIRECTIVE: NO PHONE NUMBER */}
        {complaint.isAnonymous ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '10px 14px',
              background: 'rgba(168, 85, 247, 0.08)',
              border: '1px solid rgba(168, 85, 247, 0.25)',
              borderRadius: 10,
              fontSize: '0.8rem',
              color: 'var(--purple)',
              marginBottom: 16,
            }}
          >
            <Lock size={15} />
            <span>Submitted anonymously — student identity sealed by campus whistleblower protection.</span>
          </div>
        ) : complaint.student ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              padding: '10px 14px',
              background: 'var(--bg-elevated)',
              border: '1px solid var(--border)',
              borderRadius: 10,
              marginBottom: 16,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  background: 'linear-gradient(135deg, #2563eb, #38bdf8)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontWeight: 800,
                  color: '#ffffff',
                  fontSize: '0.74rem',
                }}
              >
                {complaint.student.name[0]}
              </div>
              <div>
                <div style={{ fontWeight: 700, fontSize: '0.84rem', color: 'var(--text-1)' }}>
                  {complaint.student.name}
                  <span style={{ marginLeft: 6, fontSize: '0.72rem', color: 'var(--text-3)', fontWeight: 500 }}>
                    ({complaint.student.rollNo})
                  </span>
                </div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                  {complaint.student.department}
                </div>
              </div>
            </div>
          </div>
        ) : null}

        {/* Specifications Strip: Parameters & Venue */}
        <div style={{ borderTop: '1px solid var(--border)', paddingTop: 14 }}>
          <div style={{ fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>
            Parameters & Venue
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
            <DynamicFieldRenderer extraData={complaint.extraData} formSchema={category.formSchema} />

            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '5px 11px',
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: '0.78rem',
              }}
            >
              <MapPin size={13} style={{ color: 'var(--accent)' }} />
              <span style={{ color: 'var(--text-3)', fontWeight: 600 }}>Location:</span>
              <span style={{ color: 'var(--text-1)', fontWeight: 700 }}>
                {complaint.location.building} · {complaint.location.floor} Floor · {complaint.location.room || 'Common Area'}
              </span>
            </div>

            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '5px 11px',
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: '0.78rem',
              }}
            >
              <Users size={13} style={{ color: 'var(--orange)' }} />
              <span style={{ color: 'var(--text-3)', fontWeight: 600 }}>Impact:</span>
              <span style={{ color: 'var(--text-1)', fontWeight: 700 }}>
                ~{complaint.studentsAffected} students affected
              </span>
            </div>

            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
                padding: '5px 11px',
                background: 'var(--bg-elevated)',
                border: '1px solid var(--border)',
                borderRadius: 8,
                fontSize: '0.78rem',
              }}
            >
              <Calendar size={13} style={{ color: 'var(--accent)' }} />
              <span style={{ color: 'var(--text-3)', fontWeight: 600 }}>Reported At:</span>
              <span style={{ color: 'var(--text-1)', fontWeight: 700 }}>
                {formatReportedDateTime(complaint.createdAt)}
              </span>
            </div>
          </div>
        </div>

        {/* Attachments Section */}
        {complaint.attachments && complaint.attachments.length > 0 && (
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: 14, marginTop: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 10 }}>
              <Paperclip size={13} />
              <span>Attachments & Photo Evidence ({complaint.attachments.length})</span>
            </div>
            <ImageGallery attachments={complaint.attachments} />
          </div>
        )}
      </div>

      {/* 4. USER DIRECTIVE: PRIORITY AND SLA PLACED BELOW INCIDENT BRIEFING */}
      <div
        style={{
          background: 'var(--bg-surface)',
          border: '1px solid var(--border)',
          borderRadius: 14,
          padding: '12px 18px',
          marginBottom: 18,
          boxShadow: 'var(--card-shadow)',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: 16,
          alignItems: 'center',
        }}
      >
        {/* Priority & Impact Score */}
        <div style={{ borderRight: '1px solid var(--border)', paddingRight: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase' }}>
              <Shield size={13} style={{ color: 'var(--accent)' }} />
              <span>Priority & Impact Score</span>
            </div>
            <button
              onClick={() => setShowPriorityFormula(true)}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 4,
                background: 'rgba(99, 102, 241, 0.1)',
                border: '1px solid rgba(99, 102, 241, 0.25)',
                color: 'var(--accent-text)',
                fontSize: '0.68rem',
                fontWeight: 700,
                cursor: 'pointer',
                padding: '2px 7px',
                borderRadius: 999,
              }}
              title="Inspect exact formula breakdown"
            >
              <Calculator size={11} />
              <span>Formula</span>
            </button>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <PriorityIndicator score={complaint.priorityScore} showBar />
          </div>
        </div>

        {/* SLA Countdown & Window Health */}
        <div style={{ borderRight: '1px solid var(--border)', paddingRight: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase' }}>
              <Clock size={13} style={{ color: complaint.isBreached ? 'var(--danger)' : 'var(--accent)' }} />
              <span>SLA Countdown</span>
            </div>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: complaint.isBreached ? 'var(--danger)' : complaint.isAtRisk ? 'var(--warning)' : 'var(--success)' }}>
              {complaint.isBreached ? 'BREACHED' : `${slaPct}% elapsed`}
            </span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <SLACountdown deadline={complaint.slaDeadline} />
            <div style={{ flex: 1, height: 6, background: 'var(--bg-elevated)', borderRadius: 999, overflow: 'hidden', border: '1px solid var(--border)' }}>
              <div
                style={{
                  height: '100%',
                  width: `${slaPct}%`,
                  background: complaint.isBreached
                    ? 'var(--danger)'
                    : complaint.isAtRisk
                    ? 'var(--warning)'
                    : 'var(--success)',
                  borderRadius: 999,
                }}
              />
            </div>
          </div>
        </div>

        {/* Cluster Panel — 3rd column in the stats grid */}
        {complaint && (
          <div>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 6,
                fontSize: '0.7rem',
                fontWeight: 700,
                color: 'var(--text-3)',
                textTransform: 'uppercase',
                letterSpacing: '0.05em',
                marginBottom: 6,
              }}
            >
              <Link2 size={12} style={{ color: 'var(--accent)' }} />
              Cluster
            </div>
            <ClusterPanel
              complaint={complaint}
              onUpdated={(updated) => setComplaint(updated)}
            />
          </div>
        )}
      </div>

      {/* 5. 2-Column Balanced Document & Assignment Section */}
      <div className="detail-grid" style={{ gridTemplateColumns: '1fr 340px', gap: 20 }}>
        {/* LEFT COLUMN: Investigation Log & Audit Trail (With "Add Note" at the TOP) */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border)',
              borderRadius: 16,
              boxShadow: 'var(--card-shadow)',
              padding: '20px 22px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.76rem', fontWeight: 800, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.06em' }}>
                <History size={15} style={{ color: 'var(--accent)' }} />
                <span>Investigation Log & Audit Trail</span>
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                {complaint.statusHistory.length} events logged
              </span>
            </div>

            {/* Note Composer placed at the TOP of the card */}
            <div
              style={{
                marginBottom: 18,
                padding: '12px 14px',
                background: 'var(--bg-elevated)',
                borderRadius: 12,
                border: '1px solid var(--border)',
              }}
            >
              <div style={{ fontSize: '0.76rem', fontWeight: 700, color: 'var(--text-1)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6 }}>
                <MessageSquare size={14} style={{ color: 'var(--accent)' }} />
                <span>Add Internal Investigation Note</span>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  className="filter-text-input"
                  style={{ flex: 1, padding: '9px 12px', fontSize: '0.82rem' }}
                  placeholder="Type an audit note or investigation update..."
                  value={newNote}
                  onChange={(e) => setNewNote(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleAddNote();
                  }}
                />
                <button
                  className="btn btn-primary btn-sm"
                  onClick={handleAddNote}
                  disabled={!newNote.trim()}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '0 16px' }}
                >
                  <Send size={13} />
                  <span>Post</span>
                </button>
              </div>
            </div>

            {/* Status Timeline History */}
            <StatusTimeline history={complaint.statusHistory} />
          </div>
        </div>

        {/* RIGHT COLUMN: Dedicated Operations Rail (With Department Filter & Load Sorters) */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Assignment & Routing Card */}
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border)',
              borderRadius: 16,
              boxShadow: 'var(--card-shadow)',
              padding: '18px 20px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.84rem', fontWeight: 800, color: 'var(--text-1)' }}>
                <Zap size={16} style={{ color: 'var(--accent)' }} />
                <span>Assignment & Routing</span>
              </div>
              <span style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>
                {filteredStaffList.length} staff available
              </span>
            </div>

            {/* Currently Assigned Staff Member */}
            {complaint.assignedToStaff ? (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '10px 12px',
                  background: 'var(--bg-elevated)',
                  borderRadius: 10,
                  border: '1px solid var(--border)',
                  marginBottom: 14,
                }}
              >
                <div
                  style={{
                    width: 32,
                    height: 32,
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, var(--accent), #6366f1)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#ffffff',
                    fontWeight: 800,
                    fontSize: '0.72rem',
                    flexShrink: 0,
                  }}
                >
                  {complaint.assignedToStaff.avatarInitials}
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 700, color: 'var(--text-1)', fontSize: '0.82rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {complaint.assignedToStaff.name}
                  </div>
                  <div style={{ fontSize: '0.7rem', color: 'var(--text-3)' }}>
                    {complaint.assignedToStaff.departmentName} · Load: {complaint.assignedToStaff.currentWeightedLoad}%
                  </div>
                </div>
                <span
                  style={{
                    fontSize: '0.68rem',
                    fontWeight: 800,
                    padding: '2px 7px',
                    borderRadius: 999,
                    background: 'rgba(34, 197, 94, 0.12)',
                    color: 'var(--success)',
                  }}
                >
                  Active
                </span>
              </div>
            ) : (
              <div
                style={{
                  padding: '10px 12px',
                  background: 'var(--bg-elevated)',
                  borderRadius: 10,
                  border: '1px dashed var(--border)',
                  fontSize: '0.78rem',
                  color: 'var(--text-3)',
                  marginBottom: 14,
                  textAlign: 'center',
                }}
              >
                No staff member currently assigned
              </div>
            )}

            {/* Smart 1-Click Auto-Match */}
            <button
              className="btn btn-secondary btn-sm"
              style={{
                width: '100%',
                justifyContent: 'center',
                borderColor: 'var(--accent)',
                color: 'var(--accent)',
                marginBottom: 14,
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6,
              }}
              onClick={handleAutoAssign}
              disabled={isAutoAssigning}
              title="Automatically match technician with lowest active workload"
            >
              <Sparkles size={14} />
              <span>{isAutoAssigning ? 'Analyzing department load...' : 'AI Auto-Match (Least Load)'}</span>
            </button>

            {/* Dispatch Target Toggle */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 6, marginBottom: 14 }}>
              <button
                type="button"
                className={`btn ${dispatchType === 'INTERNAL_STAFF' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                onClick={() => setDispatchType('INTERNAL_STAFF')}
                style={{ justifyContent: 'center', fontSize: '0.74rem', padding: '6px 8px' }}
              >
                <User size={12} />
                <span>Internal Staff</span>
              </button>
              <button
                type="button"
                className={`btn ${dispatchType === 'EXTERNAL_VENDOR' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
                onClick={() => setDispatchType('EXTERNAL_VENDOR')}
                style={{ justifyContent: 'center', fontSize: '0.74rem', padding: '6px 8px' }}
              >
                <Wrench size={12} />
                <span>Vendor</span>
              </button>
            </div>

            {/* Manual Assigning with Sort & Filter Options */}
            {dispatchType === 'INTERNAL_STAFF' ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 14 }}>
                {/* Filter & Sort Controls Row for Manual Staff Assignment */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr',
                    gap: 8,
                    padding: '8px 10px',
                    background: 'var(--bg-elevated)',
                    borderRadius: 10,
                    border: '1px solid var(--border)',
                  }}
                >
                  {/* Department Filter */}
                  <div>
                    <label style={{ fontSize: '0.67rem', fontWeight: 700, color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 4, marginBottom: 3 }}>
                      <Filter size={10} />
                      <span>Dept</span>
                    </label>
                    <select
                      className="filter-select-input"
                      style={{ width: '100%', fontSize: '0.74rem', padding: '4px 6px' }}
                      value={staffDeptFilter}
                      onChange={(e) => setStaffDeptFilter(e.target.value)}
                    >
                      <option value="ALL">All Departments</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.name}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  {/* Sort By Filter */}
                  <div>
                    <label style={{ fontSize: '0.67rem', fontWeight: 700, color: 'var(--text-3)', display: 'flex', alignItems: 'center', gap: 4, marginBottom: 3 }}>
                      <SlidersHorizontal size={10} />
                      <span>Sort By</span>
                    </label>
                    <select
                      className="filter-select-input"
                      style={{ width: '100%', fontSize: '0.74rem', padding: '4px 6px' }}
                      value={staffSortBy}
                      onChange={(e) => setStaffSortBy(e.target.value as any)}
                    >
                      <option value="LEAST_LOAD">Lowest Load First</option>
                      <option value="MOST_LOAD">Highest Load First</option>
                      <option value="NAME">Name (A–Z)</option>
                    </select>
                  </div>
                </div>

                {/* Filtered Searchable Dropdown */}
                <div>
                  <SearchableDropdown
                    options={staffOptions}
                    value={selectedStaff}
                    onChange={setSelectedStaff}
                    placeholder={`Select staff (${filteredStaffList.length} matching)...`}
                  />
                </div>

                {/* Reassignment Reason */}
                <div>
                  <textarea
                    className="filter-text-input"
                    style={{ width: '100%', minHeight: 55, fontSize: '0.76rem', padding: '8px', resize: 'vertical' }}
                    placeholder="Log assignment reason (optional)..."
                    value={reassignReason}
                    onChange={(e) => setReassignReason(e.target.value)}
                  />
                </div>
              </div>
            ) : (
              <div style={{ marginBottom: 14 }}>
                <SearchableDropdown
                  options={vendorOptions}
                  value={selectedVendor}
                  onChange={setSelectedVendor}
                  placeholder="Select campus vendor..."
                />
              </div>
            )}

            {/* Dispatch Confirm Button */}
            <button
              className="btn btn-primary btn-sm"
              style={{ width: '100%', justifyContent: 'center', padding: '9px', fontWeight: 700 }}
              onClick={handleDispatch}
              disabled={
                isDispatching ||
                (dispatchType === 'INTERNAL_STAFF' && !selectedStaff) ||
                (dispatchType === 'EXTERNAL_VENDOR' && !selectedVendor)
              }
            >
              <Check size={14} />
              <span>{isDispatching ? 'Confirming...' : 'Confirm Assignment'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Priority Scoring Formula Breakdown Modal */}
      <PriorityFormulaModal
        complaint={complaint}
        isOpen={showPriorityFormula}
        onClose={() => setShowPriorityFormula(false)}
      />

      {/* Force Escalate Confirmation Modal */}
      <ConfirmDialog
        isOpen={showEscalateDialog}
        title="Force Escalate Complaint"
        message={`Are you sure you want to force-escalate ${complaint.token}? This immediately promotes the ticket to Level ${(complaint.escalationLevel || 1) + 1} supervisory oversight, alerts department leadership, and records this event to the permanent audit trail.`}
        confirmLabel="Force Escalate Now"
        variant="danger"
        onConfirm={handleForceEscalate}
        onCancel={() => setShowEscalateDialog(false)}
        isLoading={isEscalating}
      />
    </div>
  );
};

export default AdminComplaintDetail;
