import {
  Complaint,
  Category,
  Staff,
  Vendor,
  Department,
  Notification,
  SLARule,
  HotspotData,
  SLATrendData,
  CategoryVolumeData,
  StaffPerformanceData,
  ComplaintStatus,
} from '../types';

// ============================================================
// CATEGORIES
// ============================================================

export const mockCategories: Category[] = [
  {
    id: 'cat-1',
    name: 'Hostel',
    icon: 'Home',
    color: '#6366f1',
    sensitive: false,
    baseSeverityWeight: 3,
    requiredSkillTags: ['maintenance', 'plumbing', 'electrical'],
    slaHours: 24,
    formSchema: [
      { key: 'hostelBlock', label: 'Hostel Block', type: 'select', required: true, options: ['Block A', 'Block B', 'Block C', 'Block D'] },
      { key: 'issueType', label: 'Issue Type', type: 'select', required: true, options: ['Plumbing', 'Electrical', 'Furniture', 'Pest Control', 'Other'] },
      { key: 'roomNumber', label: 'Room Number', type: 'text', required: true },
    ],
  },
  {
    id: 'cat-2',
    name: 'Food',
    icon: 'Utensils',
    color: '#f59e0b',
    sensitive: false,
    baseSeverityWeight: 2,
    requiredSkillTags: ['food-quality', 'hygiene'],
    slaHours: 12,
    formSchema: [
      { key: 'mealType', label: 'Meal Type', type: 'select', required: true, options: ['Breakfast', 'Lunch', 'Dinner', 'Snacks'] },
      { key: 'cafeteria', label: 'Cafeteria', type: 'select', required: true, options: ['Main Cafeteria', 'Block B Canteen', 'Sports Canteen'] },
      { key: 'issueType', label: 'Issue Type', type: 'select', required: true, options: ['Quality', 'Hygiene', 'Quantity', 'Pricing'] },
    ],
  },
  {
    id: 'cat-3',
    name: 'Academic',
    icon: 'BookOpen',
    color: '#3b82f6',
    sensitive: false,
    baseSeverityWeight: 2,
    requiredSkillTags: ['academic', 'faculty'],
    slaHours: 48,
    formSchema: [
      { key: 'department', label: 'Department', type: 'text', required: true },
      { key: 'subject', label: 'Subject/Course', type: 'text', required: true },
      { key: 'issueType', label: 'Issue Type', type: 'select', required: true, options: ['Faculty Conduct', 'Grading', 'Syllabus', 'Lab Equipment', 'Attendance'] },
    ],
  },
  {
    id: 'cat-4',
    name: 'Health',
    icon: 'Activity',
    color: '#ef4444',
    sensitive: false,
    baseSeverityWeight: 4,
    requiredSkillTags: ['health', 'medical'],
    slaHours: 6,
    formSchema: [
      { key: 'symptoms', label: 'Symptoms Reported', type: 'textarea', required: true },
      { key: 'studentsAffectedCount', label: 'Number Affected', type: 'number', required: true },
      { key: 'urgent', label: 'Is this urgent?', type: 'boolean', required: true },
    ],
  },
  {
    id: 'cat-5',
    name: 'Hygiene',
    icon: 'Sparkles',
    color: '#22c55e',
    sensitive: false,
    baseSeverityWeight: 2,
    requiredSkillTags: ['housekeeping', 'sanitation'],
    slaHours: 8,
    formSchema: [
      { key: 'area', label: 'Area', type: 'select', required: true, options: ['Washroom', 'Common Area', 'Dining Hall', 'Corridor', 'Lab', 'Library'] },
      { key: 'issueType', label: 'Issue Type', type: 'select', required: true, options: ['Cleanliness', 'Waste Disposal', 'Pest', 'Drainage'] },
    ],
  },
  {
    id: 'cat-6',
    name: 'Tech Support',
    icon: 'Laptop',
    color: '#06b6d4',
    sensitive: false,
    baseSeverityWeight: 2,
    requiredSkillTags: ['IT', 'networking', 'hardware'],
    slaHours: 12,
    formSchema: [
      { key: 'deviceType', label: 'Device / System', type: 'select', required: true, options: ['PC/Laptop', 'Projector', 'Network/WiFi', 'Printer', 'Server', 'Other'] },
      { key: 'errorDescription', label: 'Error/Issue Description', type: 'textarea', required: true },
      { key: 'labRoom', label: 'Lab / Room', type: 'text', required: false },
    ],
  },
  {
    id: 'cat-7',
    name: 'Harassment',
    icon: 'Lock',
    color: '#ec4899',
    sensitive: true,
    baseSeverityWeight: 5,
    requiredSkillTags: ['counseling', 'authority'],
    slaHours: 24,
    formSchema: [
      { key: 'incidentDate', label: 'Date of Incident', type: 'text', required: true },
      { key: 'incidentLocation', label: 'Location of Incident', type: 'text', required: true },
      { key: 'relationshipToPerpetrator', label: 'Relation to Respondent', type: 'select', required: true, options: ['Faculty', 'Staff', 'Student', 'Other'] },
    ],
  },
  {
    id: 'cat-8',
    name: 'Malpractice',
    icon: 'AlertTriangle',
    color: '#f97316',
    sensitive: true,
    baseSeverityWeight: 5,
    requiredSkillTags: ['authority', 'academic-integrity'],
    slaHours: 48,
    formSchema: [
      { key: 'examOrCourse', label: 'Exam / Course', type: 'text', required: true },
      { key: 'malpracticeType', label: 'Type of Malpractice', type: 'select', required: true, options: ['Cheating', 'Impersonation', 'Plagiarism', 'Bribery', 'Other'] },
      { key: 'evidenceAvailable', label: 'Evidence Available?', type: 'boolean', required: false },
    ],
  },
  {
    id: 'cat-9',
    name: 'Ragging',
    icon: 'ShieldAlert',
    color: '#dc2626',
    sensitive: true,
    baseSeverityWeight: 5,
    requiredSkillTags: ['authority', 'anti-ragging-cell', 'counseling'],
    slaHours: 12,
    formSchema: [
      { key: 'raggingType', label: 'Nature of Ragging', type: 'select', required: true, options: ['Physical Harassment', 'Verbal Abuse / Intimidation', 'Forced Acts / Tasks', 'Extortion / Financial Demands', 'Cyber / Online Ragging'] },
      { key: 'incidentLocation', label: 'Location / Hostel Block', type: 'text', required: true },
      { key: 'perpetratorsKnown', label: 'Perpetrators Identified?', type: 'select', required: true, options: ['Yes - Senior batch known', 'Partial - Room/Branch known', 'Unknown / Group'] },
      { key: 'immediateAssistanceNeeded', label: 'Immediate Medical or Physical Assistance Required?', type: 'boolean', required: true },
    ],
  },
];

// ============================================================
// DEPARTMENTS
// ============================================================

export const mockDepartments: Department[] = [
  { id: 'dept-1', name: 'Maintenance', description: 'Handles hostel and building upkeep', headOfDepartment: 'Rajesh Kumar' },
  { id: 'dept-2', name: 'Catering & Food', description: 'Manages food quality and cafeteria operations', headOfDepartment: 'Meena Sundaram' },
  { id: 'dept-3', name: 'IT & Systems', description: 'Handles all technology and network issues', headOfDepartment: 'Arjun Nair' },
  { id: 'dept-4', name: 'Student Affairs', description: 'Handles academic, health, and sensitive complaints', headOfDepartment: 'Dr. Priya Sharma' },
];

// ============================================================
// STAFF
// ============================================================

