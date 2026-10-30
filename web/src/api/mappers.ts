import {
  Complaint,
  Category,
  Staff,
  Department,
  SLARule,
  Vendor,
  ComplaintStatus,
} from '../types';
import { API_BASE_URL } from './client';

const safeJsonParse = (value: any, fallback: any = {}) => {
  if (typeof value !== 'string') return value || fallback;
  try {
    return JSON.parse(value);
  } catch (e) {
    return fallback;
  }
};

export const toComplaint = (dto: any): Complaint => {
  if (!dto) return {} as Complaint;

  return {
    id: String(dto.id),
    token: dto.token,
    categoryId: String(dto.categoryId || ''),
    categoryName: dto.categoryName,
    locationId: String(dto.locationId || dto.location?.id || ''),
    location: {
      id: String(dto.location?.id || dto.locationId || ''),
      building: dto.location?.building || 'Main Campus',
      floor: dto.location?.floor || 'Ground',
      room: dto.location?.room || '',
    },
    description: dto.description || '',
    severity: Number(dto.severity || 1),
    studentsAffected: Number(dto.studentsAffected || 1),
    status: dto.status as ComplaintStatus,
    priorityScore: Number(dto.priorityScore || 0),
    extraData: safeJsonParse(dto.extraData, {}),
    isAnonymous: Boolean(dto.isAnonymous),
    incidentId: dto.incidentId ? String(dto.incidentId) : undefined,
    incidentCount: dto.incidentCount,
    recurrenceCountLast90Days: dto.recurrenceCountLast90Days,
    student: dto.student
      ? {
          id: String(dto.student.id),
          name: dto.student.name,
          rollNo: dto.student.rollNo,
          department: dto.student.department,
          contact: dto.student.contact || '',
        }
      : undefined,
    assignedByAdminId: dto.assignedByAdminId ? String(dto.assignedByAdminId) : undefined,
    assignedToStaffId: dto.assignedToStaffId ? String(dto.assignedToStaffId) : undefined,
    assignedToStaff: dto.assignedToStaff
      ? {
          id: String(dto.assignedToStaff.id),
          name: dto.assignedToStaff.name,
          role: dto.assignedToStaff.jobTitle || 'Staff',
          departmentId: String(dto.assignedToStaff.departmentId),
          departmentName: dto.assignedToStaff.departmentName || '',
          skillTags: [],
          status: 'ACTIVE',
          currentWeightedLoad: Number(dto.assignedToStaff.currentWeightedLoad || 0),
          avatarInitials: dto.assignedToStaff.avatarInitials || 'ST',
          email: dto.assignedToStaff.email || '',
        }
      : undefined,
    dispatchType: dto.dispatchType,
    vendorAssignment: dto.vendorAssignment
      ? {
          vendorId: String(dto.vendorAssignment.vendorId),
          vendorName: dto.vendorAssignment.vendorName,
          assignedAt: dto.vendorAssignment.assignedAt,
          contactStatus: dto.vendorAssignment.contactStatus,
          notes: dto.vendorAssignment.notes,
        }
      : undefined,
    createdAt: dto.createdAt || new Date().toISOString(),
    slaDeadline: dto.slaDeadline || new Date().toISOString(),
    resolvedAt: dto.resolvedAt || undefined,
    statusHistory: Array.isArray(dto.statusHistory)
      ? dto.statusHistory.map((sh: any) => ({
          id: String(sh.id),
          status: sh.status,
          changedBy: sh.changedBy,
          changedByRole: sh.changedByRole,
          note: sh.note,
          timestamp: sh.timestamp,
        }))
      : [],
    attachments: Array.isArray(dto.attachments)
      ? dto.attachments.map((att: any) => ({
          id: String(att.id),
          fileUrl: att.fileUrl?.startsWith('http')
            ? att.fileUrl
            : `${API_BASE_URL}${att.fileUrl || '/api/attachments/' + att.id}`,
          fileName: att.fileName || 'attachment',
          mimeType: att.mimeType || 'application/octet-stream',
        }))
      : [],
    review: dto.review
      ? {
          id: String(dto.review.id),
          rating: Number(dto.review.rating),
          comment: dto.review.comment || '',
          createdAt: dto.review.createdAt,
        }
      : undefined,
    escalationLevel: dto.escalationLevel ? Number(dto.escalationLevel) : undefined,
    isAtRisk: Boolean(dto.isAtRisk),
    isBreached: Boolean(dto.isBreached),
    siblingTokens: dto.siblingTokens || [],
    siblingIds: Array.isArray(dto.siblingIds)
      ? dto.siblingIds.map((id: number) => String(id))
      : [],
    clusterExcluded: Boolean(dto.clusterExcluded),
    clusterScore: dto.clusterScore ? Number(dto.clusterScore) : undefined,
    incident: dto.incident
      ? {
          id: String(dto.incident.id),
          status: dto.incident.status || 'OPEN',
          complaintCount: Number(dto.incident.complaintCount || 0),
          summary: dto.incident.summary,
          members: Array.isArray(dto.incident.members)
            ? dto.incident.members.map((m: any) => ({
                id: String(m.id),
                token: m.token || '',
                description: m.description || '',
                status: m.status as any,
                clusterScore: m.clusterScore ? Number(m.clusterScore) : undefined,
                createdAt: m.createdAt || '',
              }))
            : [],
          createdAt: dto.incident.createdAt || '',
          lastComplaintAt: dto.incident.lastComplaintAt || '',
        }
      : undefined,
  };
};

export const toCategory = (dto: any): Category => {
  return {
    id: String(dto.id),
    name: dto.name,
    icon: dto.icon || 'HelpCircle',
    color: dto.color || '#64748b',
    sensitive: Boolean(dto.sensitive),
    baseSeverityWeight: Number(dto.baseSeverityWeight || 1),
    requiredSkillTags: dto.requiredSkillNames || [],
    formSchema: safeJsonParse(dto.formSchema, []),
    slaHours: Number(dto.defaultSlaHours || 24),
  };
};

export const toStaff = (dto: any): Staff => {
  return {
    id: String(dto.id),
    name: dto.name,
    role: dto.jobTitle || 'Staff',
    departmentId: String(dto.departmentId || ''),
    departmentName: dto.departmentName || '',
    skillTags: dto.skillTags || [],
    status: dto.status === 'ACTIVE' ? 'ACTIVE' : 'INACTIVE',
    currentWeightedLoad: Number(dto.currentWeightedLoad || 0),
    avatarInitials: dto.avatarInitials || 'ST',
    email: dto.email,
  };
};

export const toDepartment = (dto: any): Department => {
  return {
    id: String(dto.id),
    name: dto.name,
    description: dto.description || '',
    headOfDepartment: dto.headOfDept || '',
  };
};

export const toSLARule = (dto: any): SLARule => {
  return {
    id: String(dto.id),
    categoryId: String(dto.categoryId),
    categoryName: dto.categoryName || '',
    severityTier: dto.severityTier,
    resolutionHours: Number(dto.resolutionHours),
    escalationHours: Number(dto.escalationHours),
  };
};

export const toVendor = (dto: any): Vendor => {
  return {
    id: String(dto.id),
    name: dto.name,
    categoryTags: dto.skillTags || [],
    phone: dto.phone || '',
    email: dto.email || '',
  };
};
