import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Play,
  MessageSquare,
  CheckCircle,
  FileText,
  MapPin,
  Paperclip,
  Wrench,
  History,
  Clock,
  Users,
  Shield,
  Send,
  Lock,
  Calendar,
  Calculator,
} from 'lucide-react';
import { complaintApi, categoryApi, vendorApi } from '../../api';
import { useAuth } from '../../hooks/useAuth';
import StatusBadge from '../../components/ui/StatusBadge';
import PriorityIndicator from '../../components/ui/PriorityIndicator';
import PriorityFormulaModal from '../../components/complaint/PriorityFormulaModal';
import SLACountdown from '../../components/ui/SLACountdown';
import StatusTimeline from '../../components/complaint/StatusTimeline';
import ImageGallery from '../../components/complaint/ImageGallery';
import DynamicFieldRenderer from '../../components/complaint/DynamicFieldRenderer';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import SearchableDropdown from '../../components/ui/SearchableDropdown';
import CategoryIcon from '../../components/ui/CategoryIcon';
import ErrorState from '../../components/ui/ErrorState';
import LoadingSkeleton from '../../components/ui/LoadingSkeleton';
import { formatDateTime, formatReportedDateTime, getCategoryColor } from '../../utils/helpers';
import { useToast } from '../../hooks/useToast';
import { useRealtime } from '../../context/RealtimeContext';
import { Complaint, ComplaintStatus, VendorContactStatus, Category, Vendor } from '../../types';

const VENDOR_STEPS: { key: VendorContactStatus; label: string }[] = [
  { key: 'CONTACTED', label: 'Contacted' },
  { key: 'CONFIRMED', label: 'Confirmed' },
  { key: 'ON_SITE', label: 'On-site' },
  { key: 'DONE', label: 'Done' },
];

const StaffComplaintDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { addToast } = useToast();

  const [complaint, setComplaint] = useState<Complaint | undefined>(undefined);
  const [categories, setCategories] = useState<Category[]>([]);
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const [showResolveDialog, setShowResolveDialog] = useState(false);
  const [showInfoDialog, setShowInfoDialog] = useState(false);
  const [showPriorityFormula, setShowPriorityFormula] = useState(false);
  const [resolveNotes, setResolveNotes] = useState('');
  const [infoQuestion, setInfoQuestion] = useState('');
  const [newStaffNote, setNewStaffNote] = useState('');
  const [vendorContactStatus, setVendorContactStatus] = useState<VendorContactStatus>('CONTACTED');
  const [selectedVendor, setSelectedVendor] = useState<string>('');
  const [vendorNotes, setVendorNotes] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

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
      if (incoming.vendorAssignment?.contactStatus) {
        setVendorContactStatus(incoming.vendorAssignment.contactStatus);
      }
      if (incoming.vendorAssignment?.vendorId) {
        setSelectedVendor(incoming.vendorAssignment.vendorId);
      }
      if (incoming.vendorAssignment?.notes) {
        setVendorNotes(incoming.vendorAssignment.notes);
      }
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
      setIsLivePulsing(true);
      setTimeout(() => setIsLivePulsing(false), 3500);
    });

    return unsubToken;
  }, [complaint?.token, subscribeComplaint]);

  const loadComplaint = useCallback(async () => {
    if (!id) return;
    setIsLoading(true);
    setIsError(false);
    try {
      const [compRes, catsRes, vendorsRes] = await Promise.allSettled([
        complaintApi.getById(id),
        categoryApi.getAll(),
        vendorApi.getVendors(),
      ]);

      if (compRes.status === 'fulfilled' && compRes.value?.id) {
        setComplaint(compRes.value);
        if (compRes.value.vendorAssignment?.contactStatus) {
          setVendorContactStatus(compRes.value.vendorAssignment.contactStatus);
        }
        if (compRes.value.vendorAssignment?.vendorId) {
          setSelectedVendor(compRes.value.vendorAssignment.vendorId);
        }
        if (compRes.value.vendorAssignment?.notes) {
          setVendorNotes(compRes.value.vendorAssignment.notes);
        }
      } else {
        setIsError(true);
      }

      if (catsRes.status === 'fulfilled' && catsRes.value.length > 0) setCategories(catsRes.value);
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
        color: 'var(--accent)',
        sensitive: false,
        baseSeverityWeight: 1,
        slaHours: 24,
        formSchema: [],
        requiredSkillTags: [],
      }
    : null;

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
          message={`Unable to load assigned complaint #${id} from the server. Please verify your connection.`}
          onRetry={loadComplaint}
        />
      </div>
    );
  }

  const catColor = getCategoryColor(complaint.categoryName);

  const handleStartProgress = async () => {
    if (!complaint) return;
    setIsProcessing(true);
    try {
      const updated = await complaintApi.updateStatus(complaint.id, {
        status: 'IN_PROGRESS',
        note: 'Work commenced by assigned staff',
      });
      setComplaint(updated);
      addToast({ type: 'info', title: 'Status Updated', message: `${complaint.token} is now marked In Progress.` });
    } catch {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: 'IN_PROGRESS' as ComplaintStatus,
        changedBy: user?.name || 'Staff Member',
        changedByRole: 'STAFF' as const,
        note: 'Work commenced by assigned staff',
        timestamp: new Date().toISOString(),
      };
      setComplaint((prev) =>
        prev
          ? {
              ...prev,
              status: 'IN_PROGRESS',
              statusHistory: [historyEntry, ...prev.statusHistory],
            }
          : prev
      );
      addToast({ type: 'info', title: 'Status Updated', message: `${complaint.token} is now marked In Progress.` });
    } finally {
      setIsProcessing(false);
    }
  };

  const handleResolve = async () => {
    if (!complaint) return;
    if (!resolveNotes.trim() || resolveNotes.trim().length < 8) {
      addToast({
        type: 'warning',
        title: 'Resolution Notes Required',
        message: 'Please provide at least 8 characters describing how the issue was fixed.',
      });
      return;
    }

    setIsProcessing(true);
    try {
      const updated = await complaintApi.updateStatus(complaint.id, {
        status: 'RESOLVED',
        note: `Resolved: ${resolveNotes.trim()}`,
      });
      setComplaint(updated);
      addToast({
        type: 'success',
        title: 'Complaint Resolved',
        message: `${complaint.token} marked as Resolved. Student notified.`,
      });
    } catch {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: 'RESOLVED' as ComplaintStatus,
        changedBy: user?.name || 'Staff Member',
        changedByRole: 'STAFF' as const,
        note: `Resolved: ${resolveNotes.trim()}`,
        timestamp: new Date().toISOString(),
      };

      setComplaint((prev) =>
        prev
          ? {
              ...prev,
              status: 'RESOLVED',
              resolvedAt: new Date().toISOString(),
              statusHistory: [historyEntry, ...prev.statusHistory],
            }
          : prev
      );

      addToast({
        type: 'success',
        title: 'Complaint Resolved',
        message: `${complaint.token} marked as Resolved. Student notified.`,
      });
    } finally {
      setIsProcessing(false);
      setShowResolveDialog(false);
      setResolveNotes('');
    }
  };

  const handleRequestInfo = async () => {
    if (!complaint || !infoQuestion.trim()) return;

    setIsProcessing(true);
    try {
      const updated = await complaintApi.requestInfo(complaint.id, {
        message: infoQuestion.trim(),
      });
      setComplaint(updated);
      addToast({
        type: 'info',
        title: 'Info Requested',
        message: 'Question sent to the student.',
      });
    } catch {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: 'AWAITING_STUDENT_INFO' as ComplaintStatus,
        changedBy: user?.name || 'Staff Member',
        changedByRole: 'STAFF' as const,
        note: `Information requested: "${infoQuestion.trim()}"`,
        timestamp: new Date().toISOString(),
      };

      setComplaint((prev) =>
        prev
          ? {
              ...prev,
              status: 'AWAITING_STUDENT_INFO',
              statusHistory: [historyEntry, ...prev.statusHistory],
            }
          : prev
      );

      addToast({
        type: 'info',
        title: 'Info Requested',
        message: 'Question sent to the student.',
      });
    } finally {
      setIsProcessing(false);
      setShowInfoDialog(false);
      setInfoQuestion('');
    }
  };

  const handleAddStaffNote = async () => {
    if (!complaint || !newStaffNote.trim()) return;

    try {
      const updated = await complaintApi.updateStatus(complaint.id, {
        status: complaint.status,
        note: newStaffNote.trim(),
      });
      setComplaint(updated);
    } catch {
      const historyEntry = {
        id: `sh-${Date.now()}`,
        status: complaint.status,
        changedBy: user?.name || 'Staff Member',
        changedByRole: 'STAFF' as const,
        note: newStaffNote.trim(),
        timestamp: new Date().toISOString(),
      };

      setComplaint((prev) =>
        prev ? { ...prev, statusHistory: [historyEntry, ...prev.statusHistory] } : prev
      );
    }
    setNewStaffNote('');
    addToast({ type: 'info', title: 'Work Note Logged', message: 'Update logged to case audit trail.' });
  };

  const vendorOptions = vendors.map((v) => ({
    value: v.id,
    label: v.name,
    sublabel: v.categoryTags.join(', '),
  }));

  const stepIndex = VENDOR_STEPS.findIndex((s) => s.key === vendorContactStatus);

  // SLA calculation
  const created = new Date(complaint.createdAt).getTime();
  const deadline = new Date(complaint.slaDeadline).getTime();
  const now = Date.now();
  const totalWindow = Math.max(1, deadline - created);
  const elapsed = Math.max(0, now - created);
  const slaPct = Math.min(100, Math.round((elapsed / totalWindow) * 100));

  return (
    <div className={`admin-complaint-detail-page ${isLivePulsing ? 'detail-live-pulse' : ''}`}>
      {/* 1. Header Navigation Bar */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 14,
          marginBottom: 16,
          flexWrap: 'wrap',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
          <button className="btn btn-secondary btn-sm" onClick={() => navigate('/staff/queue')}>
            <ArrowLeft size={14} />
            <span>My Queue</span>
          </button>

          <span
            style={{
              fontFamily: 'monospace',
              fontSize: '1.25rem',
              fontWeight: 800,
              color: 'var(--text-1)',
            }}
          >
            {complaint.token}
          </span>

          <span
            className="table-category-pill"
            style={{ borderColor: `${catColor}30`, color: catColor, background: `${catColor}12` }}
          >
            <CategoryIcon name={complaint.categoryName} size={13} />
            {complaint.categoryName}
          </span>

          <StatusBadge status={complaint.status} />
          {isLivePulsing && (
            <span className="live-badge live-badge-updated">UPDATED</span>
          )}
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          {complaint.status === 'ASSIGNED' && (
            <button className="btn btn-primary btn-sm" onClick={handleStartProgress} disabled={isProcessing}>
              <Play size={13} />
              <span>Start Work</span>
            </button>
          )}

          {complaint.status !== 'RESOLVED' && complaint.status !== 'CLOSED' && (
            <>
              <button className="btn btn-secondary btn-sm" onClick={() => setShowInfoDialog(true)}>
                <MessageSquare size={13} />
                <span>Request Student Info</span>
              </button>
              <button className="btn btn-success btn-sm" onClick={() => setShowResolveDialog(true)}>
                <CheckCircle size={13} />
                <span>Mark Resolved</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* 2. Incident Briefing Document Card (Placed ABOVE SLA, NO phone number) */}
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
            <span>Assigned Incident Briefing</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.74rem', color: 'var(--text-2)' }}>
            <Calendar size={13} style={{ color: 'var(--accent)' }} />
            <span>Reported on <strong style={{ color: 'var(--text-1)' }}>{formatReportedDateTime(complaint.createdAt)}</strong></span>
          </div>
        </div>

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

        {/* Student Profile Strip (NO PHONE NUMBER) */}
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
            <span>Submitted anonymously — student identity sealed by campus policy.</span>
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

        {/* Parameters & Venue */}
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

        {/* Attachments */}
        {complaint.attachments && complaint.attachments.length > 0 && (
          <div style={{ borderTop: '1px solid var(--border)', paddingTop: 14, marginTop: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 10 }}>
              <Paperclip size={13} />
              <span>Evidence Attachments ({complaint.attachments.length})</span>
            </div>
            <ImageGallery attachments={complaint.attachments} />
          </div>
        )}
      </div>

      {/* 3. SLA & Priority Fixloop Overview Banner */}
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
        <div style={{ borderRight: '1px solid var(--border)', paddingRight: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase' }}>
              <Shield size={13} style={{ color: 'var(--accent)' }} />
              <span>Priority Score</span>
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
          <PriorityIndicator score={complaint.priorityScore} showBar />
        </div>

        <div style={{ borderRight: '1px solid var(--border)', paddingRight: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase' }}>
              <Clock size={13} style={{ color: complaint.isBreached ? 'var(--danger)' : 'var(--accent)' }} />
              <span>SLA Target</span>
            </div>
            <span style={{ fontSize: '0.72rem', fontWeight: 700, color: complaint.isBreached ? 'var(--danger)' : 'var(--success)' }}>
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
                  background: complaint.isBreached ? 'var(--danger)' : 'var(--success)',
                  borderRadius: 999,
                }}
              />
            </div>
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--text-3)', textTransform: 'uppercase', marginBottom: 3 }}>
            Resolution Deadline
          </div>
          <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-1)' }}>
            {formatDateTime(complaint.slaDeadline)}
          </div>
        </div>
      </div>

      {/* 4. 2-Column Work Area */}
      <div className="detail-grid" style={{ gridTemplateColumns: '1fr 340px', gap: 20 }}>
        {/* LEFT COLUMN: Activity Log & Staff Notes (With Add Note at the TOP) */}
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
                <span>Work Log & Status History</span>
              </div>
              <span style={{ fontSize: '0.72rem', color: 'var(--text-3)' }}>
                {complaint.statusHistory.length} events logged
              </span>
            </div>

            {/* Staff Work Note Composer at the TOP */}
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
                <span>Add Work Progress Note</span>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  type="text"
                  className="filter-text-input"
                  style={{ flex: 1, padding: '9px 12px', fontSize: '0.82rem' }}
                  placeholder="Record parts ordered, inspection result, etc..."
                  value={newStaffNote}
                  onChange={(e) => setNewStaffNote(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleAddStaffNote();
                  }}
                />
                <button
                  className="btn btn-primary btn-sm"
                  onClick={handleAddStaffNote}
                  disabled={!newStaffNote.trim()}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '0 16px' }}
                >
                  <Send size={13} />
                  <span>Post</span>
                </button>
              </div>
            </div>

            <StatusTimeline history={complaint.statusHistory} />
          </div>
        </div>

        {/* RIGHT COLUMN: Vendor Coordination Rail */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div
            style={{
              background: 'var(--bg-surface)',
              border: '1px solid var(--border)',
              borderRadius: 16,
              boxShadow: 'var(--card-shadow)',
              padding: '18px 20px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: '0.84rem', fontWeight: 800, color: 'var(--text-1)', marginBottom: 14 }}>
              <Wrench size={16} style={{ color: 'var(--accent)' }} />
              <span>Vendor / External Contractor</span>
            </div>

            <div style={{ marginBottom: 12 }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', fontWeight: 600, marginBottom: 4 }}>
                Selected Vendor
              </div>
              <SearchableDropdown
                options={vendorOptions}
                value={selectedVendor}
                onChange={setSelectedVendor}
                placeholder="Assign contractor..."
              />
            </div>

            {/* Vendor Milestones */}
            <div style={{ marginBottom: 14 }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-3)', fontWeight: 600, marginBottom: 8 }}>
                Contractor Milestone
              </div>
              <div style={{ display: 'flex', gap: 4 }}>
                {VENDOR_STEPS.map((s, idx) => (
                  <button
                    key={s.key}
                    type="button"
                    className={`btn btn-sm ${stepIndex >= idx ? 'btn-primary' : 'btn-secondary'}`}
                    style={{ flex: 1, fontSize: '0.68rem', padding: '4px 2px', justifyContent: 'center' }}
                    onClick={() => setVendorContactStatus(s.key)}
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            <textarea
              className="filter-text-input"
              style={{ width: '100%', minHeight: 65, fontSize: '0.76rem', padding: '8px', marginBottom: 12, resize: 'vertical' }}
              placeholder="Contractor quotation, arrival ETA, or work notes..."
              value={vendorNotes}
              onChange={(e) => setVendorNotes(e.target.value)}
            />

            <button
              className="btn btn-secondary btn-sm"
              style={{ width: '100%', justifyContent: 'center' }}
              onClick={() => addToast({ type: 'success', title: 'Vendor Milestone Saved', message: 'Contractor progress updated.' })}
            >
              Update Contractor Status
            </button>
          </div>
        </div>
      </div>

      {/* Resolve Dialog */}
      <ConfirmDialog
        isOpen={showResolveDialog}
        title="Resolve Complaint"
        message={`Provide final resolution notes for ${complaint.token}:`}
        confirmLabel="Confirm Resolution"
        variant="default"
        isLoading={isProcessing}
        onConfirm={handleResolve}
        onCancel={() => setShowResolveDialog(false)}
      >
        <div className="form-group" style={{ marginTop: 12 }}>
          <textarea
            className="form-textarea"
            placeholder="Detailed description of what action was taken to fix the issue..."
            value={resolveNotes}
            onChange={(e) => setResolveNotes(e.target.value)}
            style={{ minHeight: 80 }}
          />
        </div>
      </ConfirmDialog>

      {/* Request Info Dialog */}
      <ConfirmDialog
        isOpen={showInfoDialog}
        title="Request Student Information"
        message={`What clarification do you require from the reporter of ${complaint.token}?`}
        confirmLabel="Send Request"
        variant="warning"
        isLoading={isProcessing}
        onConfirm={handleRequestInfo}
        onCancel={() => setShowInfoDialog(false)}
      >
        <div className="form-group" style={{ marginTop: 12 }}>
          <textarea
            className="form-textarea"
            placeholder="E.g., Which specific computer terminal is experiencing the issue? Have you tried restarting?"
            value={infoQuestion}
            onChange={(e) => setInfoQuestion(e.target.value)}
            style={{ minHeight: 80 }}
          />
        </div>
      </ConfirmDialog>

      {/* Priority Scoring Formula Breakdown Modal */}
      <PriorityFormulaModal
        complaint={complaint}
        isOpen={showPriorityFormula}
        onClose={() => setShowPriorityFormula(false)}
      />
    </div>
  );
};

export default StaffComplaintDetail;