export const mockStaff: Staff[] = [
  { id: 'stf-1', name: 'Vikram Singh', role: 'Senior Technician', departmentId: 'dept-1', departmentName: 'Maintenance', skillTags: ['plumbing', 'electrical', 'maintenance'], status: 'ACTIVE', currentWeightedLoad: 42.5, avatarInitials: 'VS', email: 'vikram@fixloop.edu' },
  { id: 'stf-2', name: 'Anita Rao', role: 'Junior Technician', departmentId: 'dept-1', departmentName: 'Maintenance', skillTags: ['maintenance', 'carpentry'], status: 'ACTIVE', currentWeightedLoad: 18.0, avatarInitials: 'AR', email: 'anita@fixloop.edu' },
  { id: 'stf-3', name: 'Suresh Babu', role: 'Food Inspector', departmentId: 'dept-2', departmentName: 'Catering & Food', skillTags: ['food-quality', 'hygiene', 'sanitation'], status: 'ACTIVE', currentWeightedLoad: 25.3, avatarInitials: 'SB', email: 'suresh@fixloop.edu' },
  { id: 'stf-4', name: 'Kavitha Menon', role: 'Housekeeping Supervisor', departmentId: 'dept-2', departmentName: 'Catering & Food', skillTags: ['housekeeping', 'sanitation'], status: 'ACTIVE', currentWeightedLoad: 12.1, avatarInitials: 'KM', email: 'kavitha@fixloop.edu' },
  { id: 'stf-5', name: 'Deepak Joshi', role: 'IT Support Lead', departmentId: 'dept-3', departmentName: 'IT & Systems', skillTags: ['IT', 'networking', 'hardware'], status: 'ACTIVE', currentWeightedLoad: 55.8, avatarInitials: 'DJ', email: 'deepak@fixloop.edu' },
  { id: 'stf-6', name: 'Nisha Gupta', role: 'Network Engineer', departmentId: 'dept-3', departmentName: 'IT & Systems', skillTags: ['networking', 'IT'], status: 'ACTIVE', currentWeightedLoad: 30.2, avatarInitials: 'NG', email: 'nisha@fixloop.edu' },
  { id: 'stf-7', name: 'Ravi Chandran', role: 'Student Counselor', departmentId: 'dept-4', departmentName: 'Student Affairs', skillTags: ['counseling', 'academic', 'authority'], status: 'ACTIVE', currentWeightedLoad: 20.0, avatarInitials: 'RC', email: 'ravi@fixloop.edu' },
  { id: 'stf-8', name: 'Lalitha Devi', role: 'Medical Officer', departmentId: 'dept-4', departmentName: 'Student Affairs', skillTags: ['health', 'medical', 'counseling'], status: 'INACTIVE', currentWeightedLoad: 0, avatarInitials: 'LD', email: 'lalitha@fixloop.edu' },
];

// ============================================================
// VENDORS
// ============================================================

export const mockVendors: Vendor[] = [
  { id: 'ven-1', name: 'QuickFix Plumbers', categoryTags: ['plumbing'], phone: '+91 98765 43210', email: 'quickfix@vendors.in' },
  { id: 'ven-2', name: 'BrightSpark Electricals', categoryTags: ['electrical'], phone: '+91 87654 32109', email: 'brightspark@vendors.in' },
  { id: 'ven-3', name: 'CleanZone Sanitation', categoryTags: ['housekeeping', 'sanitation', 'pest-control'], phone: '+91 76543 21098', email: 'cleanzone@vendors.in' },
  { id: 'ven-4', name: 'CoolBreeze HVAC', categoryTags: ['electrical', 'maintenance'], phone: '+91 65432 10987', email: 'coolbreeze@vendors.in' },
  { id: 'ven-5', name: 'TechServe Systems', categoryTags: ['IT', 'hardware', 'networking'], phone: '+91 54321 09876', email: 'techserve@vendors.in' },
];

// ============================================================
// HELPERS
// ============================================================

const daysAgo = (d: number): string => {
  const date = new Date();
  date.setDate(date.getDate() - d);
  return date.toISOString();
};

const hoursAgo = (h: number): string => {
  const date = new Date();
  date.setHours(date.getHours() - h);
  return date.toISOString();
};

const hoursFromNow = (h: number): string => {
  const date = new Date();
  date.setHours(date.getHours() + h);
  return date.toISOString();
};

// ============================================================
// PRIORITY SCORING (User's Normalization Formula)
// ============================================================
// severity_norm     = severity / 5
// scale_norm        = ln(1 + students_affected) / ln(1 + 500)   // cap at 500 students
// recurrence_norm   = min(recurrence_count_last_90_days, 10) / 10
// time_urgency_norm = min(hours_since_created / sla_hours, 1.0)
// priority_score    = (0.48 * severity_norm) + (0.21 * scale_norm) + (0.08 * recurrence_norm) + (0.23 * time_urgency_norm)

export const computePriority = (
  severity: number,
  studentsAffected: number,
  recurrenceCount: number = 0,
  hoursElapsed: number = 0,
  slaHours: number = 24
): number => {
  const severity_norm = Math.min(Math.max(severity, 1), 5) / 5;
  const scale_norm = Math.log(1 + Math.min(Math.max(studentsAffected, 1), 500)) / Math.log(1 + 500);
  const recurrence_norm = Math.min(Math.max(recurrenceCount, 0), 10) / 10;
  const time_urgency_norm = Math.min(Math.max(hoursElapsed, 0) / Math.max(slaHours, 1), 1.0);

  const rawScore =
    0.48 * severity_norm +
    0.21 * scale_norm +
    0.08 * recurrence_norm +
    0.23 * time_urgency_norm;

  return Number((rawScore * 100).toFixed(1));
};

// ============================================================
// COMPLAINTS
// ============================================================

