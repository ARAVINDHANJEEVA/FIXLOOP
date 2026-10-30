import apiClient from './client';
import { Complaint, ComplaintStatus, DispatchType, VendorContactStatus } from '../types';
import { toComplaint } from './mappers';

export interface AdminQueueParams {
  status?: string;
  categoryId?: number | string;
  departmentId?: number | string;
  dateFrom?: string;
  dateTo?: string;
  restricted?: boolean;
  page?: number;
  size?: number;
}

export interface StaffQueueParams {
  status?: string;
  page?: number;
  size?: number;
}

export interface PagedComplaints {
  content: Complaint[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export const complaintApi = {
  getAdminQueue: async (params: AdminQueueParams = {}): Promise<PagedComplaints> => {
    const queryParams: Record<string, any> = {
      page: params.page ?? 0,
      size: params.size ?? 50,
      restricted: params.restricted ?? false,
    };
    if (params.status && params.status !== 'ALL') queryParams.status = params.status;
    if (params.categoryId && params.categoryId !== 'ALL') queryParams.categoryId = params.categoryId;
    if (params.departmentId && params.departmentId !== 'ALL') queryParams.departmentId = params.departmentId;
    if (params.dateFrom) queryParams.dateFrom = params.dateFrom;
    if (params.dateTo) queryParams.dateTo = params.dateTo;

    const res = await apiClient.get('/api/complaints', { params: queryParams });
    const data = res.data;

    return {
      content: Array.isArray(data.content) ? data.content.map(toComplaint) : [],
      totalElements: data.totalElements ?? (data.content?.length || 0),
      totalPages: data.totalPages ?? 1,
      number: data.number ?? 0,
    };
  },

  getStaffQueue: async (params: StaffQueueParams = {}): Promise<PagedComplaints> => {
    const queryParams: Record<string, any> = {
      page: params.page ?? 0,
      size: params.size ?? 50,
    };
    if (params.status && params.status !== 'ALL') queryParams.status = params.status;

    const res = await apiClient.get('/api/complaints/mine', { params: queryParams });
    const data = res.data;

    return {
      content: Array.isArray(data.content) ? data.content.map(toComplaint) : [],
      totalElements: data.totalElements ?? (data.content?.length || 0),
      totalPages: data.totalPages ?? 1,
      number: data.number ?? 0,
    };
  },

  getById: async (idOrToken: string | number): Promise<Complaint> => {
    const res = await apiClient.get(`/api/complaints/${idOrToken}`);
    return toComplaint(res.data);
  },

  dispatch: async (
    id: string | number,
    payload: { dispatchType: DispatchType; departmentId?: number | string; vendorId?: number | string; note?: string }
  ): Promise<Complaint> => {
    const body = {
      dispatchType: payload.dispatchType,
      departmentId: payload.departmentId ? Number(payload.departmentId) : undefined,
      vendorId: payload.vendorId ? Number(payload.vendorId) : undefined,
      note: payload.note,
    };
    const res = await apiClient.patch(`/api/complaints/${id}/dispatch`, body);
    return toComplaint(res.data);
  },

  autoAssign: async (id: string | number, departmentId?: number | string): Promise<Complaint> => {
    const body = departmentId ? { departmentId: Number(departmentId) } : undefined;
    const res = await apiClient.post(`/api/complaints/${id}/assign`, body);
    return toComplaint(res.data);
  },

  reassign: async (id: string | number, payload: { staffId: number | string; reason: string }): Promise<Complaint> => {
    const body = {
      staffId: Number(payload.staffId),
      reason: payload.reason,
    };
    const res = await apiClient.post(`/api/complaints/${id}/reassign`, body);
    return toComplaint(res.data);
  },

  updateStatus: async (id: string | number, payload: { status: ComplaintStatus; note?: string }): Promise<Complaint> => {
    const res = await apiClient.patch(`/api/complaints/${id}/status`, payload);
    return toComplaint(res.data);
  },

  requestInfo: async (id: string | number, payload: { message: string }): Promise<Complaint> => {
    const res = await apiClient.post(`/api/complaints/${id}/request-info`, payload);
    return toComplaint(res.data);
  },

  updateVendor: async (
    id: string | number,
    payload: { vendorId: number | string; contactStatus: VendorContactStatus; notes?: string }
  ): Promise<Complaint> => {
    const body = {
      vendorId: Number(payload.vendorId),
      contactStatus: payload.contactStatus,
      notes: payload.notes,
    };
    const res = await apiClient.post(`/api/complaints/${id}/vendor`, body);
    return toComplaint(res.data);
  },

  uploadAttachment: async (id: string | number, file: File): Promise<any> => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post(`/api/complaints/${id}/attachments`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  /**
   * Admin manually clusters a complaint into a specific existing incident.
   * POST /api/complaints/{id}/cluster
   */
  clusterManually: async (
    id: string | number,
    payload: { targetIncidentId: number; reason?: string }
  ): Promise<Complaint> => {
    const res = await apiClient.post(`/api/complaints/${id}/cluster`, payload);
    return toComplaint(res.data);
  },

  /**
   * Admin removes a complaint from its cluster (sets cluster_excluded = true).
   * POST /api/complaints/{id}/uncluster
   */
  uncluster: async (
    id: string | number,
    payload: { reason?: string }
  ): Promise<Complaint> => {
    const res = await apiClient.post(`/api/complaints/${id}/uncluster`, payload);
    return toComplaint(res.data);
  },
};

export default complaintApi;
