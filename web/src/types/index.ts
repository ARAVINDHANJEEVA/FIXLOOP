// ============================================================
// FIXLOOP — Type Definitions
// ============================================================

export type Role = 'ADMIN' | 'STAFF';

export type ComplaintStatus =
  | 'SUBMITTED'
  | 'UNDER_ADMIN_REVIEW'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'AWAITING_STUDENT_INFO'
  | 'VENDOR_ASSIGNED'
  | 'RESOLVED'
  | 'REOPENED'
  | 'CLOSED';

export type DispatchType = 'INTERNAL_STAFF' | 'EXTERNAL_VENDOR';

export type PriorityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type CategoryName =
  | 'Hostel'
  | 'Food'
  | 'Academic'
  | 'Health'
  | 'Hygiene'
  | 'Tech Support'
  | 'Harassment'
  | 'Malpractice'
  | 'Ragging';

export type VendorContactStatus = 'CONTACTED' | 'CONFIRMED' | 'ON_SITE' | 'DONE';

// ============================================================
// AUTH
// ============================================================

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: Role;
  restrictedAccess?: boolean;
  avatarInitials: string;
}

// ============================================================
// CATEGORY
// ============================================================

export interface FormField {
  key: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'textarea' | 'boolean';
  required: boolean;
  options?: string[];
}

export interface Category {
  id: string;
  name: CategoryName;
  icon: string;
  color: string;
  sensitive: boolean;
  baseSeverityWeight: number;
  requiredSkillTags: string[];
  formSchema: FormField[];
  slaHours: number;
}

// ============================================================
// LOCATION
// ============================================================

export interface Location {
  id: string;
  building: string;
  floor: string;
  room: string;
}

// ============================================================
// STUDENT
// ============================================================

export interface Student {
  id: string;
  name: string;
  rollNo: string;
  department: string;
  contact: string;
}

// ============================================================
// STAFF
// ============================================================

export interface Staff {
  id: string;
  name: string;
  role: string;
  departmentId: string;
  departmentName: string;
  skillTags: string[];
  status: 'ACTIVE' | 'INACTIVE';
  currentWeightedLoad: number;
  avatarInitials: string;
  email: string;
}

// ============================================================
// VENDOR
// ============================================================

export interface Vendor {
  id: string;
  name: string;
  categoryTags: string[];
  phone: string;
  email: string;
}

// ============================================================
// COMPLAINT
// ============================================================

export interface StatusHistoryEntry {
  id: string;
  status: ComplaintStatus;
  changedBy: string;
  changedByRole: Role | 'SYSTEM';
  note?: string;
  timestamp: string;
}

export interface ComplaintVendor {
  vendorId: string;
  vendorName: string;
  assignedAt: string;
  contactStatus: VendorContactStatus;
  notes?: string;
}

export interface Attachment {
  id: string;
  fileUrl: string;
  fileName: string;
  mimeType: string;
}

export interface Review {
  id: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface Complaint {
  id: string;
  token: string;
  categoryId: string;
  categoryName: CategoryName;
  locationId: string;
  location: Location;
  description: string;
  severity: number;
  studentsAffected: number;
  status: ComplaintStatus;
  priorityScore: number;
  extraData: Record<string, string | number | boolean>;
  isAnonymous: boolean;
  incidentId?: string;
  incidentCount?: number;
  recurrenceCountLast90Days?: number;
  student?: Student;
  assignedByAdminId?: string;
  assignedToStaffId?: string;
  assignedToStaff?: Staff;
  dispatchType?: DispatchType;
  vendorAssignment?: ComplaintVendor;
  createdAt: string;
  slaDeadline: string;
  resolvedAt?: string;
  statusHistory: StatusHistoryEntry[];
  attachments: Attachment[];
  review?: Review;
  escalationLevel?: number;
  isAtRisk?: boolean;
  isBreached?: boolean;
  siblingTokens?: string[];
  /** Integer IDs of sibling complaints in the same cluster, for comma display */
  siblingIds?: string[];
  /** True if an admin manually excluded this complaint from auto-clustering */
  clusterExcluded?: boolean;
  /** Clustering confidence score (0.0–1.0) */
  clusterScore?: number;
  /** Full cluster detail when complaint belongs to an incident */
  incident?: IncidentSummary;
}

// ============================================================
// CLUSTER
// ============================================================

export interface ClusterMember {
  id: string;
  token: string;
  description: string;
  status: ComplaintStatus;
  clusterScore?: number;
  createdAt: string;
}

export interface IncidentSummary {
  id: string;
  status: string;
  complaintCount: number;
  /** First complaint's description — display summary for incident card */
  summary?: string;
  members: ClusterMember[];
  createdAt: string;
  lastComplaintAt: string;
}

// ============================================================
// INCIDENT (legacy — full incident aggregate)
// ============================================================

export interface Incident {
  id: string;
  categoryId: string;
  locationId: string;
  status: ComplaintStatus;
  complaintCount: number;
  complaints: Complaint[];
  createdAt: string;
}

// ============================================================
// SLA
// ============================================================

export interface SLARule {
  id: string;
  categoryId: string;
  categoryName: string;
  severityTier: 'LOW' | 'MEDIUM' | 'HIGH';
  resolutionHours: number;
  escalationHours: number;
}

// ============================================================
// ANALYTICS
// ============================================================

export interface HotspotData {
  location: string;
  category: string;
  count: number;
}

export interface SLATrendData {
  period: string;
  breached: number;
  resolved: number;
  atRisk: number;
}

export interface CategoryVolumeData {
  name: string;
  value: number;
  color: string;
}

export interface StaffPerformanceData {
  staffId: string;
  staffName: string;
  department: string;
  avgResolutionHours: number;
  slaCompliancePercent: number;
  openTickets: number;
  resolvedThisMonth: number;
}

// ============================================================
// NOTIFICATIONS
// ============================================================

export interface Notification {
  id: string;
  type: 'NEW_COMPLAINT' | 'STATUS_CHANGE' | 'SLA_ALERT' | 'ASSIGNMENT' | 'ESCALATION';
  title: string;
  message: string;
  complaintId?: string;
  complaintToken?: string;
  timestamp: string;
  read: boolean;
}

// ============================================================
// DEPARTMENT
// ============================================================

export interface Department {
  id: string;
  name: string;
  description: string;
  headOfDepartment: string;
}

// ============================================================
// FILTER STATE
// ============================================================

export interface QueueFilters {
  categories: string[];
  statuses: ComplaintStatus[];
  departmentId?: string;
  dateFrom?: string;
  dateTo?: string;
  priorityMin?: number;
  priorityMax?: number;
  sortBy: 'priority' | 'newest' | 'sla';
}

export interface PriorityWeights {
  w1: number; // severity_norm (severity / 5)
  w2: number; // scale_norm (ln(1 + students_affected) / ln(1 + 500))
  w3: number; // recurrence_norm (min(recurrence_count_last_90_days, 10) / 10)
  w4: number; // time_urgency_norm (min(hours_since_created / sla_hours, 1.0))
}