export const mockComplaints: Complaint[] = [
  // 1 — CRITICAL, AT_RISK
  {
    id: 'cmp-001',
    token: 'FXL-2026-000182',
    categoryId: 'cat-4',
    categoryName: 'Health',
    locationId: 'loc-1',
    location: { id: 'loc-1', building: 'Medical Block', floor: 'Ground', room: 'Infirmary' },
    description: 'Multiple students from Block B reported severe stomach pain and vomiting after lunch on Tuesday. Currently 14 students are in the infirmary.',
    severity: 5,
    studentsAffected: 14,
    status: 'IN_PROGRESS',
    priorityScore: 98.4,
    extraData: { symptoms: 'Nausea, vomiting, abdominal cramps', studentsAffectedCount: 14, urgent: true },
    isAnonymous: false,
    incidentId: 'inc-001',
    incidentCount: 14,
    student: { id: 'stu-1', name: 'Aryan Mehta', rollNo: '22CS101', department: 'Computer Science', contact: '+91 90000 00001' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-7',
    assignedToStaff: mockStaff[6],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: hoursAgo(10),
    slaDeadline: hoursFromNow(2),
    isAtRisk: true,
    isBreached: false,
    statusHistory: [
      { id: 'sh-1', status: 'SUBMITTED', changedBy: 'Aryan Mehta', changedByRole: 'STAFF', timestamp: hoursAgo(10) },
      { id: 'sh-2', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'High priority — escalated immediately', timestamp: hoursAgo(9) },
      { id: 'sh-3', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', note: 'Auto-assigned to Ravi Chandran (lowest load in Student Affairs)', timestamp: hoursAgo(8) },
      { id: 'sh-4', status: 'IN_PROGRESS', changedBy: 'Ravi Chandran', changedByRole: 'STAFF', note: 'Coordinating with infirmary staff. Food samples sent to lab.', timestamp: hoursAgo(6) },
    ],
    attachments: [
      { id: 'att-1', fileUrl: 'https://placehold.co/400x300/1a1f35/6366f1?text=Infirmary+Report', fileName: 'infirmary_report.jpg', mimeType: 'image/jpeg' },
    ],
    siblingTokens: ['FXL-2026-000183', 'FXL-2026-000184', 'FXL-2026-000185'],
    escalationLevel: 1,
  },
  // 2 — BREACHED
  {
    id: 'cmp-002',
    token: 'FXL-2026-000165',
    categoryId: 'cat-1',
    categoryName: 'Hostel',
    locationId: 'loc-2',
    location: { id: 'loc-2', building: 'Block A Hostel', floor: '3rd Floor', room: 'A-312' },
    description: 'The bathroom ceiling has been leaking for 3 days. Water accumulation is causing floor slippage hazard. Multiple requests have been ignored.',
    severity: 4,
    studentsAffected: 6,
    status: 'ASSIGNED',
    priorityScore: 87.1,
    extraData: { hostelBlock: 'Block A', issueType: 'Plumbing', roomNumber: 'A-312' },
    isAnonymous: false,
    incidentId: 'inc-002',
    incidentCount: 3,
    student: { id: 'stu-2', name: 'Priya Nambiar', rollNo: '22ME045', department: 'Mechanical Engg', contact: '+91 90000 00002' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-1',
    assignedToStaff: mockStaff[0],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: daysAgo(3),
    slaDeadline: hoursAgo(15),
    isAtRisk: false,
    isBreached: true,
    statusHistory: [
      { id: 'sh-5', status: 'SUBMITTED', changedBy: 'Priya Nambiar', changedByRole: 'STAFF', timestamp: daysAgo(3) },
      { id: 'sh-6', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: daysAgo(3) },
      { id: 'sh-7', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', note: 'Auto-assigned to Vikram Singh', timestamp: daysAgo(2) },
    ],
    attachments: [
      { id: 'att-2', fileUrl: 'https://placehold.co/400x300/1a1f35/ef4444?text=Ceiling+Leak', fileName: 'ceiling_leak.jpg', mimeType: 'image/jpeg' },
      { id: 'att-3', fileUrl: 'https://placehold.co/400x300/1a1f35/ef4444?text=Water+Damage', fileName: 'water_damage.jpg', mimeType: 'image/jpeg' },
    ],
    siblingTokens: ['FXL-2026-000166', 'FXL-2026-000167'],
    escalationLevel: 2,
  },
  // 3 — IN_PROGRESS, VENDOR
  {
    id: 'cmp-003',
    token: 'FXL-2026-000201',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-3',
    location: { id: 'loc-3', building: 'CS Block', floor: '2nd Floor', room: 'Lab CS-204' },
    description: 'All 30 PCs in Lab CS-204 are showing BSOD on startup since yesterday morning. Programming practicals are blocked for 3rd year batch.',
    severity: 4,
    studentsAffected: 60,
    status: 'VENDOR_ASSIGNED',
    priorityScore: 91.7,
    extraData: { deviceType: 'PC/Laptop', errorDescription: 'Blue Screen of Death on startup — CRITICAL_PROCESS_DIED', labRoom: 'CS-204' },
    isAnonymous: false,
    incidentId: 'inc-003',
    incidentCount: 7,
    student: { id: 'stu-3', name: 'Kiran Thomas', rollNo: '24CS310', department: 'Computer Science', contact: '+91 90000 00003' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'EXTERNAL_VENDOR',
    vendorAssignment: { vendorId: 'ven-5', vendorName: 'TechServe Systems', assignedAt: hoursAgo(5), contactStatus: 'ON_SITE', notes: 'Vendor on-site, diagnosing driver issue.' },
    createdAt: hoursAgo(20),
    slaDeadline: hoursFromNow(4),
    isAtRisk: true,
    isBreached: false,
    statusHistory: [
      { id: 'sh-8', status: 'SUBMITTED', changedBy: 'Kiran Thomas', changedByRole: 'STAFF', timestamp: hoursAgo(20) },
      { id: 'sh-9', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: hoursAgo(19) },
      { id: 'sh-10', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', timestamp: hoursAgo(18) },
      { id: 'sh-11', status: 'VENDOR_ASSIGNED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'Escalated to TechServe Systems — beyond in-house capacity', timestamp: hoursAgo(5) },
    ],
    attachments: [
      { id: 'att-4', fileUrl: 'https://placehold.co/400x300/1a1f35/06b6d4?text=BSOD+Error', fileName: 'bsod_error.jpg', mimeType: 'image/jpeg' },
    ],
    siblingTokens: ['FXL-2026-000202', 'FXL-2026-000203'],
  },
  // 4 — AWAITING INFO
  {
    id: 'cmp-004',
    token: 'FXL-2026-000198',
    categoryId: 'cat-2',
    categoryName: 'Food',
    locationId: 'loc-4',
    location: { id: 'loc-4', building: 'Main Cafeteria', floor: 'Ground', room: 'Dining Hall' },
    description: 'The rice served at dinner consistently has small stones/foreign objects. This has been happening for 2 weeks.',
    severity: 3,
    studentsAffected: 200,
    status: 'AWAITING_STUDENT_INFO',
    priorityScore: 72.3,
    extraData: { mealType: 'Dinner', cafeteria: 'Main Cafeteria', issueType: 'Quality' },
    isAnonymous: false,
    student: { id: 'stu-4', name: 'Sneha Iyer', rollNo: '23EC012', department: 'Electronics', contact: '+91 90000 00004' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-3',
    assignedToStaff: mockStaff[2],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: daysAgo(2),
    slaDeadline: hoursFromNow(8),
    statusHistory: [
      { id: 'sh-12', status: 'SUBMITTED', changedBy: 'Sneha Iyer', changedByRole: 'STAFF', timestamp: daysAgo(2) },
      { id: 'sh-13', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: daysAgo(2) },
      { id: 'sh-14', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', timestamp: daysAgo(1) },
      { id: 'sh-15', status: 'IN_PROGRESS', changedBy: 'Suresh Babu', changedByRole: 'STAFF', timestamp: hoursAgo(20) },
      { id: 'sh-16', status: 'AWAITING_STUDENT_INFO', changedBy: 'Suresh Babu', changedByRole: 'STAFF', note: 'Can you provide photos of the affected food? Was this only one serving or multiple batches?', timestamp: hoursAgo(10) },
    ],
    attachments: [],
  },
  // 5 — RESOLVED
  {
    id: 'cmp-005',
    token: 'FXL-2026-000155',
    categoryId: 'cat-5',
    categoryName: 'Hygiene',
    locationId: 'loc-5',
    location: { id: 'loc-5', building: 'Library Block', floor: '1st Floor', room: 'Washroom LB-101' },
    description: 'Washroom near the library entrance has been unclean for days. Foul odor and clogged drain.',
    severity: 2,
    studentsAffected: 80,
    status: 'RESOLVED',
    priorityScore: 45.2,
    extraData: { area: 'Washroom', issueType: 'Drainage' },
    isAnonymous: false,
    student: { id: 'stu-5', name: 'Rahul Verma', rollNo: '23CE033', department: 'Civil Engg', contact: '+91 90000 00005' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-4',
    assignedToStaff: mockStaff[3],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: daysAgo(5),
    slaDeadline: daysAgo(4),
    resolvedAt: daysAgo(1),
    statusHistory: [
      { id: 'sh-17', status: 'SUBMITTED', changedBy: 'Rahul Verma', changedByRole: 'STAFF', timestamp: daysAgo(5) },
      { id: 'sh-18', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: daysAgo(5) },
      { id: 'sh-19', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', timestamp: daysAgo(4) },
      { id: 'sh-20', status: 'IN_PROGRESS', changedBy: 'Kavitha Menon', changedByRole: 'STAFF', timestamp: daysAgo(3) },
      { id: 'sh-21', status: 'RESOLVED', changedBy: 'Kavitha Menon', changedByRole: 'STAFF', note: 'Drain cleared, washroom cleaned and disinfected. Fragrance dispensers installed.', timestamp: daysAgo(1) },
    ],
    attachments: [],
    review: { id: 'rev-1', rating: 4, comment: 'Fixed quickly, appreciate the prompt response!', createdAt: daysAgo(1) },
  },
  // 6 — REOPENED
  {
    id: 'cmp-006',
    token: 'FXL-2026-000140',
    categoryId: 'cat-1',
    categoryName: 'Hostel',
    locationId: 'loc-6',
    location: { id: 'loc-6', building: 'Block C Hostel', floor: '2nd Floor', room: 'C-215' },
    description: 'AC unit in room C-215 was reported fixed but it is still not cooling properly. Temperature is 32°C inside the room.',
    severity: 3,
    studentsAffected: 2,
    status: 'REOPENED',
    priorityScore: 58.9,
    extraData: { hostelBlock: 'Block C', issueType: 'Electrical', roomNumber: 'C-215' },
    isAnonymous: false,
    student: { id: 'stu-6', name: 'Aditi Sharma', rollNo: '21EE078', department: 'Electrical Engg', contact: '+91 90000 00006' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-1',
    assignedToStaff: mockStaff[0],
    dispatchType: 'EXTERNAL_VENDOR',
    vendorAssignment: { vendorId: 'ven-4', vendorName: 'CoolBreeze HVAC', assignedAt: hoursAgo(48), contactStatus: 'CONFIRMED', notes: 'Vendor revisit scheduled for tomorrow morning.' },
    createdAt: daysAgo(8),
    slaDeadline: hoursFromNow(24),
    statusHistory: [
      { id: 'sh-22', status: 'SUBMITTED', changedBy: 'Aditi Sharma', changedByRole: 'STAFF', timestamp: daysAgo(8) },
      { id: 'sh-23', status: 'RESOLVED', changedBy: 'Vikram Singh', changedByRole: 'STAFF', note: 'Refrigerant recharged, unit operational', timestamp: daysAgo(3) },
      { id: 'sh-24', status: 'REOPENED', changedBy: 'Aditi Sharma', changedByRole: 'STAFF', note: 'AC is still not working — temperature showing 32°C. Fix was incomplete.', timestamp: daysAgo(1) },
    ],
    attachments: [],
  },
  // 7 — SUBMITTED (fresh)
  {
    id: 'cmp-007',
    token: 'FXL-2026-000215',
    categoryId: 'cat-3',
    categoryName: 'Academic',
    locationId: 'loc-7',
    location: { id: 'loc-7', building: 'ECE Block', floor: '4th Floor', room: 'Room EC-401' },
    description: 'Faculty has been marking absences for students who are present in class. 12 students have been affected this semester.',
    severity: 3,
    studentsAffected: 12,
    status: 'SUBMITTED',
    priorityScore: 65.0,
    extraData: { department: 'Electronics & Communication', subject: 'Signals and Systems', issueType: 'Attendance' },
    isAnonymous: false,
    student: { id: 'stu-7', name: 'Mohan Krishnaswamy', rollNo: '23EC056', department: 'Electronics', contact: '+91 90000 00007' },
    createdAt: hoursAgo(1),
    slaDeadline: hoursFromNow(47),
    statusHistory: [
      { id: 'sh-25', status: 'SUBMITTED', changedBy: 'Mohan Krishnaswamy', changedByRole: 'STAFF', timestamp: hoursAgo(1) },
    ],
    attachments: [],
  },
  // 8 — SENSITIVE, ANONYMOUS (Harassment)
  {
    id: 'cmp-008',
    token: 'FXL-2026-000210',
    categoryId: 'cat-7',
    categoryName: 'Harassment',
    locationId: 'loc-8',
    location: { id: 'loc-8', building: 'Admin Block', floor: '1st Floor', room: 'Faculty Room A-106' },
    description: 'A faculty member has been making inappropriate comments during class and singling out students for personal remarks.',
    severity: 5,
    studentsAffected: 3,
    status: 'UNDER_ADMIN_REVIEW',
    priorityScore: 95.0,
    extraData: { incidentDate: '2026-07-22', incidentLocation: 'Lecture Hall H-301', relationshipToPerpetrator: 'Faculty' },
    isAnonymous: true,
    createdAt: hoursAgo(14),
    slaDeadline: hoursFromNow(10),
    statusHistory: [
      { id: 'sh-26', status: 'SUBMITTED', changedBy: 'Anonymous', changedByRole: 'STAFF', timestamp: hoursAgo(14) },
      { id: 'sh-27', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Restricted — under authority review only', timestamp: hoursAgo(12) },
    ],
    attachments: [],
  },
  // 9 — SENSITIVE (Malpractice)
  {
    id: 'cmp-009',
    token: 'FXL-2026-000205',
    categoryId: 'cat-8',
    categoryName: 'Malpractice',
    locationId: 'loc-9',
    location: { id: 'loc-9', building: 'Examination Hall', floor: 'Ground', room: 'Hall E-1' },
    description: 'Students were seen passing handwritten chits during the mid-semester examination. Invigilator allegedly ignored it.',
    severity: 4,
    studentsAffected: 8,
    status: 'ASSIGNED',
    priorityScore: 88.5,
    extraData: { examOrCourse: 'Data Structures Mid-Sem 2026', malpracticeType: 'Cheating', evidenceAvailable: true },
    isAnonymous: true,
    createdAt: daysAgo(1),
    slaDeadline: hoursFromNow(30),
    statusHistory: [
      { id: 'sh-28', status: 'SUBMITTED', changedBy: 'Anonymous', changedByRole: 'STAFF', timestamp: daysAgo(1) },
      { id: 'sh-29', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: daysAgo(1) },
      { id: 'sh-30', status: 'ASSIGNED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Assigned to restricted authority group', timestamp: hoursAgo(22) },
    ],
    attachments: [],
  },
  // 10 — CLOSED (old)
  {
    id: 'cmp-010',
    token: 'FXL-2026-000121',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-10',
    location: { id: 'loc-10', building: 'Library Block', floor: 'Ground', room: 'Lib Study Hall' },
    description: 'WiFi is completely down in the library study hall. Cannot access online resources or e-library.',
    severity: 3,
    studentsAffected: 150,
    status: 'CLOSED',
    priorityScore: 70.0,
    extraData: { deviceType: 'Network/WiFi', errorDescription: 'Complete WiFi outage in Library Study Hall', labRoom: 'Library Study Hall' },
    isAnonymous: false,
    student: { id: 'stu-8', name: 'Ranjitha Pillai', rollNo: '22IT099', department: 'Information Technology', contact: '+91 90000 00008' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-6',
    assignedToStaff: mockStaff[5],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: daysAgo(10),
    slaDeadline: daysAgo(9),
    resolvedAt: daysAgo(7),
    statusHistory: [
      { id: 'sh-31', status: 'SUBMITTED', changedBy: 'Ranjitha Pillai', changedByRole: 'STAFF', timestamp: daysAgo(10) },
      { id: 'sh-32', status: 'IN_PROGRESS', changedBy: 'Nisha Gupta', changedByRole: 'STAFF', timestamp: daysAgo(9) },
      { id: 'sh-33', status: 'RESOLVED', changedBy: 'Nisha Gupta', changedByRole: 'STAFF', note: 'Access point replaced. WiFi restored.', timestamp: daysAgo(7) },
      { id: 'sh-34', status: 'CLOSED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: daysAgo(6) },
    ],
    attachments: [],
    review: { id: 'rev-2', rating: 5, comment: 'Fixed the same day, excellent!', createdAt: daysAgo(7) },
  },
  // 11 — Another hostel complaint
  {
    id: 'cmp-011',
    token: 'FXL-2026-000207',
    categoryId: 'cat-1',
    categoryName: 'Hostel',
    locationId: 'loc-11',
    location: { id: 'loc-11', building: 'Block D Hostel', floor: '1st Floor', room: 'D-108' },
    description: 'Water heater in Block D room D-108 is not functioning. Cold water only for 5 days.',
    severity: 2,
    studentsAffected: 2,
    status: 'IN_PROGRESS',
    priorityScore: 38.6,
    extraData: { hostelBlock: 'Block D', issueType: 'Electrical', roomNumber: 'D-108' },
    isAnonymous: false,
    incidentId: 'inc-004',
    incidentCount: 2,
    student: { id: 'stu-9', name: 'Amit Bose', rollNo: '24CS205', department: 'Computer Science', contact: '+91 90000 00009' },
    assignedToStaff: mockStaff[1],
    assignedToStaffId: 'stf-2',
    dispatchType: 'INTERNAL_STAFF',
    createdAt: daysAgo(2),
    slaDeadline: hoursFromNow(14),
    statusHistory: [
      { id: 'sh-35', status: 'SUBMITTED', changedBy: 'Amit Bose', changedByRole: 'STAFF', timestamp: daysAgo(2) },
      { id: 'sh-36', status: 'ASSIGNED', changedBy: 'System', changedByRole: 'SYSTEM', timestamp: daysAgo(1) },
      { id: 'sh-37', status: 'IN_PROGRESS', changedBy: 'Anita Rao', changedByRole: 'STAFF', timestamp: hoursAgo(12) },
    ],
    attachments: [],
  },
  // 12 — Food (SUBMITTED)
  {
    id: 'cmp-012',
    token: 'FXL-2026-000212',
    categoryId: 'cat-2',
    categoryName: 'Food',
    locationId: 'loc-4',
    location: { id: 'loc-4', building: 'Main Cafeteria', floor: 'Ground', room: 'Dining Hall' },
    description: 'Insects were found in the sambar served at lunch. Multiple students witnessed it.',
    severity: 4,
    studentsAffected: 50,
    status: 'UNDER_ADMIN_REVIEW',
    priorityScore: 79.2,
    extraData: { mealType: 'Lunch', cafeteria: 'Main Cafeteria', issueType: 'Hygiene' },
    isAnonymous: false,
    incidentId: 'inc-005',
    incidentCount: 4,
    student: { id: 'stu-10', name: 'Divya Rajendran', rollNo: '23ME078', department: 'Mechanical Engg', contact: '+91 90000 00010' },
    createdAt: hoursAgo(3),
    slaDeadline: hoursFromNow(9),
    statusHistory: [
      { id: 'sh-38', status: 'SUBMITTED', changedBy: 'Divya Rajendran', changedByRole: 'STAFF', timestamp: hoursAgo(3) },
      { id: 'sh-39', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: hoursAgo(2) },
    ],
    attachments: [
      { id: 'att-5', fileUrl: 'https://placehold.co/400x300/1a1f35/f59e0b?text=Food+Issue', fileName: 'food_issue.jpg', mimeType: 'image/jpeg' },
    ],
    siblingTokens: ['FXL-2026-000213', 'FXL-2026-000214'],
  },
  // 13 — RESOLVED (September 2026) - Assigned to Deepak Joshi (stf-5)
  {
    id: 'cmp-013',
    token: 'FXL-2026-000220',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-3',
    location: { id: 'loc-3', building: 'CS Block', floor: '3rd Floor', room: 'Lab CS-306' },
    description: 'High-speed Gigabit switch port failure in Lab CS-306 disconnected 28 workstations during practical exams.',
    severity: 4,
    studentsAffected: 28,
    status: 'RESOLVED',
    priorityScore: 78.4,
    extraData: { lab: 'CS-306', issueType: 'Networking Switch', workstations: 28 },
    isAnonymous: false,
    student: { id: 'stu-3', name: 'Sneha Patel', rollNo: '23IT044', department: 'Information Technology', contact: '+91 90000 00003' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-09-01T09:15:00.000Z',
    slaDeadline: '2026-09-01T21:15:00.000Z',
    resolvedAt: '2026-09-01T14:30:00.000Z',
    statusHistory: [
      { id: 'sh-40', status: 'SUBMITTED', changedBy: 'Sneha Patel', changedByRole: 'STAFF', timestamp: '2026-09-01T09:15:00.000Z' },
      { id: 'sh-41', status: 'ASSIGNED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: '2026-09-01T09:30:00.000Z' },
      { id: 'sh-42', status: 'IN_PROGRESS', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', timestamp: '2026-09-01T10:00:00.000Z' },
      { id: 'sh-43', status: 'RESOLVED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'Replaced faulty Cisco 48-port switch with backup unit. All 28 links verified active.', timestamp: '2026-09-01T14:30:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-3', rating: 5, comment: 'Resolved well within SLA before afternoon lab session started!', createdAt: '2026-09-01T15:00:00.000Z' },
  },
  // 14 — RESOLVED (September 2026) - Assigned to Vikram Singh (stf-1)
  {
    id: 'cmp-014',
    token: 'FXL-2026-000218',
    categoryId: 'cat-1',
    categoryName: 'Hostel',
    locationId: 'loc-2',
    location: { id: 'loc-2', building: 'Block A Hostel', floor: '2nd Floor', room: 'A-204' },
    description: 'Main corridor fluorescent lighting ballast burned out creating dark unsafe walkway.',
    severity: 2,
    studentsAffected: 40,
    status: 'RESOLVED',
    priorityScore: 52.0,
    extraData: { hostelBlock: 'Block A', issueType: 'Electrical' },
    isAnonymous: false,
    student: { id: 'stu-1', name: 'Aryan Mehta', rollNo: '22CS101', department: 'Computer Science', contact: '+91 90000 00001' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-1',
    assignedToStaff: mockStaff[0],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-09-02T10:00:00.000Z',
    slaDeadline: '2026-09-03T10:00:00.000Z',
    resolvedAt: '2026-09-02T16:20:00.000Z',
    statusHistory: [
      { id: 'sh-44', status: 'SUBMITTED', changedBy: 'Aryan Mehta', changedByRole: 'STAFF', timestamp: '2026-09-02T10:00:00.000Z' },
      { id: 'sh-45', status: 'RESOLVED', changedBy: 'Vikram Singh', changedByRole: 'STAFF', note: 'LED conversion kit installed. Corridor lighting fully restored.', timestamp: '2026-09-02T16:20:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-4', rating: 4, comment: 'Good work, much brighter than before.', createdAt: '2026-09-02T17:00:00.000Z' },
  },
  // 15 — RESOLVED (August 2026) - Assigned to Deepak Joshi (stf-5)
  {
    id: 'cmp-015',
    token: 'FXL-2026-000170',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-8',
    location: { id: 'loc-8', building: 'Library Block', floor: 'Ground Floor', room: 'Digital Library' },
    description: 'Digital catalog search kiosk screen flickering and touch digitizer unresponsive.',
    severity: 2,
    studentsAffected: 65,
    status: 'CLOSED',
    priorityScore: 42.5,
    extraData: { issueType: 'Hardware', device: 'OPAC Terminal 2' },
    isAnonymous: false,
    student: { id: 'stu-5', name: 'Rahul Verma', rollNo: '23CE033', department: 'Civil Engg', contact: '+91 90000 00005' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-08-25T11:00:00.000Z',
    slaDeadline: '2026-08-26T11:00:00.000Z',
    resolvedAt: '2026-08-25T15:45:00.000Z',
    statusHistory: [
      { id: 'sh-46', status: 'RESOLVED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'HDMI cable and touch controller ribbon replaced. Kiosk calibrated.', timestamp: '2026-08-25T15:45:00.000Z' },
      { id: 'sh-47', status: 'CLOSED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: '2026-08-26T09:00:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-5', rating: 5, comment: 'Working smoothly now, thanks Deepak!', createdAt: '2026-08-25T16:30:00.000Z' },
  },
  // 16 — RESOLVED (August 2026) - Assigned to Suresh Babu (stf-3)
  {
    id: 'cmp-016',
    token: 'FXL-2026-000162',
    categoryId: 'cat-2',
    categoryName: 'Food',
    locationId: 'loc-4',
    location: { id: 'loc-4', building: 'Main Cafeteria', floor: 'Ground', room: 'Juice Counter' },
    description: 'Expired dairy products stored in front-facing display cooler at the cafeteria juice kiosk.',
    severity: 4,
    studentsAffected: 35,
    status: 'CLOSED',
    priorityScore: 81.0,
    extraData: { cafeteria: 'Main Cafeteria', inspectionType: 'Food Safety' },
    isAnonymous: false,
    student: { id: 'stu-2', name: 'Pooja Sundaram', rollNo: '21BT012', department: 'Biotechnology', contact: '+91 90000 00002' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-3',
    assignedToStaff: mockStaff[2],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-08-18T08:30:00.000Z',
    slaDeadline: '2026-08-18T16:30:00.000Z',
    resolvedAt: '2026-08-18T12:00:00.000Z',
    statusHistory: [
      { id: 'sh-48', status: 'RESOLVED', changedBy: 'Suresh Babu', changedByRole: 'STAFF', note: 'Entire batch discarded. Vendor penalized and issued formal compliance notice.', timestamp: '2026-08-18T12:00:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-6', rating: 5, comment: 'Immediate action taken by inspection team.', createdAt: '2026-08-18T14:00:00.000Z' },
  },
  // 17 — RESOLVED (August 2026 - BREACHED SLA) - Assigned to Anita Rao (stf-2)
  {
    id: 'cmp-017',
    token: 'FXL-2026-000150',
    categoryId: 'cat-1',
    categoryName: 'Hostel',
    locationId: 'loc-6',
    location: { id: 'loc-6', building: 'Block C Hostel', floor: '3rd Floor', room: 'C-305' },
    description: 'Broken window latch in room C-305 causing window to slam shut violently during rainstorms.',
    severity: 2,
    studentsAffected: 3,
    status: 'RESOLVED',
    priorityScore: 41.2,
    isBreached: true,
    extraData: { issueType: 'Carpentry', roomNumber: 'C-305' },
    isAnonymous: false,
    student: { id: 'stu-6', name: 'Aditi Sharma', rollNo: '21EE078', department: 'Electrical Engg', contact: '+91 90000 00006' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-2',
    assignedToStaff: mockStaff[1],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-08-05T09:00:00.000Z',
    slaDeadline: '2026-08-06T09:00:00.000Z',
    resolvedAt: '2026-08-07T14:00:00.000Z',
    statusHistory: [
      { id: 'sh-49', status: 'RESOLVED', changedBy: 'Anita Rao', changedByRole: 'STAFF', note: 'Brass sash latch installed and frame realigned. Delay due to material procurement.', timestamp: '2026-08-07T14:00:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-7', rating: 3, comment: 'Took two days longer than promised but fix is solid.', createdAt: '2026-08-07T16:00:00.000Z' },
  },
  // 18 — RESOLVED (July 2026) - Assigned to Deepak Joshi (stf-5)
  {
    id: 'cmp-018',
    token: 'FXL-2026-000125',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-7',
    location: { id: 'loc-7', building: 'ECE Block', floor: '2nd Floor', room: 'Auditorium' },
    description: 'Ceiling projector HDMI transmission loss during guest lecture series.',
    severity: 3,
    studentsAffected: 120,
    status: 'CLOSED',
    priorityScore: 68.0,
    extraData: { issueType: 'AV Equipment', venue: 'Auditorium' },
    isAnonymous: false,
    student: { id: 'stu-7', name: 'Mohan Krishnaswamy', rollNo: '23EC056', department: 'Electronics', contact: '+91 90000 00007' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-07-20T13:00:00.000Z',
    slaDeadline: '2026-07-21T01:00:00.000Z',
    resolvedAt: '2026-07-20T16:15:00.000Z',
    statusHistory: [
      { id: 'sh-50', status: 'RESOLVED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'Active optical HDMI repeater replaced. Tested 4K video playback successfully.', timestamp: '2026-07-20T16:15:00.000Z' },
      { id: 'sh-51', status: 'CLOSED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: '2026-07-21T10:00:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-8', rating: 5, comment: 'Saved the seminar! Excellent prompt support.', createdAt: '2026-07-20T17:00:00.000Z' },
  },
  // 19 — RESOLVED (July 2026) - Assigned to Ravi Chandran (stf-7)
  {
    id: 'cmp-019',
    token: 'FXL-2026-000110',
    categoryId: 'cat-3',
    categoryName: 'Academic',
    locationId: 'loc-7',
    location: { id: 'loc-7', building: 'Administrative Block', floor: '1st Floor', room: 'Exam Cell' },
    description: 'Grade card print discrepancy for semester 4 re-evaluation results.',
    severity: 3,
    studentsAffected: 15,
    status: 'CLOSED',
    priorityScore: 59.5,
    extraData: { semester: 4, issueType: 'Marks Card' },
    isAnonymous: false,
    student: { id: 'stu-4', name: 'Karthik Raja', rollNo: '22ME145', department: 'Mechanical Engg', contact: '+91 90000 00004' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-7',
    assignedToStaff: mockStaff[6],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-07-08T10:00:00.000Z',
    slaDeadline: '2026-07-10T10:00:00.000Z',
    resolvedAt: '2026-07-09T14:30:00.000Z',
    statusHistory: [
      { id: 'sh-52', status: 'RESOLVED', changedBy: 'Ravi Chandran', changedByRole: 'STAFF', note: 'Corrected marks database entry and re-issued stamped marksheets to all 15 students.', timestamp: '2026-07-09T14:30:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-9', rating: 4, comment: 'Promptly clarified and re-issued.', createdAt: '2026-07-09T16:00:00.000Z' },
  },
  // 20 — RESOLVED (June 2026) - Assigned to Deepak Joshi (stf-5)
  {
    id: 'cmp-020',
    token: 'FXL-2026-000085',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-3',
    location: { id: 'loc-3', building: 'CS Block', floor: 'Ground', room: 'Server Room' },
    description: 'Primary DNS forwarder timeouts causing campus-wide external domain resolution delays.',
    severity: 5,
    studentsAffected: 450,
    status: 'CLOSED',
    priorityScore: 94.0,
    extraData: { issueType: 'DNS Infrastructure', severity: 'Critical' },
    isAnonymous: false,
    student: { id: 'stu-1', name: 'Aryan Mehta', rollNo: '22CS101', department: 'Computer Science', contact: '+91 90000 00001' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-06-15T07:30:00.000Z',
    slaDeadline: '2026-06-15T13:30:00.000Z',
    resolvedAt: '2026-06-15T09:45:00.000Z',
    statusHistory: [
      { id: 'sh-53', status: 'RESOLVED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'Cleaned cache tables, re-routed DNS upstreams to redundant fiber links. Response latency down to 4ms.', timestamp: '2026-06-15T09:45:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-10', rating: 5, comment: 'Lightning fast fix for a major campus outage.', createdAt: '2026-06-15T11:00:00.000Z' },
  },
  // 21 — RESOLVED (May 2026) - Assigned to Deepak Joshi (stf-5)
  {
    id: 'cmp-021',
    token: 'FXL-2026-000045',
    categoryId: 'cat-6',
    categoryName: 'Tech Support',
    locationId: 'loc-3',
    location: { id: 'loc-3', building: 'CS Block', floor: '1st Floor', room: 'Faculty Room 102' },
    description: 'VoIP handset configuration lost after VLAN re-numbering.',
    severity: 2,
    studentsAffected: 6,
    status: 'CLOSED',
    priorityScore: 34.0,
    extraData: { issueType: 'VoIP Telephony' },
    isAnonymous: false,
    student: { id: 'stu-3', name: 'Sneha Patel', rollNo: '23IT044', department: 'Information Technology', contact: '+91 90000 00003' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-5',
    assignedToStaff: mockStaff[4],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: '2026-05-12T14:00:00.000Z',
    slaDeadline: '2026-05-13T14:00:00.000Z',
    resolvedAt: '2026-05-12T17:30:00.000Z',
    statusHistory: [
      { id: 'sh-54', status: 'RESOLVED', changedBy: 'Deepak Joshi', changedByRole: 'STAFF', note: 'SIP profile re-provisioned on PBX. Intercom and outbound calling active.', timestamp: '2026-05-12T17:30:00.000Z' },
    ],
    attachments: [],
    review: { id: 'rev-11', rating: 5, comment: 'Fixed the same afternoon.', createdAt: '2026-05-12T18:00:00.000Z' },
  },
  // 22 — RAGGING (UNDER_ADMIN_REVIEW, Anonymous Whistleblower)
  {
    id: 'cmp-022',
    token: 'FXL-2026-000240',
    categoryId: 'cat-9',
    categoryName: 'Ragging',
    locationId: 'loc-6',
    location: { id: 'loc-6', building: 'Block C Hostel', floor: '2nd Floor', room: 'C-214' },
    description: 'First-year junior students in Block C room 214 reported seniors demanding forced physical drills and verbal intimidation after 11 PM. Whistleblower submitted urgent request for anti-ragging squad patrol.',
    severity: 5,
    studentsAffected: 4,
    status: 'UNDER_ADMIN_REVIEW',
    priorityScore: 96.5,
    extraData: { raggingType: 'Forced Acts / Tasks', incidentLocation: 'Block C Hostel Room C-214', perpetratorsKnown: 'Yes - Senior batch known', immediateAssistanceNeeded: false },
    isAnonymous: true,
    createdAt: hoursAgo(4),
    slaDeadline: hoursFromNow(8),
    isAtRisk: false,
    isBreached: false,
    statusHistory: [
      { id: 'sh-55', status: 'SUBMITTED', changedBy: 'Anonymous Whistleblower', changedByRole: 'STAFF', timestamp: hoursAgo(4) },
      { id: 'sh-56', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Restricted — Anti-Ragging Cell alerted. Proctor patrol deployed to Block C.', timestamp: hoursAgo(3) },
    ],
    attachments: [],
  },
  // 23 — RAGGING (ASSIGNED, Disclosed Student)
  {
    id: 'cmp-023',
    token: 'FXL-2026-000241',
    categoryId: 'cat-9',
    categoryName: 'Ragging',
    locationId: 'loc-12',
    location: { id: 'loc-12', building: 'Sports Complex', floor: 'Ground Floor', room: 'Pavilion East Wing' },
    description: 'Late-night financial extortion and confiscation of student ID cards near sports pavilion by 4th-year seniors. Formal complaint filed directly by affected student with Chief Proctor.',
    severity: 5,
    studentsAffected: 3,
    status: 'ASSIGNED',
    priorityScore: 92.0,
    extraData: { raggingType: 'Extortion / Financial Demands', incidentLocation: 'Sports Complex Pavilion', perpetratorsKnown: 'Partial - Room/Branch known', immediateAssistanceNeeded: false },
    isAnonymous: false,
    student: { id: 'stu-11', name: 'Tanmay Kulkarni', rollNo: '24EC188', department: 'Electronics & Communication', contact: '+91 90000 00011' },
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-7',
    assignedToStaff: mockStaff[6],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: hoursAgo(18),
    slaDeadline: hoursFromNow(6),
    isAtRisk: false,
    isBreached: false,
    statusHistory: [
      { id: 'sh-57', status: 'SUBMITTED', changedBy: 'Tanmay Kulkarni', changedByRole: 'STAFF', timestamp: hoursAgo(18) },
      { id: 'sh-58', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', timestamp: hoursAgo(16) },
      { id: 'sh-59', status: 'ASSIGNED', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Assigned to Dr. Priya Sharma (Head of Student Affairs) & Anti-Ragging Disciplinary Squad.', timestamp: hoursAgo(12) },
    ],
    attachments: [],
  },
  // 24 — RAGGING (IN_PROGRESS, Anonymous Whistleblower, Urgent At Risk)
  {
    id: 'cmp-024',
    token: 'FXL-2026-000242',
    categoryId: 'cat-9',
    categoryName: 'Ragging',
    locationId: 'loc-13',
    location: { id: 'loc-13', building: 'Indoor Stadium', floor: 'Basement', room: 'Men Locker Room B' },
    description: 'Aggressive physical intimidation and harassment of junior basketball team recruits during evening practice hours. Immediate anti-ragging cell intervention initiated with student statements recorded.',
    severity: 5,
    studentsAffected: 6,
    status: 'IN_PROGRESS',
    priorityScore: 94.8,
    extraData: { raggingType: 'Physical Harassment', incidentLocation: 'Indoor Stadium Locker Room B', perpetratorsKnown: 'Yes - Senior batch known', immediateAssistanceNeeded: true },
    isAnonymous: true,
    assignedByAdminId: 'adm-1',
    assignedToStaffId: 'stf-7',
    assignedToStaff: mockStaff[6],
    dispatchType: 'INTERNAL_STAFF',
    createdAt: hoursAgo(8),
    slaDeadline: hoursFromNow(4),
    isAtRisk: true,
    isBreached: false,
    statusHistory: [
      { id: 'sh-60', status: 'SUBMITTED', changedBy: 'Anonymous Whistleblower', changedByRole: 'STAFF', timestamp: hoursAgo(8) },
      { id: 'sh-61', status: 'UNDER_ADMIN_REVIEW', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Urgent medical and physical safety verified.', timestamp: hoursAgo(7) },
      { id: 'sh-62', status: 'IN_PROGRESS', changedBy: 'Admin Krishnan', changedByRole: 'ADMIN', note: 'Statements recorded in presence of Chief Proctor. Hearing scheduled with parents and Anti-Ragging Committee.', timestamp: hoursAgo(5) },
    ],
    attachments: [],
  },
];

// ============================================================
// SLA RULES
// ============================================================

export const mockSLARules: SLARule[] = [
  { id: 'sla-1', categoryId: 'cat-1', categoryName: 'Hostel', severityTier: 'LOW', resolutionHours: 48, escalationHours: 72 },
  { id: 'sla-2', categoryId: 'cat-1', categoryName: 'Hostel', severityTier: 'MEDIUM', resolutionHours: 24, escalationHours: 36 },
  { id: 'sla-3', categoryId: 'cat-1', categoryName: 'Hostel', severityTier: 'HIGH', resolutionHours: 12, escalationHours: 18 },
  { id: 'sla-4', categoryId: 'cat-2', categoryName: 'Food', severityTier: 'LOW', resolutionHours: 24, escalationHours: 36 },
  { id: 'sla-5', categoryId: 'cat-2', categoryName: 'Food', severityTier: 'HIGH', resolutionHours: 8, escalationHours: 12 },
  { id: 'sla-6', categoryId: 'cat-4', categoryName: 'Health', severityTier: 'HIGH', resolutionHours: 4, escalationHours: 6 },
  { id: 'sla-7', categoryId: 'cat-6', categoryName: 'Tech Support', severityTier: 'MEDIUM', resolutionHours: 12, escalationHours: 24 },
  { id: 'sla-8', categoryId: 'cat-7', categoryName: 'Harassment', severityTier: 'HIGH', resolutionHours: 24, escalationHours: 48 },
];

// ============================================================
// ANALYTICS DATA
// ============================================================

export const mockHotspotData: HotspotData[] = [
  { location: 'Block A Hostel', category: 'Hostel', count: 14 },
  { location: 'Block B Hostel', category: 'Hostel', count: 11 },
  { location: 'Main Cafeteria', category: 'Food', count: 18 },
  { location: 'CS Block', category: 'Tech Support', count: 9 },
  { location: 'Library Block', category: 'Hygiene', count: 7 },
  { location: 'Library Block', category: 'Tech Support', count: 5 },
  { location: 'Block C Hostel', category: 'Hostel', count: 8 },
  { location: 'ECE Block', category: 'Academic', count: 6 },
  { location: 'Medical Block', category: 'Health', count: 4 },
  { location: 'Block D Hostel', category: 'Hostel', count: 10 },
];

export const mockSLATrends: SLATrendData[] = [
  { period: 'Jan', breached: 3, resolved: 45, atRisk: 8 },
  { period: 'Feb', breached: 5, resolved: 52, atRisk: 11 },
  { period: 'Mar', breached: 2, resolved: 60, atRisk: 7 },
  { period: 'Apr', breached: 7, resolved: 48, atRisk: 15 },
  { period: 'May', breached: 4, resolved: 55, atRisk: 9 },
  { period: 'Jun', breached: 6, resolved: 63, atRisk: 13 },
  { period: 'Jul', breached: 3, resolved: 38, atRisk: 6 },
];

export const mockCategoryVolume: CategoryVolumeData[] = [
  { name: 'Hostel', value: 43, color: '#6366f1' },
  { name: 'Food', value: 31, color: '#f59e0b' },
  { name: 'Tech Support', value: 24, color: '#06b6d4' },
  { name: 'Hygiene', value: 18, color: '#22c55e' },
  { name: 'Academic', value: 14, color: '#3b82f6' },
  { name: 'Health', value: 9, color: '#ef4444' },
  { name: 'Harassment', value: 3, color: '#ec4899' },
  { name: 'Malpractice', value: 2, color: '#f97316' },
];

export const mockStaffPerformance: StaffPerformanceData[] = [
  { staffId: 'stf-1', staffName: 'Vikram Singh', department: 'Maintenance', avgResolutionHours: 18.4, slaCompliancePercent: 87, openTickets: 7, resolvedThisMonth: 23 },
  { staffId: 'stf-2', staffName: 'Anita Rao', department: 'Maintenance', avgResolutionHours: 22.1, slaCompliancePercent: 91, openTickets: 4, resolvedThisMonth: 15 },
  { staffId: 'stf-3', staffName: 'Suresh Babu', department: 'Catering & Food', avgResolutionHours: 9.7, slaCompliancePercent: 94, openTickets: 5, resolvedThisMonth: 28 },
  { staffId: 'stf-4', staffName: 'Kavitha Menon', department: 'Catering & Food', avgResolutionHours: 6.3, slaCompliancePercent: 98, openTickets: 2, resolvedThisMonth: 31 },
  { staffId: 'stf-5', staffName: 'Deepak Joshi', department: 'IT & Systems', avgResolutionHours: 14.2, slaCompliancePercent: 82, openTickets: 9, resolvedThisMonth: 19 },
  { staffId: 'stf-6', staffName: 'Nisha Gupta', department: 'IT & Systems', avgResolutionHours: 11.8, slaCompliancePercent: 89, openTickets: 6, resolvedThisMonth: 22 },
  { staffId: 'stf-7', staffName: 'Ravi Chandran', department: 'Student Affairs', avgResolutionHours: 28.5, slaCompliancePercent: 78, openTickets: 3, resolvedThisMonth: 12 },
];

// ============================================================
// NOTIFICATIONS
// ============================================================

export const mockNotifications: Notification[] = [
  { id: 'notif-1', type: 'SLA_ALERT', title: 'SLA Breach Alert', message: 'FXL-2026-000165 has breached SLA — 15 hours overdue', complaintId: 'cmp-002', complaintToken: 'FXL-2026-000165', timestamp: hoursAgo(1), read: false },
  { id: 'notif-2', type: 'NEW_COMPLAINT', title: 'New Complaint Received', message: 'FXL-2026-000215 submitted — Academic complaint from Mohan K.', complaintId: 'cmp-007', complaintToken: 'FXL-2026-000215', timestamp: hoursAgo(1), read: false },
  { id: 'notif-3', type: 'SLA_ALERT', title: 'At-Risk Warning', message: 'FXL-2026-000182 approaching SLA deadline — 2 hours remaining', complaintId: 'cmp-001', complaintToken: 'FXL-2026-000182', timestamp: hoursAgo(2), read: false },
  { id: 'notif-4', type: 'STATUS_CHANGE', title: 'Complaint Reopened', message: 'FXL-2026-000140 was reopened by the student', complaintId: 'cmp-006', complaintToken: 'FXL-2026-000140', timestamp: daysAgo(1), read: true },
  { id: 'notif-5', type: 'ASSIGNMENT', title: 'New Assignment', message: 'You have been assigned FXL-2026-000201 — Lab CS-204 BSOD', complaintId: 'cmp-003', complaintToken: 'FXL-2026-000201', timestamp: hoursAgo(18), read: true },
];

// ============================================================
// STATUS UTILITIES
// ============================================================

export const statusLabels: Record<ComplaintStatus, string> = {
  SUBMITTED: 'Submitted',
  UNDER_ADMIN_REVIEW: 'Admin Review',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In Progress',
  AWAITING_STUDENT_INFO: 'Awaiting Info',
  VENDOR_ASSIGNED: 'Vendor Assigned',
  RESOLVED: 'Resolved',
  REOPENED: 'Reopened',
  CLOSED: 'Closed',
};

export const getCategoryById = (id: string): Category | undefined =>
  mockCategories.find((c) => c.id === id);

export const getStaffById = (id: string): Staff | undefined =>
  mockStaff.find((s) => s.id === id);

export const getVendorById = (id: string): Vendor | undefined =>
  mockVendors.find((v) => v.id === id);
